package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginInput = findViewById<EditText>(R.id.loginInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (login.isEmpty()) {
                loginInput.error = "Введите логин"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "Введите пароль"
                return@setOnClickListener
            }

            Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show()
        }
    }
}