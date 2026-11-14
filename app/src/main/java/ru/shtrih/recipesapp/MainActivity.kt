package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SearchView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Главный экран. Список рецептов, поиск, фильтр по категории и дате, удаление, детали и нижняя навигация.
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: RecipeAdapter

    // Категории для фильтра. Первый пункт «Все категории» — id -1, значит без фильтра.
    private var filterCategories: List<Pair<Long, String>> = emptyList()
    private var selectedCategoryId: Long = -1L
    // Текст поиска держим отдельно, чтобы фильтр и поиск работали вместе
    private var currentQuery: String = ""
    // Период фильтра в виде чисел вида 20261114. null — без ограничения
    private var filterFrom: Int? = null
    private var filterTo: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recipesList)
        val addButton = findViewById<Button>(R.id.addRecipeButton)
        val searchView = findViewById<SearchView>(R.id.searchView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val categoryFilter = findViewById<Spinner>(R.id.categoryFilter)
        val dateFilterButton = findViewById<Button>(R.id.dateFilterButton)
        val clearDateButton = findViewById<Button>(R.id.clearDateButton)

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

        // Выбор периода: открываем календарь с диапазоном дат
        dateFilterButton.setOnClickListener { showDatePicker(dateFilterButton) }

        // Сброс периода: показываем все даты
        clearDateButton.setOnClickListener {
            filterFrom = null
            filterTo = null
            dateFilterButton.text = getString(R.string.date_filter_all)
            reloadList()
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
                    R.layout.item_category_selected,
                    R.id.categoryText,
                    filterCategories.map { it.second }
                )
                filterAdapter.setDropDownViewResource(R.layout.item_category)
                categoryFilter.adapter = filterAdapter
                // Фон выпадающего окна
                categoryFilter.setPopupBackgroundResource(R.drawable.bg_dropdown)
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

        // Нижняя навигация: «Рецепты» остаётся на экране, «Профиль» открывает отдельный экран
        bottomNav.selectedItemId = R.id.nav_recipes
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recipes -> true
                R.id.nav_profile -> {
                    openProfile()
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

    // Календарь с диапазоном дат. Результат переводим в числа вида 20261114.
    private fun showDatePicker(button: Button) {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.date_picker_title))
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first
            val end = selection.second
            filterFrom = dayKey(start, TimeZone.getTimeZone("UTC"))
            filterTo = dayKey(end, TimeZone.getTimeZone("UTC"))
            button.text = getString(R.string.date_range_format, formatDay(start), formatDay(end))
            reloadList()
        }
        picker.show(supportFragmentManager, "date_range")
    }

    // Дата в виде 20261114 для сравнения. Календарь в UTC — чтобы выбранный день не сдвигался.
    private fun dayKey(millis: Long, zone: TimeZone): Int {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    private fun formatDay(millis: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(millis)
    }

    // Единая точка загрузки: учитывает поиск, фильтр по категории и период.
    private fun reloadList() {
        // Копируем значения фильтров до запуска потока
        val categoryId = selectedCategoryId
        val query = currentQuery
        val from = filterFrom
        val to = filterTo

        Thread {
            val userId = Session.getUserId(this)
            val recipes = (when {
                categoryId != -1L -> dbHelper.getRecipesByCategory(userId, categoryId)
                    .filter { query.isEmpty() || it.title.lowercase().contains(query.lowercase()) }
                query.isNotEmpty() -> dbHelper.searchRecipes(userId, query)
                else -> dbHelper.getRecipes(userId)
            }).filter { recipe ->
                val key = dayKey(recipe.createdAt, TimeZone.getDefault())
                (from == null || key >= from) && (to == null || key <= to)
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

    // Открываем детали и передаём все данные рецепта, включая путь к фото
    private fun openDetail(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_ID, recipe.id)
            putExtra(RecipeDetailActivity.EXTRA_TITLE, recipe.title)
            putExtra(RecipeDetailActivity.EXTRA_CATEGORY, recipe.categoryName)
            putExtra(RecipeDetailActivity.EXTRA_TIME, recipe.cookTime)
            putExtra(RecipeDetailActivity.EXTRA_DESCRIPTION, recipe.description)
            putExtra(RecipeDetailActivity.EXTRA_IMAGE, recipe.imagePath)
        }
        startActivity(intent)
    }

    // Профиль открывается как отдельный экран
    private fun openProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }
}