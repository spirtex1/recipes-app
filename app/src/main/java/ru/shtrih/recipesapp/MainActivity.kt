package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

// Главный экран. Список рецептов, поиск, фильтр по категории, удаление, детали и нижняя навигация.
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: RecipeAdapter

    // Категории для фильтра. Первый пункт «Все категории» — id -1, значит без фильтра.
    private var filterCategories: List<Pair<Long, String>> = emptyList()
    private var selectedCategoryId: Long = -1L
    // Текст поиска держим отдельно, чтобы фильтр и поиск работали вместе
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recipesList)
        val addButton = findViewById<Button>(R.id.addRecipeButton)
        val searchView = findViewById<SearchView>(R.id.searchView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val categoryFilter = findViewById<Spinner>(R.id.categoryFilter)

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

        // Поиск по мере ввода. Запоминаем текст, чтобы фильтр его учитывал.
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty().trim()
                reloadList()
                return true
            }
        })

        // Загружаем категории в фильтр в фоне
        Thread {
            val cats = dbHelper.getCategories()
            runOnUiThread {
                filterCategories = listOf(-1L to getString(R.string.filter_all)) + cats
                val filterAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    filterCategories.map { it.second }
                )
                filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categoryFilter.adapter = filterAdapter
            }
        }.start()

        // Выбор категории в фильтре сразу обновляет список
        categoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = filterCategories.getOrNull(position)?.first ?: -1L
                reloadList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        reloadList()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате на экран, например после добавления рецепта
        reloadList()
    }

    // Единая точка загрузки: учитывает и поиск, и фильтр по категории.
    // Если выбран фильтр, берём рецепты категории, иначе все, а поиск применяем поверх.
    private fun reloadList() {
        Thread {
            val userId = Session.getUserId(this)
            val recipes = when {
                selectedCategoryId != -1L -> dbHelper.getRecipesByCategory(userId, selectedCategoryId)
                    .filter { currentQuery.isEmpty() || it.title.lowercase().contains(currentQuery.lowercase()) }
                currentQuery.isNotEmpty() -> dbHelper.searchRecipes(userId, currentQuery)
                else -> dbHelper.getRecipes(userId)
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
                reloadList()
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