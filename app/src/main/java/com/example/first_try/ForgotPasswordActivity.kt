package com.example.first_try

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var spinnerSecurityQuestion: Spinner
    private lateinit var editTextSecurityAnswer:EditText
    private lateinit var editTextNewPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonResetPassword: Button
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        editTextUsername = findViewById(R.id.editTextUsername)
        spinnerSecurityQuestion = findViewById(R.id.spinnerSecurityQuestion)
        editTextSecurityAnswer = findViewById(R.id.editTextSecurityAnswer)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonResetPassword = findViewById(R.id.buttonResetPassword)

        database = FirebaseDatabase.getInstance().reference

        // Set up the security question spinner (same as in SignupActivity)
        ArrayAdapter.createFromResource(
            this,
            R.array.security_questions_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSecurityQuestion.adapter = adapter
        }

        buttonResetPassword.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val securityQuestion = spinnerSecurityQuestion.selectedItem.toString()
            val securityAnswer = editTextSecurityAnswer.text.toString().trim()
            val newPassword = editTextNewPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            if (username.isEmpty() || securityAnswer.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRef = database.child("users").child(username)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null && user.securityQuestion == securityQuestion && user.securityAnswer == securityAnswer) {
                            // Security answer matches - update password
                            val hashedNewPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
                            userRef.child("password").setValue(hashedNewPassword)
                                .addOnSuccessListener {
                                    Toast.makeText(this@ForgotPasswordActivity, "Password reset successful", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@ForgotPasswordActivity, "Password reset failed", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Invalid security answer
                            Toast.makeText(this@ForgotPasswordActivity, "Invalid security answer", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // User not found
                        Toast.makeText(this@ForgotPasswordActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handleerrors
                    Toast.makeText(this@ForgotPasswordActivity, "Error fetching user data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}