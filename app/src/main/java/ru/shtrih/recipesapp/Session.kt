package ru.shtrih.recipesapp

import android.content.Context

// Сессия: запоминаем id пользователя в SharedPreferences.
// Эти данные живут в телефоне и переживают закрытие приложения.
object Session {

    private const val PREFS_NAME = "session"
    private const val KEY_USER_ID = "user_id"

    // Сохраняем id после успешного входа или регистрации
    fun saveUser(context: Context, userId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    // Возвращает id или -1, если никто не вошёл
    fun getUserId(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)
    }

    // Выход: стираем id, при следующем запуске попадём на экран входа
    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_ID)
            .apply()
    }
}