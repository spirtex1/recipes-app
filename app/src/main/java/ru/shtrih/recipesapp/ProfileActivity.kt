package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

// Экран профиля: карточка пользователя, настройки, данные и выход
class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.themeButton).setOnClickListener { toggleTheme() }
        findViewById<Button>(R.id.statsButton).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<Button>(R.id.adminButton).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener { logout() }

        loadUserInfo()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    // Заполняем карточку: имя, роль, краткая статистика, видимость админки
    private fun loadUserInfo() {
        Thread {
            val userId = Session.getUserId(this)
            val role = dbHelper.getUserRole(userId)
            val login = dbHelper.getUserLogin(userId)
            val recipesCount = dbHelper.getRecipesCount()
            runOnUiThread {
                findViewById<TextView>(R.id.greetingText).text =
                    getString(R.string.profile_greeting, login)
                findViewById<TextView>(R.id.roleText).text =
                    if (role == "admin") getString(R.string.profile_role_admin)
                    else getString(R.string.profile_role_user)
                findViewById<TextView>(R.id.statsSummary).text =
                    getString(R.string.stats_summary, recipesCount)
                findViewById<Button>(R.id.adminButton).visibility =
                    if (role == "admin") View.VISIBLE else View.GONE
                findViewById<Button>(R.id.themeButton).text =
                    if (Session.isDarkTheme(this)) "Светлая тема" else "Тёмная тема"
            }
        }.start()
    }

    // Переключаем тему, сохраняем выбор и обновляем текст кнопки
    private fun toggleTheme() {
        val nowDark = !Session.isDarkTheme(this)
        Session.setDarkTheme(this, nowDark)
        AppCompatDelegate.setDefaultNightMode(
            if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Выход: стираем сессию и открываем экран входа, очистив стек экранов
    private fun logout() {
        Session.logout(this)
        Toast.makeText(this, getString(R.string.logout_done), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}