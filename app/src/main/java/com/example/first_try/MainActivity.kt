package com.example.first_try

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.ImageButton
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject

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
    private var currentCall: Call? = null
    private var uploadedPdfData: String? = null
    private var uploadedYoutubeData: String? = null
    private var uploadedWebsiteData: String? = null
    private var uploadedAudioData: String? = null
    private var uploadedImageData: String? = null
    private lateinit var logoutButton: ImageButton
    private lateinit var copyAllButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Handler().postDelayed({ initializeUI() }, 3000)}
    private fun initializeUI(){
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
        logoutButton = findViewById(R.id.logout_button)
        copyAllButton = findViewById(R.id.copy_all_button)

//        val youtubeButton: Button = findViewById(R.id.upload_youtube_button)
//        val websiteButton: Button = findViewById(R.id.upload_website_button)
//        val queryButton: Button = findViewById(R.id.button_query)

        uploadFileButton.setOnClickListener {uploadBottomSheetFragment = UploadBottomSheetFragment() // Assign here
            uploadBottomSheetFragment?.show(supportFragmentManager, uploadBottomSheetFragment?.tag)
        }
        logoutButton.setOnClickListener {
            logout()
        }
        clearButton.setOnClickListener {
            inputText.text.clear()
            outputText.text.clear()
            clearSavedData()
            cancelUpload()
        }
        shortSummaryButton.setOnClickListener { shortSummarize(getSavedData()) }
        longSummaryButton.setOnClickListener { longSummarize(getSavedData()) }
        generateTaglineButton.setOnClickListener { generateTagline(getSavedData()) }
//        customButton.setOnClickListener { handleCustomOperation() }

//        youtubeButton.setOnClickListener { showYoutubeLinkDialog() }
//        websiteButton.setOnClickListener { showWebsiteLinkDialog() }
        customButton.setOnClickListener { showQueryDialog() }
        inputText.setOnClickListener {
            showScrollablePopup()
        }
        copyAllButton.setOnClickListener {
            copyTextToClipboard()
        }
    }
    private fun copyTextToClipboard() {
        // Get the ClipboardManager
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Create a ClipData object with the text from outputText EditText
        val clip = android.content.ClipData.newPlainText("Copied Text", outputText.text.toString())
        // Set the clip to the clipboard
        clipboard.setPrimaryClip(clip)
        // Show a toast message to indicate the text has been copied
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showScrollablePopup() {
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_text, null)
        val editableTextView: EditText = dialogView.findViewById(R.id.dialog_text)
        val okButton: Button = dialogView.findViewById(R.id.ok_button)

        // Set the data to the EditText
        editableTextView.setText(getSavedData())

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            // Save the edited text back to the inputText field
            inputText.setText(editableTextView.text.toString())
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun logout() {
        // Clear login info
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Redirect to login screen
        val intent = Intent(this, loginactivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
    private fun cancelUpload() {
        currentCall?.cancel()
        currentCall = null // Reset the call reference
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

        val inputStream = contentResolver.openInputStream(uri) ?: run {
            runOnUiThread {
                hideProgressBar()
                Toast.makeText(this@MainActivity, "Error opening file", Toast.LENGTH_LONG).show()
            }
            return
        }

        val tempFile = File.createTempFile("temp", null, cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        val requestBody = tempFile.asRequestBody(mediaType.toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", tempFile.name, requestBody)
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
        currentCall = client.newCall(request)
        currentCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                tempFile.delete() // Delete temp file on failure
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()?.replace("\\n", "\n")
                when (mediaType) {
                    "application/pdf" -> uploadedPdfData = body
                    "audio/*" -> uploadedAudioData = body
                    "image/*" -> uploadedImageData = body
                }
                runOnUiThread {
                    hideProgressBar()
                    inputText.text.clear()
                    inputText.setText(body)
                }
                tempFile.delete() // Delete temp file after successful upload
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
        currentCall = client.newCall(request)
        currentCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()?.replace("\\n", "\n")
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
        currentCall = client.newCall(request)
        currentCall?.enqueue(object : Callback {
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
            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("data", it)
            }

            // Create request body
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)

            // Build POST request
            val url = "$BASE_URL/shortsummarize/"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            currentCall = client.newCall(request)
            currentCall?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to summarize: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body)
                    }
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
            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("data", it)
            }

            // Create request body
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)

            // Build POST request
            val url = "$BASE_URL/longsummarize/"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            currentCall = client.newCall(request)
            currentCall?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to summarize: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body)
                    }
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
            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("data", it)
            }
            Log.d("RequestPayload", jsonPayload.toString())
            // Create request body
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toString().toRequestBody(mediaType)
            // Build POST request
            val url = "$BASE_URL/generate_tagline/"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            currentCall = client.newCall(request)
            currentCall?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText("Failed to generate tagline: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()?.replace("\\n", "\n")
                    Log.d("ResponseBody", "Response body: $body")
                    runOnUiThread {
                        hideProgressBar()
                        outputText.text.clear()
                        outputText.setText(body)
                    }
                }
            })
        } ?: run {
            hideProgressBar()
            outputText.text.clear()
            outputText.setText("No data to generate tagline")
        }
    }


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

        // Create JSON payload
        val jsonPayload = JSONObject()
        jsonPayload.put("data", data)
        jsonPayload.put("query", query)

        // Create request body
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        // Build POST request
        val url = "$BASE_URL/response/"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        currentCall = client.newCall(request)
        currentCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideProgressBar()
                    outputText.text.clear()
                    outputText.setText("Failed to generate response: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()?.replace("\\n", "\n")
                runOnUiThread {
                    hideProgressBar()
                    outputText.text.clear()
                    outputText.setText(body)
                }
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
