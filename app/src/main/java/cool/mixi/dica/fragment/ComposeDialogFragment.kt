package cool.mixi.dica.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Location
import android.media.ExifInterface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ScaleXSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.hendraanggrian.appcompat.socialview.Hashtag
import com.hendraanggrian.appcompat.socialview.Mention
import com.hendraanggrian.appcompat.widget.HashtagArrayAdapter
import com.hendraanggrian.appcompat.widget.MentionArrayAdapter
import cool.mixi.dica.App
import cool.mixi.dica.R
import cool.mixi.dica.activity.BaseActivity
import cool.mixi.dica.activity.IndexActivity
import cool.mixi.dica.activity.StickerActivity
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Media
import cool.mixi.dica.bean.Status
import cool.mixi.dica.database.AppDatabase
import cool.mixi.dica.util.*
import cool.mixi.dica.view.MySocialEditTextView
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
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import javax.net.ssl.HttpsURLConnection

interface ICompose {
    fun done()
}

class ComposeDialogFragment : BaseDialogFragment() {

    var tmpMediaUri: ArrayList<String> = ArrayList()

    var roomView: View? = null
    var lastAddress: Address? = null

    private var inReplyStatusId: Int = 0
    private var inReplyScreenName: String? = ""
    private var retweetText: String? = ""
    private var editText: MySocialEditTextView? = null
    private var commonError: String? = null
    var sharedText: String? = null
    private var previewImageBoxBg: Drawable? = null
    private var maxPhotoUploadNumber: String? = null
    private var strMsgErr: String? = null
    private var strMsg: String? = null
    private var retweetMode = false

    var callback: WeakReference<ICompose>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        previewImageBoxBg = context?.getDrawable(R.drawable.photo_preview_box)
        maxPhotoUploadNumber = getString(R.string.max_photo_number_upload).format("${Consts.UPLOAD_MAX_PHOTOS}")
        commonError = getString(R.string.common_error)
        strMsgErr = getString(R.string.post_failure)
        strMsg = getString(R.string.post_success)

        dialog?.let {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomDialog)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
            it.setCanceledOnTouchOutside(true)
            val lp = it.window.attributes
            lp.gravity = Gravity.BOTTOM
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            it.window.attributes = lp
            it.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }


        roomView = inflater?.inflate(R.layout.dlg_compose, container)
        dialog?.setTitle("")


        editText = roomView?.et_text
        roomView?.et_text?.setText(PrefUtil.getLastStatus())
        roomView?.bt_submit?.setOnClickListener {
            composeSubmit(it)
        }

        roomView?.iv_emoji?.setOnClickListener {
            if (isOverPhotosLimit()) return@setOnClickListener
            val intent = Intent(context, StickerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivityForResult(intent, Consts.REQ_STICKER)
        }

        roomView?.iv_from_album?.setOnClickListener {
            if (isOverPhotosLimit()) return@setOnClickListener
            Nammu.askForPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, galleryPermCallback)
        }

        roomView?.iv_from_camera?.setOnClickListener {
            if (isOverPhotosLimit()) return@setOnClickListener
            Nammu.askForPermission(
                activity,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), cameraPermCallback
            )
        }

        roomView?.iv_location?.setOnClickListener {
            if (lastAddress != null) {
                (it as TextView).text = ""
                lastAddress = null
            } else {
                Nammu.askForPermission(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermCallback
                )
            }

        }
        roomView?.rb_public?.setOnClickListener { App.instance.selectedGroup.clear() }
        roomView?.rb_assign?.setOnClickListener { choiceGroupDialog() }

        // clear childView first
        roomView?.photo_box?.removeAllViews()

        // Reply to?
        if (arguments != null) {
            inReplyScreenName = arguments?.getString(Consts.EXTRA_IN_REPLY_USERNAME)
            inReplyStatusId = arguments?.getInt(Consts.EXTRA_IN_REPLY_STATUS_ID, 0)!!
            retweetText = arguments?.getString(Consts.EXTRA_RETWEET_TEXT)

            inReplyScreenName?.let {
                var str = getString(R.string.status_reply_to).format(inReplyScreenName)
                roomView?.group_reply?.visibility = View.VISIBLE
                roomView?.tv_reply_to?.text = str
            }

            retweetText?.let { retweetMode = true }
        }

        return roomView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PrefUtil.setLastStatus(et_text.text.toString())
    }

    override fun onStart() {
        super.onStart()

        if (App.instance.mygroup == null) {
            App.instance.loadGroup()
        }

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Consts.REQ_PHOTO_PATH) {
            data?.getStringExtra(Consts.EXTRA_PHOTO_URI)?.let {
                addPhotoPreview(it)
            }
            return
        } else if (requestCode == Consts.REQ_STICKER) {
            data?.getStringExtra(Consts.EXTRA_STICKER_URI)?.let { addPhotoPreview(it) }
        }
        EasyImage.handleActivityResult(requestCode, resultCode, data, activity, imageSelectedCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (tmpMediaUri.size == 0) {
            // Restore from cache
            App.instance.mediaUris.forEach { addPhotoPreviewFromLocalCache(it) }
        } else if (tmpMediaUri.size == 1) {
            handleImagePick(File(tmpMediaUri[0]))
            tmpMediaUri.clear()
        } else {
            // share to DiCa
            tmpMediaUri.iterator().forEach { addPhotoPreview(compressFilePath(it)) }
            tmpMediaUri.clear()
        }

        doAsync {
            val users = AppDatabase.getInstance().userDao().getAll()
            val tags = AppDatabase.getInstance().hashTagDao().getAll()
            val adapter = MentionArrayAdapter<Mention>(context!!)
            val adapterHashTag = HashtagArrayAdapter<Hashtag>(context!!)
            uiThread {
                users?.forEach { that ->
                    adapter.add(Mention(that.getEmail(), "${that.name}", that.profile_image_url_large))
                }
                tags?.forEach { that -> adapterHashTag.add(Hashtag("${that.name}")) }
                et_text.mentionAdapter = adapter
                et_text.hashtagAdapter = adapterHashTag
                if(retweetMode){
                    setSpanForRetweet(editText)
                }
            }
        }

        editText?.let {
            if (!retweetMode && sharedText != null) {
                it.setText(sharedText)
                return
            }
        }
    }

    private fun setSpanForRetweet(it: MySocialEditTextView?){
        if(it == null) return

        it.setHyperlinkEnabled(false)
        it.compoundDrawablePadding = it.text.toString().length * 10
        val span = SpannableStringBuilder("$retweetText")
        val start = 0
        val end = retweetText!!.length
        try {
            span.setSpan(BackgroundColorSpan(Color.DKGRAY), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(ForegroundColorSpan(Color.LTGRAY), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(ScaleXSpan(0.6f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }catch (e: Exception){}
        span.append("\n\n")
        it.text = span
        it.requestFocus()
        it.setSelection(it.text.length)
        it.requestFocus(it.text.length)
    }

    private fun isOverPhotosLimit(): Boolean {
        val c = roomView!!.photo_box.childCount
        val oversize = c >= Consts.UPLOAD_MAX_PHOTOS
        if (oversize) {
            App.instance.toast(maxPhotoUploadNumber!!)
        }
        return oversize
    }

    private fun setMyLocation() {
        roomView?.iv_location?.text = getString(R.string.loading)
        LocationUtil.instance.getLocation(object : IGetLocation {
            override fun done(location: Location?) {
                if (location != null) {
                    setMyAddress(location)
                }
            }
        })
    }

    private fun setMyAddress(location: Location) {
        LocationUtil.instance.getAddress(location, object : IGetAddress {
            override fun done(address: Address) {
                lastAddress = address
                roomView?.iv_location?.text = address.getAddressLine(0)
            }
        })
    }

    private val imageSelectedCallback = object : DefaultCallback() {
        override fun onImagesPicked(p0: MutableList<File>, p1: EasyImage.ImageSource?, p2: Int) {
            if (isOverPhotosLimit()) {
                return
            }

            if (p0.size == 1) {
                handleImagePick(p0[0])
                return
            }

            p0.forEach {
                if (isOverPhotosLimit()) {
                    return@forEach
                }

                addPhotoPreview(compressFilePath(it.absolutePath))
            }
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
        if (!imageFile.exists()) {
            return
        }

        var filePath = imageFile.absolutePath
        if (filePath.toLowerCase().endsWith(".gif")) {
            return
        }

        filePath = compressFilePath(imageFile.absolutePath)
        var fg = PhotoProcessFragment()
        var b = Bundle()
        b.putString(Consts.EXTRA_PHOTO_URI, filePath)
        fg.arguments = b
        fg.setTargetFragment(this, Consts.REQ_PHOTO_PATH)
        fg.myShow(fragmentManager, Consts.FG_PHOTO_CROP)
    }

    private fun setMediaPreviewBox(uri: String): ImageView {
        val box = roomView?.photo_box as ViewGroup
        var imgView = ImageView(this.context)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(10, 10, 10, 10)
        imgView.layoutParams = lp
        imgView.layoutParams.height = 200
        imgView.layoutParams.width = 200
        imgView.setTag(R.id.preview_image_uri, uri)
        imgView.background = previewImageBoxBg
        imgView.setOnClickListener {
            (it.parent as ViewGroup).removeView(it)
            val uri = it.getTag(R.id.preview_image_uri)
            App.instance.mediaUris.remove(uri)
            if (box.childCount == 0) box.visibility = View.GONE
        }
        box.addView(imgView)
        return imgView
    }

    private fun addPhotoPreviewFromLocalCache(uri: String) {
        if (isOverPhotosLimit()) {
            return
        }
        val imgView = setMediaPreviewBox(uri)
        if (uri.startsWith("http", true)) {
            Glide.with(context!!).load(uri).into(imgView)
        } else {
            val imageFile = File(uri)
            if (imageFile.absoluteFile.endsWith("mp4")) {
                Glide.with(context!!).asBitmap().load(imageFile).into(imgView!!)
            } else {
                Glide.with(context!!).load(imageFile).into(imgView!!)
            }
        }
    }

    private fun addPhotoPreview(uri: String) {
        App.instance.mediaUris.add(uri)
        addPhotoPreviewFromLocalCache(uri)
    }

    class StatusUpdateCallback(fragment: ComposeDialogFragment) : Callback<String> {
        private val ref = SoftReference<ComposeDialogFragment>(fragment)
        override fun onFailure(call: Call<String>, t: Throwable) {
            eLog("${t.message!!}")
            if (ref.get() == null) return

            ref.get()?.let {
                App.instance.toast(it.strMsgErr?.format(t.message)!!)
                (it as BaseActivity).loaded()
            }
        }

        override fun onResponse(call: Call<String>, response: Response<String>) {
            if (ref.get() == null || ref.get()?.activity == null) {
                if (response != null && response.code() == HttpsURLConnection.HTTP_OK) {
                    App.instance.clear()
                }
                return
            }

            (ref.get()?.activity as BaseActivity).loaded()

            val strMsgErr = ref.get()?.strMsgErr
            val strMsg = ref.get()?.strMsg
            val res = response.body().toString()
            dLog("$res, ${response.errorBody()}, ${response.message()}")

            if (response.code() != HttpsURLConnection.HTTP_OK) {
                App.instance.toast("${strMsgErr?.format(res)}")
                return
            }

            var msgToShow: String?
            msgToShow = try {
                Gson().fromJson(res, Status::class.java)

                ref.get()?.let {

                    // Reload Status Timeline
                    if ((it.callback == null || it.callback!!.get() == null)
                        && it.activity != null && it.activity is IndexActivity
                    ) {
                        (it.activity as IndexActivity).setComposeDialogCallback(it)
                    }

                    it.callback?.get()?.let { that -> that.done() }
                    it.composeDone()
                }
                strMsg
            } catch (e: Exception) {
                eLog(e.toString())
                strMsgErr?.format(res)
            }

            msgToShow?.let {
                App.instance.toast(msgToShow)
            }

            App.instance.mediaUris.clear()
        }

    }

    private fun composeSubmit(button: View) {
        DiCaUtil.hideKeyboard(context, button)
        activity?.let {
            (it as BaseActivity).loading(getString(R.string.status_saving))
            DiCaUtil.hideKeyboard(it)
        }

        var text = et_text.text.toString()
        var lat = ""
        var long = ""
        var source = activity?.getString(R.string.app_name)
        val strUploading = getString(R.string.status_media_uploading)
        val strStatusPosting = getString(R.string.status_posting)
        lastAddress?.let {
            lat = "${it?.latitude}"
            long = "${it?.longitude}"
        }

        var allowGids = ""
        val targetGroup = if (roomView?.rb_public?.isChecked!!) {
            ArrayList()
        } else {
            allowGids = "<${App.instance.selectedGroup.joinToString("><")}>"
            App.instance.selectedGroup
        }

        val ref = SoftReference<ComposeDialogFragment>(this)
        doAsync {
            var firstMediaId = ""
            var errorMsg = StringBuffer()
            App.instance.mediaUris?.forEachIndexed { index, it ->
                if (it.startsWith("http", true)) {
                    text = "$text\n[img]$it[/img]\n"
                    return@forEachIndexed
                }

                val file = File(it)
                dLog("fileSize: ${file.length()}")
//                val requestFile = RequestBody.create(MediaType.parse("image/*"), file)

                val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
                val body = MultipartBody.Part.createFormData("media", file.name, requestFile)

                ref.get()?.activity?.let { that ->
                    uiThread { (that as BaseActivity).loadingState(strUploading.format("${index + 1}")) }
                }

                try {
                    val response = ApiService.create().mediaUpload(body).execute()
                    dLog("media/upload: ${response.body()}, ${response.message()}, ${response.errorBody()}")
                    if (response == null || !response.isSuccessful) {
                        errorMsg.append(" #$index - ${response?.body()}\n")
                        return@forEachIndexed
                    }

                    try {
                        val media = Gson().fromJson(response?.body(), Media::class.java)
                        if (media != null && media.media_id > 0 && firstMediaId.isEmpty()) {
                            firstMediaId = "${media.media_id}"
                        }

                        media.image?.friendica_preview_url?.let { that ->
                            text = "$text\n[img]$that[/img]\n"
                            firstMediaId = ""

                            // set photo permission
                            media.image!!.hashId()?.let { hashId ->
                                // get album name
                                val photo = ApiService.create().friendicaPhoto(hashId).execute().body()
                                dLog("photo $photo")
                                dLog("permissionResult - $allowGids - $hashId}")
                                ApiService.create().friendicaPhotoUpdate(hashId, photo?.album, allowGids).execute()
                            }
                        }
                    } catch (e: Exception) {
                        val msg = "#$index - ${response.body()}\n"
                        errorMsg.append(msg)
                        eLog("err $msg")
                    }

                } catch (e: Exception) {
                    val msg = "#$index - ${e.message}\n"
                    errorMsg.append(msg)
                    eLog("err2 $msg ${e.message}")
                }
            }



            uiThread {
                ref.get()?.activity?.let { that ->
                    (that as BaseActivity).loadingState(strStatusPosting)
                }

                //TODO: after 2019/03 new API official, this part of [if] should be interrupted by return
                if (errorMsg.isNotEmpty()) {
                    val snackBar = Snackbar.make(
                        bt_submit
                        , errorMsg.toString(), Snackbar.LENGTH_INDEFINITE
                    )
                    snackBar.setAction(android.R.string.ok) { snackBar.dismiss() }
                    snackBar.show()
                    ref.get()?.activity?.let { that -> (that as BaseActivity).loaded() }
                }

                ApiService.create()
                    .statusUpdate("$source", "$text", inReplyStatusId, "$lat", "$long", targetGroup, firstMediaId)
                    .enqueue(StatusUpdateCallback(ref.get()!!))
            }
        }
    }

    private fun composeDone() {
        et_text.setText("")
        dismiss()
        App.instance.loadGroup()
    }

    private fun choiceGroupDialog() {
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
            if (!isSelect) {
                arrayId?.get(index)?.let { App.instance.selectedGroup.remove(it) }
            } else {
                arrayId?.get(index)?.let { App.instance.selectedGroup.add(it) }
            }
        }
        builder?.setCancelable(true)
        builder?.setNegativeButton(android.R.string.cancel) { dialog, index ->

        }
        builder?.setPositiveButton(android.R.string.ok) { dialog, button ->
            afterGroupSelected()
        }
        builder?.setOnDismissListener { afterGroupSelected() }
        builder?.create()?.show()
    }

    private fun afterGroupSelected() {
        if (App.instance.selectedGroup.size == 0) {
            roomView?.rb_public?.isChecked = true
        }
    }


    fun getImageOrientation(imageFile: File): Int {
        val exif = ExifInterface(imageFile.absolutePath)
        val exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        return when (exifRotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    fun applyRotationIfNeeded(imageFile: File) {
        val rotation = getImageOrientation(imageFile)
        if (rotation == 0) return
        applyRotationIfNeeded(imageFile, rotation)
    }

    private fun applyRotationIfNeeded(imageFile: File, rotation: Int) {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        lateinit var out: FileOutputStream
        try {
            out = FileOutputStream(imageFile)
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        } catch (e: Exception) {
        } finally {
            rotatedBitmap.recycle()
            out.close()
        }
    }

    private fun compressFilePath(filePath: String): String {
        val orgFile = File(filePath)
        val rotation = getImageOrientation(orgFile)
        val bitmap: Bitmap = BitmapFactory.decodeFile(filePath)
        val ext = filePath.substringAfterLast(".", "")
        var outStream: OutputStream?
        val file = File.createTempFile(getString(R.string.app_name), "")
//        val file = File.createTempFile(getString(R.string.app_name), ".$ext")
        try {
            outStream = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, Consts.COMPRESS_PHOTO_QUALITY, outStream)
            outStream!!.close()
        } catch (e: Exception) {
            dLog("${e.message}")
        } finally {
            file.deleteOnExit()
        }

        dLog("originalFilePath ${orgFile.absolutePath}: ${orgFile.length()}")
        dLog("compressFilePath ${file.absolutePath}: ${file.length()}")
        if (rotation > 0) {
            applyRotationIfNeeded(file, rotation)
        }
        return file.absolutePath
    }
}