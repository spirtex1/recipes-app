package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Экран регистрации: проверяем поля, создаём пользователя и сразу входим
class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DBHelper(this)

        val loginInput = findViewById<EditText>(R.id.loginInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmInput = findViewById<EditText>(R.id.confirmInput)
        val submitButton = findViewById<Button>(R.id.registerSubmitButton)
        val backButton = findViewById<Button>(R.id.backToLoginButton)

        submitButton.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirm = confirmInput.text.toString().trim()

            // Валидация: пустые поля, короткий пароль и несовпадение паролей
            if (login.isEmpty()) {
                loginInput.error = getString(R.string.err_login_empty)
                return@setOnClickListener
            }
            if (password.length < 4) {
                passwordInput.error = getString(R.string.password_short)
                return@setOnClickListener
            }
            if (confirm != password) {
                confirmInput.error = getString(R.string.passwords_mismatch)
                return@setOnClickListener
            }

            // Запись в базу в фоне
            Thread {
                val userId = dbHelper.registerUser(login, password)
                runOnUiThread {
                    if (userId != -1L) {
                        Session.saveUser(this, userId)
                        Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, getString(R.string.login_exists), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        backButton.setOnClickListener { finish() }
    }
}