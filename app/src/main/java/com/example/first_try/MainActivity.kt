package com.example.first_try

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var inputText: EditText
    private lateinit var outputText: EditText
    private val client = OkHttpClient()
    private val BASE_URL = "https://ragbackend-production.up.railway.app"

    // Launcher for selecting media using Photo Picker
    private val selectMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { handleMediaSelection(it) }
            ?: run { Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show() }
    }

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

        uploadFileButton.setOnClickListener { selectMedia() }
        clearButton.setOnClickListener {
            inputText.text.clear()
            outputText.text.clear()
        }
        shortSummaryButton.setOnClickListener { shortSummarize(inputText.text.toString()) }
        longSummaryButton.setOnClickListener { longSummarize(inputText.text.toString()) }
        generateTaglineButton.setOnClickListener { generateTagline(inputText.text.toString()) }
        customButton.setOnClickListener {
            val data = inputText.text.toString()
            // Handle custom operation here
        }

        // Check API server connection
        checkApiServerConnection()
    }

    // Function to initiate media selection
    private fun selectMedia() {
        selectMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Checks if the device is connected to the internet
    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }

    // Checks if the app can connect to the API server
    private fun checkApiServerConnection() {
        if (!isConnected()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "$BASE_URL/"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Failed to connect to the API server: ${e.message}", Toast.LENGTH_LONG).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Successfully connected to the API server", Toast.LENGTH_LONG).show() }
                } else {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Failed to connect to the API server. Status Code: ${response.code}", Toast.LENGTH_LONG).show() }
                }
            }
        })
    }

    // Handles the selected media URI, checking permissions for Android 14+
    private fun handleMediaSelection(uri: Uri) {
        if (!isConnected()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {
                requestReadMediaVisualUserSelectedPermission()
                return
            }
        }

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val tempFile = createTempFileFromInputStream(inputStream)
            tempFile?.let {
                when {
                    it.name.endsWith(".jpg") || it.name.endsWith(".png") -> uploadImageFile(it)
                    it.name.endsWith(".mp3") || it.name.endsWith(".wav") -> uploadAudioFile(it)
                    else -> uploadFile(it)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error handling media: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Requests the necessary permission for Android 14+
    private fun requestReadMediaVisualUserSelectedPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED), 2)
    }

    // Creates a temporary file from an InputStream
    private fun createTempFileFromInputStream(inputStream: InputStream?): File? {
        inputStream?.use { input ->
            val tempFile = File.createTempFile("temp", null, cacheDir)
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            return tempFile
        }
        return null
    }

    // Uploads a generic file
    private fun uploadFile(file: File) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }


        if (!file.name.endsWith(".pdf", ignoreCase = true)) {
            outputText.setText("Only PDF files are allowed")
            return
        }
        val url = "$BASE_URL/uploadfile/"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create("application/pdf".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputText.setText("Failed to upload PDF: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread { outputText.setText(body) }
            }
        })
    }

    // Uploads an audio file
    private fun uploadAudioFile(file: File) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }

        val url = "$BASE_URL/uploadaudio/"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create("audio/*".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputText.setText("Failed to upload audio: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread { outputText.setText(body) }
            }
        })
    }

    // Uploads an image file
    private fun uploadImageFile(file: File) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }

        val url = "$BASE_URL/uploadocr/"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create("image/*".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputText.setText("Failed to upload image: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread { outputText.setText(body) }
            }
        })
    }
    // Performs short summarization
    private fun shortSummarize(data: String) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }

        val encodedData = Uri.encode(data)
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
    }

    // Performs long summarization
    private fun longSummarize(data: String) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }

        val encodedData = Uri.encode(data)
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
    }

    // Generates a tagline
    private fun generateTagline(data: String) {
        if (!isConnected()) {
            outputText.setText("No internet connection")
            return
        }

        val encodedData = Uri.encode(data)
        val url = "$BASE_URL/generatetagline/?data=$encodedData"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { outputText.setText("Failed to generate tagline: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread { outputText.setText(body) }
            }
        })
    }
}
