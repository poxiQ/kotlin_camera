package glebova.rsue.camera

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import glebova.rsue.camera.databinding.ActivityMainBinding
import glebova.rsue.camera.dialogs.ChoosePhotoDialog
import glebova.rsue.camera.extensions.getFileName
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var photoFile: File
    private lateinit var binding: ActivityMainBinding
    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.let { uri ->
                val parcelFileDescriptor = applicationContext
                    .contentResolver
                    .openFileDescriptor(uri, "r", null)

                parcelFileDescriptor?.let { _ ->
                    val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                    val file = File(
                        applicationContext.cacheDir,
                        applicationContext.contentResolver.getFileName(uri)
                    )
                    val outputStream = FileOutputStream(file)
                    IOUtils.copy(inputStream, outputStream)
                    onGetImage(file = file, uri = uri)
                }
            }
        }
    private val openCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            onImageTake()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }

    private fun setupListeners() {
        binding.button.setOnClickListener {
            ChoosePhotoDialog().let {
                it.callback = object : ChoosePhotoDialog.ChoosePhotoDialogCallback {
                    override fun onChoosePhotoClick() {
                        openGalleryLauncher.launch("image/*")
                    }

                    override fun onMakePhotoClick() {
                        applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            ?.let { file ->
                                createImageFile(file).let { tempFile ->
                                    photoFile = tempFile
                                    openCameraLauncher.launch(
                                        FileProvider.getUriForFile(
                                            applicationContext,
                                            applicationContext.applicationContext.packageName + ".provider",
                                            tempFile
                                        )
                                    )
                                }
                            }
                    }
                }
                it.show(supportFragmentManager, ChoosePhotoDialog::class.simpleName)
            }
        }
    }

    private fun onImageTake() {
//            val bitmap = data?.extras?.get("data") as Bitmap
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        binding.image.setImageBitmap(bitmap)
    }

    private fun onGetImage(file: File, uri: Uri) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        binding.image.setImageBitmap(bitmap)
    }

    private fun createImageFile(directory: File): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            directory
        )
    }
}