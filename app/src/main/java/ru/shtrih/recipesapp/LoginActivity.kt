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

        // Если уже вошли раньше, сессия есть, экран входа не нужен
        if (Session.getUserId(this) != -1L) {
            openMain()
            return
        }

        setContentView(R.layout.activity_login)
        dbHelper = DBHelper(this)

        val loginInput = findViewById<EditText>(R.id.loginInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            if (!validate(loginInput, passwordInput)) return@setOnClickListener
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Запрос в базу — в фоне, результат — в главном потоке
            Thread {
                val userId = dbHelper.getUserId(login, password)
                runOnUiThread {
                    if (userId != -1L) {
                        Session.saveUser(this, userId)
                        openMain()
                    } else {
                        Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        registerButton.setOnClickListener {
            if (!validate(loginInput, passwordInput)) return@setOnClickListener
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            Thread {
                val userId = dbHelper.registerUser(login, password)
                runOnUiThread {
                    if (userId != -1L) {
                        Session.saveUser(this, userId)
                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        openMain()
                    } else {
                        Toast.makeText(this, "Такой логин уже существует", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    // Проверка пустых полей: setError, как в TC-02
    private fun validate(loginInput: EditText, passwordInput: EditText): Boolean {
        if (loginInput.text.toString().trim().isEmpty()) {
            loginInput.error = "Введите логин"
            return false
        }
        if (passwordInput.text.toString().trim().isEmpty()) {
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