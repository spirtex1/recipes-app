package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Экран добавления рецепта. Берём данные из полей, проверяем и сохраняем в базу.
class AddRecipeActivity : AppCompatActivity() {

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

        // Загружаем категории из базы в фоне и заполняем Spinner
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
            }
        }.start()

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val timeText = timeInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()

            // Валидация: пустые поля не должны падать или сохраняться
            if (title.isEmpty()) {
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

            // Запись в базу в фоне, описание сохраняем вместе с рецептом
            Thread {
                dbHelper.addRecipe(title, description, cookTime, categoryId, Session.getUserId(this))
                runOnUiThread {
                    Toast.makeText(this, "Рецепт сохранён", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.start()
        }
    }
}