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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), UploadBottomSheetFragment.UploadBottomSheetListener {

    private lateinit var inputText: EditText
    private lateinit var outputText: EditText
    private val client = OkHttpClient()
    private val BASE_URL = "https://ragbackend-production.up.railway.app"
    private val requestLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleUri(it) }
    }

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

        val uploadFileButton: Button = findViewById(R.id.button5)
        val clearButton: Button = findViewById(R.id.button6)
        val shortSummaryButton: Button = findViewById(R.id.button1)
        val longSummaryButton: Button = findViewById(R.id.button2)
        val generateTaglineButton: Button = findViewById(R.id.button3)
        val customButton: Button = findViewById(R.id.button4)

        uploadFileButton.setOnClickListener {
            val uploadBottomSheetFragment = UploadBottomSheetFragment()
            uploadBottomSheetFragment.show(supportFragmentManager, uploadBottomSheetFragment.tag)
        }

        clearButton.setOnClickListener {
            inputText.text.clear()
            outputText.text.clear()
        }
        shortSummaryButton.setOnClickListener { shortSummarize(getSavedData()) }
        longSummaryButton.setOnClickListener { longSummarize(getSavedData()) }
        generateTaglineButton.setOnClickListener { generateTagline(getSavedData()) }
        customButton.setOnClickListener { handleCustomOperation() }
    }

    // Implementing UploadBottomSheetListener methods
    override fun onUploadPdf() {
        requestLauncher.launch("application/pdf")
    }

    override fun onUploadYoutube() {
        // Prompt user to enter YouTube URL or handle through an intent
        val url = "https://youtube.com/sample_video" // This should be fetched from user input
        uploadYoutubeVideo(url)
    }

    override fun onUploadWebsite() {
        // Prompt user to enter website URL or handle through an intent
        val url = "https://example.com" // This should be fetched from user input
        uploadWebsite(url)
    }

    override fun onUploadAudio() {
        requestLauncher.launch("audio/*")
    }

    override fun onUploadImage() {
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

    private fun uploadFile(uri: Uri, mediaType: String) {
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
                runOnUiThread { Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                when (mediaType) {
                    "application/pdf" -> uploadedPdfData = body
                    "audio/*" -> uploadedAudioData = body
                    "image/*" -> uploadedImageData = body
                }
                runOnUiThread { inputText.setText(body) }
            }
        })
    }

    private fun uploadYoutubeVideo(url: String) {
        val request = Request.Builder()
            .url("$BASE_URL/uploadYoutubevideo/?url=$url")
            .post(RequestBody.create(null, ByteArray(0))) // Empty body, URL is in the query
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                uploadedYoutubeData = body
                runOnUiThread { inputText.setText(body) }
            }
        })
    }

    private fun uploadWebsite(url: String) {
        val request = Request.Builder()
            .url("$BASE_URL/uploadwebsite/?url=$url")
            .post(RequestBody.create(null, ByteArray(0))) // Empty body, URL is in the query
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                uploadedWebsiteData = body
                runOnUiThread { inputText.setText(body) }
            }
        })
    }

    private fun shortSummarize(data: String?) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }
        data?.let {
            val encodedData = Uri.encode(it)
            val url = "$BASE_URL/shortsummarize/?data=$encodedData"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { outputText.setText("Failed to summarize: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    runOnUiThread { outputText.setText(body) }
                }
            })
        } ?: run {
            outputText.setText("No data to summarize")
        }
    }

    private fun longSummarize(data: String?) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }
        data?.let {
            val encodedData = Uri.encode(it)
            val url = "$BASE_URL/longsummarize/?data=$encodedData"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { outputText.setText("Failed to summarize: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    runOnUiThread { outputText.setText(body) }
                }
            })
        } ?: run {
            outputText.setText("No data to summarize")
        }
    }

    private fun generateTagline(data: String?) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }
        data?.let {
            val json = "{\"data\":\"$it\"}"
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)
            val request = Request.Builder()
                .url("$BASE_URL/tagline/")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { outputText.setText("Failed to generate tagline: ${e.message}") }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    runOnUiThread { outputText.setText(body) }
                }
            })
        } ?: run {
            outputText.setText("No data to generate tagline")
        }
    }

    private fun handleCustomOperation() {
        val data = inputText.text.toString()
        val query = "Sample query" // Replace with the actual query you need to use
        generateResponse(data, query)
    }

    private fun generateResponse(data: String, query: String) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }
        val json = "{\"data\":\"$data\", \"query\":\"$query\"}"
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url("$BASE_URL/response/")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputText.setText("Failed to generate response: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread { outputText.setText(body) }
            }
        })
    }

    private fun getSavedData(): String? {
        // Combine data from all upload sources
        return uploadedPdfData ?: uploadedYoutubeData ?: uploadedWebsiteData ?: uploadedAudioData ?: uploadedImageData
    }

    private fun getRealPathFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(column_index)
            }
        }
        return ""
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
}
