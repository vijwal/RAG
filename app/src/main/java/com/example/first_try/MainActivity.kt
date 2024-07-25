package com.example.first_try

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

interface DialogDismissListener {
    fun onDialogDismissed()
}
class MainActivity : AppCompatActivity(), UploadBottomSheetFragment.UploadBottomSheetListener {

    private lateinit var inputText: EditText
    private lateinit var outputText: EditText
    private lateinit var progressBar: ProgressBar
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
        .readTimeout(30, TimeUnit.SECONDS)    // Increase read timeout
        .writeTimeout(30, TimeUnit.SECONDS)   // Increase write timeout
        .build()
    private val BASE_URL = "https://ragbackend-production.up.railway.app"
    private val requestLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleUri(it) }
    }
    private var uploadBottomSheetFragment: UploadBottomSheetFragment? = null
    // Variables to store the data
    private var uploadedPdfData: String? = null
    private var uploadedYoutubeData: String? = null
    private var uploadedWebsiteData: String? = null
    private var uploadedAudioData: String? = null
    private var uploadedImageData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputText = findViewById(R.id.input_text)
        outputText = findViewById(R.id.output_text)
        progressBar = findViewById(R.id.progressBar)

//        outputText.setText(("output will be here"))
        // Make outputText non-editable
//        outputText.isFocusable = false
//        outputText.isFocusableInTouchMode = false
//        outputText.inputType = 0 // Disables input

        val uploadFileButton: Button = findViewById(R.id.button5)
        val clearButton: Button = findViewById(R.id.button6)
        val shortSummaryButton: Button = findViewById(R.id.button1)
        val longSummaryButton: Button = findViewById(R.id.button2)
        val generateTaglineButton: Button = findViewById(R.id.button3)
        val customButton: Button = findViewById(R.id.button4)
//        val youtubeButton: Button = findViewById(R.id.upload_youtube_button)
//        val websiteButton: Button = findViewById(R.id.upload_website_button)
//        val queryButton: Button = findViewById(R.id.button_query)

        uploadFileButton.setOnClickListener {uploadBottomSheetFragment = UploadBottomSheetFragment() // Assign here
            uploadBottomSheetFragment?.show(supportFragmentManager, uploadBottomSheetFragment?.tag)
        }

        clearButton.setOnClickListener {
            inputText.text.clear()
            outputText.text.clear()
        }
        shortSummaryButton.setOnClickListener { shortSummarize(getSavedData()) }
        longSummaryButton.setOnClickListener { longSummarize(getSavedData()) }
        generateTaglineButton.setOnClickListener { generateTagline(getSavedData()) }
//        customButton.setOnClickListener { handleCustomOperation() }

//        youtubeButton.setOnClickListener { showYoutubeLinkDialog() }
//        websiteButton.setOnClickListener { showWebsiteLinkDialog() }
        customButton.setOnClickListener { showQueryDialog() }
    }


    // Implementing UploadBottomSheetListener methods
    override fun onUploadPdf() {
        uploadBottomSheetFragment?.dismiss()
        requestLauncher.launch("application/pdf")
    }

    override fun onUploadYoutube() {
        uploadBottomSheetFragment?.dismiss()
        showYoutubeLinkDialog()
    }

    override fun onUploadWebsite() {
        uploadBottomSheetFragment?.dismiss()
        showWebsiteLinkDialog()
    }

    override fun onUploadAudio() {
        uploadBottomSheetFragment?.dismiss()
        requestLauncher.launch("audio/*")
    }

    override fun onUploadImage() {
        uploadBottomSheetFragment?.dismiss()
        requestLauncher.launch("image/*")
    }

    private fun handleUri(uri: Uri) {
        val fileType = contentResolver.getType(uri) ?: return
        when {
            fileType.startsWith("application/pdf") -> uploadFile(uri, "application/pdf")
            fileType.startsWith("audio/") -> uploadFile(uri, "audio/*")
            fileType.startsWith("image/") -> uploadFile(uri, "image/*")
        }
    }
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    private fun uploadFile(uri: Uri, mediaType: String) {
        showProgressBar()
        val file = File(getRealPathFromUri(uri))
        val requestBody = RequestBody.create(mediaType.toMediaTypeOrNull(), file)
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestBody)
            .build()

        val url = when (mediaType) {
            "application/pdf" -> "$BASE_URL/uploadfile/"
            "audio/*" -> "$BASE_URL/uploadaudio/"
            "image/*" -> "$BASE_URL/uploadocr/"
            else -> throw IllegalArgumentException("Unsupported media type")
        }

        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                when (mediaType) {
                    "application/pdf" -> uploadedPdfData = body
                    "audio/*" -> uploadedAudioData = body
                    "image/*" -> uploadedImageData = body
                }
                runOnUiThread {
                    hideProgressBar()
                    inputText.text.clear()
                    inputText.setText(body) }
            }
        })
    }

    private fun uploadYoutubeVideo(url: String) {
        if (!isConnected()) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
            }
            return
        }
        showProgressBar()
        val encodedUrl = Uri.encode(url)
        val request = Request.Builder()
            .url("$BASE_URL/uploadYoutubevideo/?url=$encodedUrl")
            .post(RequestBody.create(null, ByteArray(0))) // Empty body, URL is in the query
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                uploadedYoutubeData = body
                runOnUiThread {
                    hideProgressBar()
                    inputText.text.clear()
                    inputText.setText(body)
                }
            }
        })
    }

    private fun uploadWebsite(url: String) {
        if (!isConnected()) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
            }
            return
        }
        showProgressBar()
        val encodedUrl = Uri.encode(url)
        val request = Request.Builder()
            .url("$BASE_URL/uploadwebsite/?url=$encodedUrl")
            .post(RequestBody.create(null, ByteArray(0))) // Empty body, URL is in the query
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()?.replace("\\n", "\n")
                uploadedWebsiteData = body
                runOnUiThread {
                    hideProgressBar()
                    inputText.text.clear()
                    inputText.setText(body)
                }
            }
        })
    }

    private fun shortSummarize(data: String?) {
        if (!isConnected()) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
            }
            return
        }
        showProgressBar()
        data?.let {
            val encodedData = Uri.encode(it)
            val url = "$BASE_URL/shortsummarize/?data=$encodedData"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to summarize: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body) }
                }
            })
        } ?: run {
            hideProgressBar()
            outputText.text.clear()
            outputText.setText("No data to summarize")
        }
    }

    private fun longSummarize(data: String?) {
        if (!isConnected()) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
            }
            return
        }
        showProgressBar()
        data?.let {
            val encodedData = Uri.encode(it)
            val url = "$BASE_URL/longsummarize/?data=$encodedData"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to summarize: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body) }
                }
            })
        } ?: run {
            hideProgressBar()
            outputText.text.clear()
            outputText.setText("No data to summarize")
        }
    }

    private fun generateTagline(data: String?) {
        if (!isConnected()) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_LONG).show()
            }
            return
        }
        showProgressBar()
        data?.let {
                val encodedData = Uri.encode(it)
            val url = "$BASE_URL/generatetagline/?data=$encodedData"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to generate tagline: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body) }
                }
            })
        } ?: run {
            hideProgressBar()
            outputText.text.clear()
            outputText.setText("No data to generate tagline")
        }
    }

//    private fun handleCustomOperation() {
//        val data = inputText.text.toString()
//        val query = "Sample query" // Replace with the actual query you need to use
//        generateResponse(data, query)
//    }

    private fun getSavedData(): String? {
        return when {
            uploadedPdfData != null -> uploadedPdfData
            uploadedYoutubeData != null -> uploadedYoutubeData
            uploadedWebsiteData != null -> uploadedWebsiteData
            uploadedAudioData != null -> uploadedAudioData
            uploadedImageData != null -> uploadedImageData
            inputText.text.isNotEmpty() -> inputText.text.toString()
            else -> null
        }
    }
    private fun clearSavedData() {
        uploadedPdfData = null
        uploadedYoutubeData = null
        uploadedWebsiteData = null
        uploadedAudioData = null
        uploadedImageData = null
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } else {
            return false
        }
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun showYoutubeLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_youtube_link, null)
        val youtubeUrlEditText: EditText = dialogView.findViewById(R.id.youtube_url_edittext)
        val okButton: Button = dialogView.findViewById(R.id.youtube_ok_button)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter YouTube URL")
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val youtubeUrl = youtubeUrlEditText.text.toString()
            if (youtubeUrl.isNotEmpty()) {
                uploadYoutubeVideo(youtubeUrl)
            } else {
                Toast.makeText(this, "Please enter a valid YouTube URL", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showWebsiteLinkDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_website_link, null)
        val websiteUrlEditText: EditText = dialogView.findViewById(R.id.website_url_edittext)
        val okButton: Button = dialogView.findViewById(R.id.website_ok_button)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Website URL")
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val websiteUrl = websiteUrlEditText.text.toString()
            if (websiteUrl.isNotEmpty()) {
                uploadWebsite(websiteUrl)
            } else {
                Toast.makeText(this, "Please enter a valid website URL", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showQueryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_query, null)
        val queryEditText: EditText = dialogView.findViewById(R.id.query_edittext)
        val okButton: Button = dialogView.findViewById(R.id.query_ok_button)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter your Question")
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val query = queryEditText.text.toString()
            if (query.isNotEmpty()) {
                val data = getSavedData()
                generateResponse(data ?: "", query)
            } else {
                Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generateResponse(data: String, query: String) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }
        showProgressBar()
        val encodedData = Uri.encode(data)
        val encodedQuery = Uri.encode(query)
        val url = "$BASE_URL/response/?data=$encodedData&query=$encodedQuery"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    outputText.text.clear()
                    outputText.setText("Failed to generate response: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    hideProgressBar()
                    outputText.text.clear()
                    outputText.setText(body) }
            }
        })
    }

    private fun getRealPathFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return uri.path ?: ""
    }
}
