package com.example.first_try

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import at.favre.lib.crypto.bcrypt.BCrypt
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var spinnerSecurityQuestion: Spinner
    private lateinit var editTextSecurityAnswer: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        editTextName = findViewById(R.id.editTextName)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        spinnerSecurityQuestion = findViewById(R.id.spinnerSecurityQuestion)
        editTextSecurityAnswer = findViewById(R.id.editTextText3)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        database = FirebaseDatabase.getInstance().reference

        ArrayAdapter.createFromResource(
            this,
            R.array.security_questions_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSecurityQuestion.adapter = adapter
        }

        buttonSubmit.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()
            val securityQuestion = spinnerSecurityQuestion.selectedItem.toString()
            val securityAnswer = editTextSecurityAnswer.text.toString().trim()

            if (validateInputs(name, username, password, confirmPassword, securityAnswer)) {
                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    val usersRef = database.child("users").child(username)
                    usersRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            // Hash the password
                            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

                            // Save user to Firebase (using hashedPassword)
                            val user = User(name, username, hashedPassword, securityQuestion, securityAnswer)
                            usersRef.setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                                    redirectToLogin()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }
        }
    }

    private fun validateInputs(name: String, username: String, password: String, confirmPassword: String, securityAnswer: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            editTextName.error = "Name is required"
            return false
        }
        if (TextUtils.isEmpty(username)) {
            editTextUsername.error = "Username is required"
            return false
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.error = "Password is required"
            return false
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.error = "Confirm Password is required"
            return false
        }
        if (TextUtils.isEmpty(securityAnswer)) {
            editTextSecurityAnswer.error = "Security Answer is required"
            return false
        }
        return true
    }

    private fun redirectToLogin() {
        val intent = Intent(this, loginactivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}

data class User(
    val name: String = "",
    val username: String = "",
    val password: String = "", // Stores hashed password
    val securityQuestion: String = "",
    val securityAnswer: String = ""
)