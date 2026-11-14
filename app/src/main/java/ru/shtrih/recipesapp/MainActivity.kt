package ru.shtrih.recipesapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

// Главный контейнер: держит нижнюю навигацию и показывает фрагменты «Рецепты» и «Профиль»
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recipes -> {
                    showFragment(RecipesFragment())
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Первый запуск открывает «Рецепты». После поворота фрагмент восстанавливается сам.
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_recipes
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}