package ru.shtrih.recipesapp

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Экран добавления и редактирования рецепта.
// Если пришёл EXTRA_RECIPE_ID, поля заполняются и сохранение обновляет рецепт.
class AddRecipeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    private lateinit var dbHelper: DBHelper
    // Категории в том же порядке, что и в Spinner: (id, название)
    private var categories: List<Pair<Long, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        dbHelper = DBHelper(this)

        val titleInput = findViewById<EditText>(R.id.recipeTitleInput)
        val timeInput = findViewById<EditText>(R.id.cookTimeInput)
        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)
        val spinner = findViewById<Spinner>(R.id.categorySpinner)
        val saveButton = findViewById<Button>(R.id.saveRecipeButton)

        // Если передан id, значит открыли редактирование
        val recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        val editing = recipeId != -1L
        val initialCategory = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()

        // В режиме редактирования заполняем поля текущими значениями
        if (editing) {
            titleInput.setText(intent.getStringExtra(EXTRA_TITLE).orEmpty())
            val time = intent.getIntExtra(EXTRA_TIME, 0)
            if (time > 0) timeInput.setText(time.toString())
            descriptionInput.setText(intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty())
        }

        // Загружаем категории в фоне и заполняем Spinner
        Thread {
            categories = dbHelper.getCategories()
            runOnUiThread {
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.second }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
                // При редактировании выбираем текущую категорию рецепта
                if (editing) {
                    val index = categories.indexOfFirst { it.second == initialCategory }
                    if (index >= 0) spinner.setSelection(index)
                }
            }
        }.start()

        saveButton.setOnClickListener {
            val titleText = titleInput.text.toString().trim()
            val timeText = timeInput.text.toString().trim()
            val descriptionText = descriptionInput.text.toString().trim()

            // Валидация: пустые поля не должны падать или сохраняться
            if (titleText.isEmpty()) {
                titleInput.error = "Введите название"
                return@setOnClickListener
            }
            if (timeText.isEmpty()) {
                timeInput.error = "Введите время"
                return@setOnClickListener
            }
            val cookTime = timeText.toIntOrNull()
            if (cookTime == null || cookTime <= 0) {
                timeInput.error = "Введите число больше нуля"
                return@setOnClickListener
            }
            if (categories.isEmpty()) {
                Toast.makeText(this, "Категории ещё не загружены", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryId = categories[spinner.selectedItemPosition].first

            // Запись в базу в фоне: обновляем существующий рецепт или добавляем новый
            Thread {
                if (editing) {
                    dbHelper.updateRecipe(recipeId, titleText, descriptionText, cookTime, categoryId)
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.recipe_updated), Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else {
                    dbHelper.addRecipe(titleText, descriptionText, cookTime, categoryId, Session.getUserId(this))
                    runOnUiThread {
                        Toast.makeText(this, "Рецепт сохранён", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }.start()
        }
    }
}