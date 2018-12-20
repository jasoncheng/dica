package cool.mixi.dica.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.theartofdev.edmodo.cropper.CropImageView
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import kotlinx.android.synthetic.main.fg_photo_process.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class PhotoProcessFragment: BaseDialogFragment() {

    private var originalFileUri: String? = null
    private var originalFileName: String? = Date().time.toString()
    private var cropImageView: CropImageView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val roomView = inflater?.inflate(R.layout.fg_photo_process, container)
        if (arguments == null) {
            dismissAllowingStateLoss()
            return roomView
        }

        originalFileUri = arguments?.getString(Consts.EXTRA_PHOTO_URI)

        if (originalFileUri == null) {
            dismissAllowingStateLoss()
            return roomView
        }

        val f = File(originalFileUri)
        if (!f.exists()) {
            dismissAllowingStateLoss()
            return roomView
        }

        if(f.name != null && !f.name.isEmpty()) {
            originalFileName = f.name
        }

        cropImageView = roomView?.cropImageView
        cropImageView!!.setImageUriAsync(Uri.fromFile(f))

        roomView?.crop?.setOnClickListener {
            cropImageView!!.setImageBitmap(roomView?.cropImageView?.croppedImage)
        }
        roomView?.crop_rotate?.setOnClickListener {
            cropImageView!!.rotateImage(90)
        }
        roomView.crop_reset?.setOnClickListener {
            cropImageView!!.setImageUriAsync(Uri.fromFile(f))
        }
        roomView.crop_ok.setOnClickListener { saveToFile() }
        return roomView!!
    }

    override fun onStart() {
        super.onStart()
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
    }

    private fun saveToFile() {
        val bitmap: Bitmap
        try {
            bitmap = cropImageView!!.croppedImage
        } catch (e: Exception) {
            return
        }

        var outStream: OutputStream?
        val file = File.createTempFile(getString(R.string.app_name), "$originalFileName")
        try {
            outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, Consts.COMPRESS_PHOTO_QUALITY, outStream)
            outStream!!.close()
            activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))


            var intent = Intent()
            intent.putExtra(Consts.EXTRA_PHOTO_URI, file.absolutePath)
            targetFragment?.onActivityResult(targetRequestCode, RESULT_OK,  intent)
            dismissAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("saveToFile", "===============> " + e.message)
        } finally {
            file.deleteOnExit()
        }

    }
}