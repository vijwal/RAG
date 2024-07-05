package com.example.first_try

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class loginactivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginactivity)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login_button)
        signupButton = findViewById(R.id.signup_button)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView)
        database = FirebaseDatabase.getInstance().reference // Initialize database

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            val userRef = database.child("users").child(username)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null && BCrypt.verifyer().verify(password.toCharArray(), user.password).verified) {
                            // Login successful (password matches hash)
                            Toast.makeText(this@loginactivity, "Login successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@loginactivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Invalid credentials
                            Toast.makeText(this@loginactivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // User not found
                        Toast.makeText(this@loginactivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle errors
                    Toast.makeText(this@loginactivity, "Error fetching user data", Toast.LENGTH_SHORT).show()
                }
            })
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Forgot Password functionality
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}