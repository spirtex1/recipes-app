package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DBHelper(this)

        val loginInput = findViewById<EditText>(R.id.loginInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!validate(loginInput, passwordInput)) return@setOnClickListener

            Thread {
                val ok = dbHelper.checkUser(login, password)
                runOnUiThread {
                    if (ok) {
                        openMain()
                    } else {
                        Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        registerButton.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!validate(loginInput, passwordInput)) return@setOnClickListener

            Thread {
                val ok = dbHelper.registerUser(login, password)
                runOnUiThread {
                    if (ok) {
                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        openMain()
                    } else {
                        Toast.makeText(this, "Такой логин уже существует", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun validate(loginInput: EditText, passwordInput: EditText): Boolean {
        val login = loginInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (login.isEmpty()) {
            loginInput.error = "Введите логин"
            return false
        }
        if (password.isEmpty()) {
            passwordInput.error = "Введите пароль"
            return false
        }
        return true
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}