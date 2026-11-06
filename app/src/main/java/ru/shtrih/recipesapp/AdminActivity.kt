package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Панель администратора. Пускаем только пользователей с ролью admin.
class AdminActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.adminBackButton).setOnClickListener { finish() }

        // Проверяем роль в фоне. Если не админ, закрываем экран.
        Thread {
            val role = dbHelper.getUserRole(Session.getUserId(this))
            runOnUiThread {
                if (role != "admin") {
                    Toast.makeText(this, getString(R.string.admin_not_allowed), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    loadStats()
                }
            }
        }.start()
    }

    // Статистика и список пользователей, тоже в фоне
    private fun loadStats() {
        Thread {
            val usersCount = dbHelper.getUsersCount()
            val recipesCount = dbHelper.getRecipesCount()
            val users = dbHelper.getAllUsers()
            runOnUiThread {
                findViewById<TextView>(R.id.statsText).text =
                    getString(R.string.admin_stats, usersCount, recipesCount)
                findViewById<TextView>(R.id.usersText).text =
                    users.joinToString("\n") { (login, role) -> "$login — $role" }
            }
        }.start()
    }
}