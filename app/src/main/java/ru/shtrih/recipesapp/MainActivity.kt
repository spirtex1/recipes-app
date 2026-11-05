package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

// Главный экран. Показывает список рецептов из базы.
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recipesList)
        val addButton = findViewById<Button>(R.id.addRecipeButton)

        // Список сверху вниз. Колбэк вызывается при нажатии «Удалить» в карточке.
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList()) { recipe -> deleteRecipe(recipe) }
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        loadRecipes()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате на экран, например после добавления рецепта
        loadRecipes()
    }

    // Загрузка рецептов в фоне, результат показываем в главном потоке
    private fun loadRecipes() {
        Thread {
            val recipes = dbHelper.getRecipes(Session.getUserId(this))
            runOnUiThread {
                adapter.updateItems(recipes)
                // Если рецептов нет, показываем текст-заглушку
                findViewById<View>(R.id.emptyText).visibility =
                    if (recipes.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    // Удаление в фоне, затем обновляем список и показываем Snackbar
    private fun deleteRecipe(recipe: Recipe) {
        Thread {
            dbHelper.deleteRecipe(recipe.id)
            runOnUiThread {
                loadRecipes()
                Snackbar.make(
                    findViewById(R.id.recipesList),
                    getString(R.string.recipe_deleted),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }.start()
    }
}