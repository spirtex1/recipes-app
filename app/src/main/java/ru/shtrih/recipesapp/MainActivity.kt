package ru.shtrih.recipesapp

import androidx.appcompat.app.AppCompatDelegate
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

// Главный экран. Список рецептов, поиск, удаление, детали и нижняя навигация.
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
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Список сверху вниз. Два колбэка: удаление и открытие деталей.
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(
            emptyList(),
            onDelete = { recipe -> deleteRecipe(recipe) },
            onOpen = { recipe -> openDetail(recipe) }
        )
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

        // Нижняя навигация: «Рецепты» остаётся на экране, «Профиль» открывает меню
        bottomNav.selectedItemId = R.id.nav_recipes
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recipes -> true
                R.id.nav_profile -> {
                    showProfileMenu()
                    true
                }
                else -> false
            }
        }

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

    // Открываем экран деталей и передаём данные рецепта через Intent
    private fun openDetail(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_TITLE, recipe.title)
            putExtra(RecipeDetailActivity.EXTRA_CATEGORY, recipe.categoryName)
            putExtra(RecipeDetailActivity.EXTRA_TIME, recipe.cookTime)
            putExtra(RecipeDetailActivity.EXTRA_DESCRIPTION, recipe.description)
        }
        startActivity(intent)
    }

    // Меню профиля. Пункт админки показываем только тем, у кого роль admin.
// Меню профиля: тема, админка (только для админа) и выход
    private fun showProfileMenu() {
        Thread {
            val isAdmin = dbHelper.getUserRole(Session.getUserId(this)) == "admin"
            runOnUiThread {
                val options = mutableListOf(getString(R.string.theme_toggle))
                if (isAdmin) options.add(getString(R.string.admin_panel))
                options.add(getString(R.string.logout))

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.nav_profile))
                    .setItems(options.toTypedArray()) { _, which ->
                        when (options[which]) {
                            getString(R.string.theme_toggle) -> toggleTheme()
                            getString(R.string.admin_panel) -> startActivity(Intent(this, AdminActivity::class.java))
                            getString(R.string.logout) -> logout()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }.start()
    }

    // Переключаем тему и сохраняем выбор. Экран пересоздастся сам.
    private fun toggleTheme() {
        val nowDark = !Session.isDarkTheme(this)
        Session.setDarkTheme(this, nowDark)
        AppCompatDelegate.setDefaultNightMode(
            if (nowDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Выход: стираем сессию и возвращаемся на экран входа, очистив стек экранов
    private fun logout() {
        Session.logout(this)
        Toast.makeText(this, getString(R.string.logout_done), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}