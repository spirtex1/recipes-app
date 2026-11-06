package ru.shtrih.recipesapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

// Применяем сохранённую тему один раз при старте приложения
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            if (Session.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}