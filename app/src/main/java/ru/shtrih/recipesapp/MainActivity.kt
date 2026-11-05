package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

// Главный экран. Показывает список рецептов из базы, умеет искать и удалять.
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recipesList)
        val addButton = findViewById<Button>(R.id.addRecipeButton)
        val searchView = findViewById<SearchView>(R.id.searchView)

        // Список сверху вниз. Колбэк вызывается при нажатии «Удалить» в карточке.
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList()) { recipe -> deleteRecipe(recipe) }
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        // Ищем по мере ввода текста
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchFor(newText.orEmpty())
                return true
            }
        })

        loadRecipes()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате на экран, например после добавления рецепта
        loadRecipes()
    }

    // Загрузка всех рецептов пользователя в фоне
    private fun loadRecipes() {
        Thread {
            val recipes = dbHelper.getRecipes(Session.getUserId(this))
            runOnUiThread {
                adapter.updateItems(recipes)
                showEmptyText(recipes.isEmpty())
            }
        }.start()
    }

    // Пустой запрос показывает все рецепты, иначе — результаты поиска
    private fun searchFor(query: String) {
        Thread {
            val recipes = if (query.isBlank()) {
                dbHelper.getRecipes(Session.getUserId(this))
            } else {
                dbHelper.searchRecipes(Session.getUserId(this), query.trim())
            }
            runOnUiThread {
                adapter.updateItems(recipes)
                showEmptyText(recipes.isEmpty())
            }
        }.start()
    }

    // Показываем или прячем текст-заглушку
    private fun showEmptyText(isEmpty: Boolean) {
        findViewById<View>(R.id.emptyText).visibility =
            if (isEmpty) View.VISIBLE else View.GONE
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