package link.mawa.android.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.dlg_compose.*
import kotlinx.android.synthetic.main.dlg_compose.view.*
import link.mawa.android.App
import link.mawa.android.R
import link.mawa.android.activity.BaseActivity
import link.mawa.android.activity.MainActivity
import link.mawa.android.bean.Consts
import link.mawa.android.bean.Status
import link.mawa.android.util.ApiService
import link.mawa.android.util.PrefUtil
import link.mawa.android.util.dLog
import link.mawa.android.util.eLog
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        roomView = inflater?.inflate(R.layout.dlg_compose, container)
        dialog.setTitle("")

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
        return roomView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PrefUtil.setLastStatus(et_text.text.toString())
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dLog("=============> onActivityResult callback lol.......$requestCode")
        if(requestCode == Consts.REQ_PHOTO_PATH) {
            try {
                mediaFile = File(data?.getStringExtra(Consts.EXTRA_PHOTO_URI))
                dLog("=============> onActivityResult: ${mediaFile?.absolutePath} ${mediaFile?.exists()}")
                addPhotoPreview(mediaFile!!)
            }catch (e: java.lang.Exception){}
            return
        }
        EasyImage.handleActivityResult(requestCode, resultCode, data, activity, imageSelectedCallback)
    }

    private fun getLastLocation(location: Location) {
        val view = roomView?.iv_location!!
        view.text = getString(R.string.loading)
        val geocoder = Geocoder(context, Locale.getDefault())
        Thread(Runnable {
            try {
                var addresses = geocoder.getFromLocation(
                    location.latitude, location.longitude, 1
                )
                lastAddress = addresses.first()
                this@ComposeDialogFragment.activity?.runOnUiThread {
                    view.text = lastAddress?.getAddressLine(0)
                }
            } catch (e: Exception) {
                view.text = ""
                e.printStackTrace()
            }
        }).start()
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
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
            fusedLocationClient.lastLocation.addOnSuccessListener {
                it ?: return@addOnSuccessListener
                getLastLocation(it)
            }
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
        Glide.with(context).load(imageFile).into(imgView)
        imgView.setOnClickListener {
            (it.parent as ViewGroup).removeView(it)
            mediaFile = null
            box.visibility = View.GONE
        }
    }

    class StatusUpdateCallback(fragment: ComposeDialogFragment): Callback<Status> {
        private val ref = WeakReference<ComposeDialogFragment>(fragment)
        private val strMsg = ref.get()?.getString(R.string.compose_fail)
        override fun onFailure(call: Call<Status>, t: Throwable) {
            (ref.get()?.activity as BaseActivity).loaded()
            App.instance.toast(strMsg?.format(t.message)!!)
            eLog("==========>"+t.message!!)
        }

        override fun onResponse(call: Call<Status>, response: Response<Status>) {
            (ref.get()?.activity as BaseActivity).loaded()
            dLog(response.message())
            dLog(response.body().toString())
            if(response.code() != HttpsURLConnection.HTTP_OK) {
                App.instance.toast(strMsg?.format(response.message())!!)
            } else {
                ref.get()?.composeDone()
            }
        }

    }

    private fun composeSubmit() {
        (activity as BaseActivity).loading(getString(R.string.status_saving))
        val text = et_text.text.toString()
        var lat:String = ""
        var long:String = ""
        if(lastAddress != null){
            lat = "${lastAddress?.latitude}"
            long = "${lastAddress?.longitude}"
        }

        if(mediaFile == null){
            ApiService.create().statusUpdate(text, lat, long).enqueue(StatusUpdateCallback(this))
            return
        }

        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), mediaFile)
        val body = MultipartBody.Part.createFormData("media", mediaFile?.name, requestFile)
        val status = RequestBody.create(MediaType.parse("multipart/form-data"), text)

        val latBody = RequestBody.create(MediaType.parse("multipart/form-data"), lat)
        val longBody = RequestBody.create(MediaType.parse("multipart/form-data"), long)
        ApiService.create().statusUpdate(status, latBody, longBody, body).enqueue(StatusUpdateCallback(this))
    }

    private fun composeDone() {
        et_text.setText("")
        (activity as MainActivity).loadNewestStatuses()
        dismiss()
    }
}