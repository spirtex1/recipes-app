package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

// Экран «Профиль»: карточка пользователя, тема, статистика, админка и выход
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var dbHelper: DBHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        view.findViewById<Button>(R.id.themeButton).setOnClickListener { toggleTheme() }
        view.findViewById<Button>(R.id.statsButton).setOnClickListener {
            startActivity(Intent(requireContext(), StatsActivity::class.java))
        }
        view.findViewById<Button>(R.id.adminButton).setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    // Выполняем действие на главном потоке, только если фрагмент ещё на экране
    private fun onUi(block: () -> Unit) {
        activity?.runOnUiThread {
            if (isAdded) block()
        }
    }

    // Заполняем карточку: имя, роль, количество рецептов пользователя, видимость админки
    private fun loadUserInfo() {
        val ctx = requireContext()
        Thread {
            val userId = Session.getUserId(ctx)
            val role = dbHelper.getUserRole(userId)
            val login = dbHelper.getUserLogin(userId)
            // Считаем только рецепты этого пользователя
            val recipesCount = dbHelper.getCategoryStats(userId).sumOf { it.second }
            onUi {
                val root = requireView()
                root.findViewById<TextView>(R.id.avatarLetter).text = login.take(1).uppercase()
                root.findViewById<TextView>(R.id.greetingText).text =
                    getString(R.string.profile_greeting, login)
                root.findViewById<TextView>(R.id.roleText).text =
                    if (role == "admin") getString(R.string.profile_role_admin)
                    else getString(R.string.profile_role_user)
                root.findViewById<TextView>(R.id.statsSummary).text =
                    getString(R.string.stats_summary, recipesCount)
                root.findViewById<Button>(R.id.adminButton).visibility =
                    if (role == "admin") View.VISIBLE else View.GONE
                root.findViewById<Button>(R.id.themeButton).text =
                    if (Session.isDarkTheme(ctx)) "Светлая тема" else "Тёмная тема"
            }
        }.start()
    }

    // Переключаем тему, сохраняем выбор
    private fun toggleTheme() {
        val ctx = requireContext()
        val nowDark = !Session.isDarkTheme(ctx)
        Session.setDarkTheme(ctx, nowDark)
        AppCompatDelegate.setDefaultNightMode(
            if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Выход: стираем сессию и открываем экран входа, очистив стек экранов
    private fun logout() {
        val ctx = requireContext()
        Session.logout(ctx)
        Toast.makeText(ctx, getString(R.string.logout_done), Toast.LENGTH_SHORT).show()
        startActivity(Intent(ctx, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        requireActivity().finish()
    }
}