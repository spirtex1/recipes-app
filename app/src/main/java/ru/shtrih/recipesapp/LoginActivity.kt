package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Экран входа. Регистрация открывается отдельным экраном RegisterActivity.
class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если уже вошли раньше, экран входа не нужен
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

            // Проверка в базе идёт в фоне, результат показываем в главном потоке
            Thread {
                val userId = dbHelper.getUserId(login, password)
                runOnUiThread {
                    // Если экран уже закрыт, ничего не делаем
                    if (isFinishing) return@runOnUiThread
                    if (userId != -1L) {
                        Session.saveUser(this, userId)
                        openMain()
                    } else {
                        Toast.makeText(this, R.string.err_login_wrong, Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Проверка пустых полей через setError, как в TC-02
    private fun validate(loginInput: EditText, passwordInput: EditText): Boolean {
        if (loginInput.text.toString().trim().isEmpty()) {
            loginInput.error = getString(R.string.err_login_empty)
            return false
        }
        if (passwordInput.text.toString().trim().isEmpty()) {
            passwordInput.error = getString(R.string.err_password_empty)
            return false
        }
        return true
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}