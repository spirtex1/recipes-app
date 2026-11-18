package ru.shtrih.recipesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

// Экран «Профиль»: карточка пользователя, тема, статистика, смена пароля, админка и выход
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var dbHelper: DBHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        view.findViewById<Button>(R.id.themeButton).setOnClickListener { toggleTheme() }
        view.findViewById<Button>(R.id.statsButton).setOnClickListener {
            startActivity(Intent(requireContext(), StatsActivity::class.java))
        }
        view.findViewById<Button>(R.id.changePasswordButton).setOnClickListener {
            showChangePasswordDialog()
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

    // Заполняем карточку: имя, роль, количество рецептов, видимость админки и текст кнопки темы
    private fun loadUserInfo() {
        val ctx = requireContext()
        Thread {
            val userId = Session.getUserId(ctx)
            val role = dbHelper.getUserRole(userId)
            val login = dbHelper.getUserLogin(userId)
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
                    if (Session.isDarkTheme(ctx)) getString(R.string.theme_light)
                    else getString(R.string.theme_dark)
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

    // Диалог смены пароля: текущий, новый и повтор нового
    private fun showChangePasswordDialog() {
        val ctx = requireContext()
        val padding = resources.getDimensionPixelSize(R.dimen.spacing_large)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
        }
        val oldField = makePasswordField(ctx, container, R.string.password_old)
        val newField = makePasswordField(ctx, container, R.string.password_new)
        val confirmField = makePasswordField(ctx, container, R.string.confirm_hint)

        AlertDialog.Builder(ctx)
            .setTitle(R.string.change_password_title)
            .setView(container)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val oldPass = oldField.text.toString()
                val newPass = newField.text.toString()
                val confirm = confirmField.text.toString()
                when {
                    newPass.length < 4 ->
                        Toast.makeText(ctx, R.string.password_short, Toast.LENGTH_SHORT).show()
                    newPass != confirm ->
                        Toast.makeText(ctx, R.string.passwords_mismatch, Toast.LENGTH_SHORT).show()
                    newPass == oldPass ->
                        Toast.makeText(ctx, R.string.err_password_same, Toast.LENGTH_SHORT).show()
                    else -> changePassword(ctx, oldPass, newPass)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun makePasswordField(ctx: Context, container: LinearLayout, hintRes: Int): EditText {
        val field = EditText(ctx).apply {
            hint = getString(hintRes)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(field)
        return field
    }

    // Меняем пароль в фоне: если текущий неверный, база ничего не меняет
    private fun changePassword(ctx: Context, oldPass: String, newPass: String) {
        val userId = Session.getUserId(ctx)
        Thread {
            val ok = dbHelper.changePassword(userId, oldPass, newPass)
            onUi {
                val message = if (ok) R.string.password_changed else R.string.err_password_wrong
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            }
        }.start()
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