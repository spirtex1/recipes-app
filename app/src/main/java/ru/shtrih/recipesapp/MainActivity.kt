package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Главный экран. Показывает список рецептов из базы.
class MainActivity : AppCompatActivity() {

    // Наш помощник для работы с SQLite
    private lateinit var dbHelper: DBHelper
    // Адаптер связывает список рецептов с RecyclerView
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recipesList)
        val emptyText = findViewById<View>(R.id.emptyText)
        val addButton = findViewById<Button>(R.id.addRecipeButton)

        // Список сверху вниз, как обычная лента
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList())
        recyclerView.adapter = adapter

        // Кнопка пока только показывает заглушку, экран добавления сделаем отдельно
        addButton.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        // Грузим рецепты в фоне, а результат показываем в главном потоке
        loadRecipes(emptyText)
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате на экран, например после добавления рецепта
        loadRecipes(findViewById(R.id.emptyText))
    }

    private fun loadRecipes(emptyText: View) {
        Thread {
            val recipes = dbHelper.getRecipes()
            runOnUiThread {
                adapter.updateItems(recipes)
                // Если рецептов нет, показываем текст-заглушку
                emptyText.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }
}