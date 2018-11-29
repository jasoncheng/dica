package cool.mixi.dica.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.location.Address
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.BaseActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Media
import cool.mixi.dica.bean.Status
import cool.mixi.dica.util.*
import kotlinx.android.synthetic.main.dlg_compose.*
import kotlinx.android.synthetic.main.dlg_compose.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import pl.aprilapps.easyphotopicker.DefaultCallback
import pl.aprilapps.easyphotopicker.EasyImage
import pl.tajchert.nammu.Nammu
import pl.tajchert.nammu.PermissionCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.net.ssl.HttpsURLConnection

class ComposeDialogFragment: BaseDialogFragment() {

    var mediaFile: File? = null
    var roomView: View? = null
    var lastAddress: Address? = null

    var in_reply_status_id: Int? = 0
    var in_reply_screenname: String? = ""
    var editText: EditText? = null
    var sharedText: String? = null
    var sharedFile: String? = null
    var commonError: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        roomView = inflater?.inflate(R.layout.dlg_compose, container)
        dialog.setTitle("")


        editText = roomView?.et_text
        roomView?.et_text?.setText(PrefUtil.getLastStatus())
        roomView?.bt_submit?.setOnClickListener {
            composeSubmit()
        }

        roomView?.iv_from_album?.setOnClickListener {
            Nammu.askForPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, galleryPermCallback)
        }
        roomView?.iv_from_camera?.setOnClickListener {
            Nammu.askForPermission(activity,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), cameraPermCallback)
        }
        roomView?.iv_location?.setOnClickListener {
            if(lastAddress != null){
                (it as TextView).text = ""
                lastAddress = null
            } else {
                Nammu.askForPermission(activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationPermCallback)
            }

        }
        roomView?.rb_public?.setOnClickListener{ App.instance.selectedGroup.clear() }
        roomView?.rb_assign?.setOnClickListener{ choiceGroupDialog() }

        // Reply to?
        if(arguments != null){
            in_reply_screenname = arguments?.getString(Consts.EXTRA_IN_REPLY_USERNAME)
            in_reply_status_id = arguments?.getInt(Consts.EXTRA_IN_REPLY_STATUS_ID, 0)
            var str = getString(R.string.status_reply_to).format(in_reply_screenname)
            roomView?.group_reply?.visibility = View.VISIBLE
            roomView?.tv_reply_to?.text = str
        }

        return roomView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PrefUtil.setLastStatus(et_text.text.toString())
    }

    override fun onStart() {
        super.onStart()

        if(App.instance.mygroup == null){
            App.instance.loadGroup()
        }

        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Consts.REQ_PHOTO_PATH) {
            data?.getStringExtra(Consts.EXTRA_PHOTO_URI)?.let {
                addPhotoPreview(File(it))
            }
            return
        }

        EasyImage.handleActivityResult(requestCode, resultCode, data, activity, imageSelectedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedFile?.let {
            addPhotoPreview(File(it))
        }
        sharedText?.let { str ->
            editText?.let { it.setText(str) }
        }
    }

    private fun setMyLocation() {
        roomView?.iv_location?.text = getString(R.string.loading)
        LocationUtil.instance.getLocation(object: IGetLocation {
            override fun done(location: Location?) {
                if (location != null) {
                    setMyAddress(location)
                }
            }
        })
    }

    private fun setMyAddress(location: Location){
        LocationUtil.instance.getAddress(location, object: IGetAddress{
            override fun done(address: Address) {
                lastAddress = address
                roomView?.iv_location?.text = address.getAddressLine(0)
            }
        })
    }

    private val imageSelectedCallback = object : DefaultCallback() {
        override fun onImagesPicked(p0: MutableList<File>, p1: EasyImage.ImageSource?, p2: Int) {
            handleImagePick(p0[0])
        }

        override fun onImagePickerError(e: Exception?, source: EasyImage.ImageSource?, type: Int) {
            App.instance.toast(getString(R.string.fail_pick_photo).format(e?.message))
        }

        override fun onCanceled(source: EasyImage.ImageSource?, type: Int) {
        }
    }

    private val galleryPermCallback = object : PermissionCallback {
        override fun permissionGranted() {
            EasyImage.openGallery(activity, 0)
        }

        override fun permissionRefused() {
            App.instance.toast(getString(R.string.perm_deny_gallery))
        }
    }

    private val cameraPermCallback = object : PermissionCallback {
        override fun permissionGranted() {
            EasyImage.openCameraForImage(activity, 0)
        }

        override fun permissionRefused() {
            App.instance.toast(getString(R.string.perm_deny_camera))
        }
    }

    private val locationPermCallback = object : PermissionCallback {
        @SuppressLint("MissingPermission")
        override fun permissionGranted() {
            setMyLocation()
        }

        override fun permissionRefused() {
            App.instance.toast(getString(R.string.perm_deny_gps))
        }
    }

    private fun handleImagePick(imageFile: File) {
        val filePath = imageFile.absolutePath
        if (filePath.toLowerCase().endsWith(".gif")) {
            return
        }

        var fg = PhotoProcessFragment()
        var b = Bundle()
        b.putString(Consts.EXTRA_PHOTO_URI, filePath)
        fg.arguments = b
        fg.setTargetFragment(this, Consts.REQ_PHOTO_PATH)
        fg.myShow(fragmentManager, Consts.FG_PHOTO_CROP)
    }

    private fun addPhotoPreview(imageFile: File){
        mediaFile = imageFile
        val box = roomView?.photo_box as ViewGroup
        var imgView = ImageView(this.context)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        imgView.layoutParams = lp
        imgView.layoutParams.height = 200
        imgView.layoutParams.width = 200
        box.removeAllViews()
        box.addView(imgView)
        box.visibility = View.VISIBLE
        dLog("mediaPath: ${imageFile.absoluteFile}")
        if(imageFile.absoluteFile.endsWith("mp4")) {
            Glide.with(context!!).asBitmap().load(imageFile).into(imgView!!)
        } else {
            Glide.with(context!!).load(imageFile).into(imgView!!)
        }
        imgView.setOnClickListener {
            (it.parent as ViewGroup).removeView(it)
            mediaFile = null
            box.visibility = View.GONE
        }
    }

    class StatusUpdateCallback(fragment: ComposeDialogFragment): Callback<String> {
        private val ref = WeakReference<ComposeDialogFragment>(fragment)
        private val strMsgErr = ref.get()?.getString(R.string.post_failure)
        private val strMsg = ref.get()?.getString(R.string.post_success)
        override fun onFailure(call: Call<String>, t: Throwable) {
            (ref.get()?.activity as BaseActivity).loaded()
            App.instance.toast(strMsgErr?.format(t.message)!!)
            eLog("${t.message!!}")
        }

        override fun onResponse(call: Call<String>, response: Response<String>) {
            (ref.get()?.activity as BaseActivity).loaded()

            val res = response.body().toString()
            dLog("$res, ${response.errorBody()}, ${response.message()}")

            if(response.code() != HttpsURLConnection.HTTP_OK) {
                App.instance.toast("${strMsgErr?.format(res)}")
                return
            }

            var msgToShow: String?
            msgToShow = try {
                Gson().fromJson(res, Status::class.java)
                ref.get()?.composeDone()
                strMsg
            }catch (e: java.lang.Exception){
                strMsgErr?.format(res)
            }

            msgToShow?.let {
                App.instance.toast(msgToShow)
            }
        }

    }

    private fun composeSubmit() {
        (activity as BaseActivity).loading(getString(R.string.status_saving))

        val text = et_text.text.toString()
        var lat = ""
        var long = ""
        var source = activity?.getString(R.string.app_name)
        lastAddress?.let {
            lat = "${it?.latitude}"
            long = "${it?.longitude}"
        }
        val targetGroup = if(roomView?.main_radiogroup?.isSelected!!){
            ArrayList()
        } else {
            App.instance.selectedGroup
        }
        val status = RequestBody.create(MediaType.parse("multipart/form-data"), text)
        val statusIdBody = RequestBody.create(MediaType.parse("multipart/form-data"), "$in_reply_status_id")
        val sourceBody = RequestBody.create(MediaType.parse("multipart/form-data"), source)
        val latBody = RequestBody.create(MediaType.parse("multipart/form-data"), lat)
        val longBody = RequestBody.create(MediaType.parse("multipart/form-data"), long)
        var map = LinkedHashMap<String, RequestBody>()
        targetGroup.forEach {
            map["group_allow[]"] = RequestBody.create(MediaType.parse("text/plain"), it.toString())
        }

        val ref = WeakReference<ComposeDialogFragment>(this)
        doAsync {
            var mediaIds:RequestBody? = null
            var response: Response<String>? = null
            ref.get()?.mediaFile?.let {
                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), it)
                val body = MultipartBody.Part.createFormData("media", it.name, requestFile)
                response= ApiService.create().mediaUpload(body).execute()
            }
            uiThread {
                try {
                    if(response == null || response?.isSuccessful != true){
                        ref.get()?.commonError?.let { that -> App.instance.toast(that.format(response?.body())) }
                    } else {
                        val media = Gson().fromJson(response?.body(), Media::class.java)
                        mediaIds = RequestBody.create(MediaType.parse("multipart/form-data"), "${media.media_id}")
                    }

                    ApiService.create().statusUpdate(sourceBody, status, statusIdBody, latBody, longBody, map, mediaIds).enqueue(
                        StatusUpdateCallback(ref.get()!!)
                    )
                }catch (e: Exception){
                    response?.body().let { that ->
                        if(that != null){
                            App.instance.toast(that)
                        } else {
                            ref.get()?.commonError?.format("${e.message}")?.let { w -> App.instance.toast(w) }
                        }
                    }
                    ref.get()?.activity?.let { that -> (that as BaseActivity).loaded() }
                }
            }
        }
    }

    private fun composeDone() {
        et_text.setText("")
        dismiss()
        App.instance.loadGroup()
    }

    private fun choiceGroupDialog(){
        val size = App.instance.mygroup?.size
        var arrName = size?.let { arrayOfNulls<String>(it) }
        var arrayId = size?.let { IntArray(it) }
        var arraySelected = size?.let { BooleanArray(it) }
        var idx = 0

        App.instance.mygroup?.forEach {
            var name = "${it.name} (${it.user.size})"
            arrName?.set(idx, name)
            arrayId?.set(idx, it.gid)
            arraySelected?.set(idx, App.instance.selectedGroup.contains(it.gid))
            idx++
        }

        val builder = activity?.let { AlertDialog.Builder(it) }
        builder?.setTitle("")
        builder?.setMultiChoiceItems(arrName, arraySelected) { dialog, index, isSelect ->
            if(!isSelect){
                arrayId?.get(index)?.let { App.instance.selectedGroup.remove(it) }
            } else {
                arrayId?.get(index)?.let { App.instance.selectedGroup.add(it) }
            }
        }
        builder?.setCancelable(true)
        builder?.setNegativeButton(android.R.string.cancel){ dialog, index ->

        }
        builder?.setPositiveButton(android.R.string.ok) { dialog, button ->
            afterGroupSelected()
        }
        builder?.setOnDismissListener { afterGroupSelected() }
        builder?.create()?.show()
    }

    fun afterGroupSelected(){
        if(App.instance.selectedGroup.size == 0){
            roomView?.rb_public?.isChecked = true
        }
    }
}