package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Панель администратора: статистика, список пользователей, смена роли и удаление.
// Доступ только для роли admin.
class AdminActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var usersAdapter: AdminUserAdapter
    private var currentUserId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        dbHelper = DBHelper(this)
        currentUserId = Session.getUserId(this)

        findViewById<Button>(R.id.adminBackButton).setOnClickListener { finish() }

        usersAdapter = AdminUserAdapter(
            currentUserId,
            onToggleRole = { user -> toggleRole(user) },
            onDelete = { user -> confirmDelete(user) }
        )
        findViewById<RecyclerView>(R.id.usersList).apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = usersAdapter
        }

        // Проверяем роль в фоне. Если не админ, закрываем экран.
        Thread {
            val role = dbHelper.getUserRole(currentUserId)
            runOnUiThread {
                if (role != "admin") {
                    Toast.makeText(this, getString(R.string.admin_not_allowed), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    loadData()
                }
            }
        }.start()
    }

    // Статистика и список пользователей, всё в фоне
    private fun loadData() {
        Thread {
            val usersCount = dbHelper.getUsersCount()
            val recipesCount = dbHelper.getRecipesCount()
            val users = dbHelper.getAdminUsers()
            runOnUiThread {
                findViewById<TextView>(R.id.statUsers).text = usersCount.toString()
                findViewById<TextView>(R.id.statRecipes).text = recipesCount.toString()
                findViewById<TextView>(R.id.usersTitle).text =
                    getString(R.string.admin_users_title_count, users.size)
                usersAdapter.updateItems(users)
            }
        }.start()
    }

    // Переключаем роль: user → admin или admin → user
    private fun toggleRole(user: AdminUser) {
        if (user.id == currentUserId) {
            Toast.makeText(this, getString(R.string.admin_self_action), Toast.LENGTH_SHORT).show()
            return
        }
        val newRole = if (user.role == "admin") "user" else "admin"
        Thread {
            dbHelper.setUserRole(user.id, newRole)
            runOnUiThread {
                Toast.makeText(this, getString(R.string.admin_role_changed), Toast.LENGTH_SHORT).show()
                loadData()
            }
        }.start()
    }

    // Перед удалением спрашиваем подтверждение: рецепты удалятся вместе с пользователем
    private fun confirmDelete(user: AdminUser) {
        if (user.id == currentUserId) {
            Toast.makeText(this, getString(R.string.admin_self_action), Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.admin_delete_user))
            .setMessage(getString(R.string.admin_confirm_delete, user.login))
            .setPositiveButton(getString(R.string.admin_delete_user)) { _, _ -> deleteUser(user) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteUser(user: AdminUser) {
        Thread {
            dbHelper.deleteUser(user.id)
            runOnUiThread {
                Toast.makeText(this, getString(R.string.admin_deleted), Toast.LENGTH_SHORT).show()
                loadData()
            }
        }.start()
    }
}