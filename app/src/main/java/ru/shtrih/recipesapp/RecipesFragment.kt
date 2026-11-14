package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SearchView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Экран «Рецепты»: список, поиск, фильтры по категории и дате, удаление и детали
class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: RecipeAdapter

    // Категории для фильтра. Первый пункт «Все категории» — id -1, значит без фильтра.
    private var filterCategories: List<Pair<Long, String>> = emptyList()
    private var selectedCategoryId: Long = -1L
    private var currentQuery: String = ""
    // Период фильтра в виде чисел вида 20261114. null — без ограничения
    private var filterFrom: Int? = null
    private var filterTo: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recipesList)
        val addButton = view.findViewById<Button>(R.id.addRecipeButton)
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val categoryFilter = view.findViewById<Spinner>(R.id.categoryFilter)
        val dateFilterButton = view.findViewById<Button>(R.id.dateFilterButton)
        val clearDateButton = view.findViewById<Button>(R.id.clearDateButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipeAdapter(
            emptyList(),
            onDelete = { recipe -> deleteRecipe(recipe) },
            onOpen = { recipe -> openDetail(recipe) }
        )
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddRecipeActivity::class.java))
        }

        dateFilterButton.setOnClickListener { showDatePicker(dateFilterButton) }

        clearDateButton.setOnClickListener {
            filterFrom = null
            filterTo = null
            dateFilterButton.text = getString(R.string.date_filter_all)
            reloadList()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty().trim()
                reloadList()
                return true
            }
        })

        // Категории для фильтра загружаем в фоне
        Thread {
            val cats = dbHelper.getCategories()
            onUi {
                filterCategories = listOf(-1L to getString(R.string.filter_all)) + cats
                val filterAdapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_category_selected,
                    R.id.categoryText,
                    filterCategories.map { it.second }
                )
                filterAdapter.setDropDownViewResource(R.layout.item_category)
                categoryFilter.adapter = filterAdapter
                categoryFilter.setPopupBackgroundResource(R.drawable.bg_dropdown)
            }
        }.start()

        // Выбор категории сразу обновляет список
        categoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = filterCategories.getOrNull(position)?.first ?: -1L
                reloadList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при каждом показе, например после добавления рецепта
        reloadList()
    }

    // Выполняем действие на главном потоке, только если фрагмент ещё на экране
    private fun onUi(block: () -> Unit) {
        activity?.runOnUiThread {
            if (isAdded) block()
        }
    }

    // Единая точка загрузки: учитывает поиск, категорию и период
    private fun reloadList() {
        val ctx = requireContext()
        val categoryId = selectedCategoryId
        val query = currentQuery
        val from = filterFrom
        val to = filterTo

        Thread {
            val userId = Session.getUserId(ctx)
            val recipes = (when {
                categoryId != -1L -> dbHelper.getRecipesByCategory(userId, categoryId)
                    .filter { query.isEmpty() || it.title.lowercase().contains(query.lowercase()) }
                query.isNotEmpty() -> dbHelper.searchRecipes(userId, query)
                else -> dbHelper.getRecipes(userId)
            }).filter { recipe ->
                val key = dayKey(recipe.createdAt, TimeZone.getDefault())
                (from == null || key >= from) && (to == null || key <= to)
            }
            onUi {
                adapter.updateItems(recipes)
                showEmptyText(recipes.isEmpty())
            }
        }.start()
    }

    private fun showEmptyText(isEmpty: Boolean) {
        view?.findViewById<View>(R.id.emptyText)?.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    // Удаление в фоне, затем обновляем список и показываем Snackbar
    private fun deleteRecipe(recipe: Recipe) {
        Thread {
            dbHelper.deleteRecipe(recipe.id)
            onUi {
                reloadList()
                Snackbar.make(requireView(), getString(R.string.recipe_deleted), Snackbar.LENGTH_LONG).show()
            }
        }.start()
    }

    // Открываем детали и передаём все данные рецепта
    private fun openDetail(recipe: Recipe) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_ID, recipe.id)
            putExtra(RecipeDetailActivity.EXTRA_TITLE, recipe.title)
            putExtra(RecipeDetailActivity.EXTRA_CATEGORY, recipe.categoryName)
            putExtra(RecipeDetailActivity.EXTRA_TIME, recipe.cookTime)
            putExtra(RecipeDetailActivity.EXTRA_DESCRIPTION, recipe.description)
            putExtra(RecipeDetailActivity.EXTRA_IMAGE, recipe.imagePath)
        }
        startActivity(intent)
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
        picker.show(parentFragmentManager, "date_range")
    }

    // Дата в виде 20261114 для сравнения
    private fun dayKey(millis: Long, zone: TimeZone): Int {
        val c = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    private fun formatDay(millis: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(millis)
    }
}