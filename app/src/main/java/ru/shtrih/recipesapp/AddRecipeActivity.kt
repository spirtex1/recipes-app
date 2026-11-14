package ru.shtrih.recipesapp

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// Экран добавления и редактирования рецепта. Если пришёл EXTRA_RECIPE_ID, это правка.
class AddRecipeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IMAGE = "extra_image"

        // Ключи для сохранения состояния при повороте экрана
        private const val KEY_IMAGE_PATH = "state_image_path"
        private const val KEY_CATEGORY_POS = "state_category_pos"
    }

    private lateinit var dbHelper: DBHelper
    private lateinit var categorySpinner: Spinner
    // Категории в том же порядке, что и в Spinner: (id, название)
    private var categories: List<Pair<Long, String>> = emptyList()
    // Путь к фото: старое при редактировании или только что выбранное
    private var imagePath: String? = null

    // Выбор фото из галереи. Копируем и уменьшаем в фоне, потом показываем превью.
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Thread {
                val saved = ImageStorage.saveFromUri(this, uri)
                runOnUiThread {
                    if (saved != null) {
                        imagePath = saved
                        showPreview(saved)
                    } else {
                        Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        dbHelper = DBHelper(this)

        val titleInput = findViewById<EditText>(R.id.recipeTitleInput)
        val timeInput = findViewById<EditText>(R.id.cookTimeInput)
        val descriptionInput = findViewById<EditText>(R.id.descriptionInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        val saveButton = findViewById<Button>(R.id.saveRecipeButton)
        val pickPhotoButton = findViewById<Button>(R.id.pickPhotoButton)

        pickPhotoButton.setOnClickListener { pickPhoto.launch("image/*") }

        // Если пришёл id, открыли редактирование
        val recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        val editing = recipeId != -1L
        val initialCategory = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()

        // Первый запуск: заполняем из Intent. После поворота берём фото из сохранённого состояния,
        // а текст в EditText Android восстанавливает сам.
        if (savedInstanceState != null) {
            imagePath = savedInstanceState.getString(KEY_IMAGE_PATH)
        } else if (editing) {
            titleInput.setText(intent.getStringExtra(EXTRA_TITLE).orEmpty())
            val time = intent.getIntExtra(EXTRA_TIME, 0)
            if (time > 0) timeInput.setText(time.toString())
            descriptionInput.setText(intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty())
            imagePath = intent.getStringExtra(EXTRA_IMAGE)
        }
        // Превью показываем, если фото уже есть
        imagePath?.let { showPreview(it) }

        // Категории в фоне, затем заполняем Spinner и возвращаем выбор
        Thread {
            categories = dbHelper.getCategories()
            runOnUiThread {
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categories.map { it.second }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                val savedPos = savedInstanceState?.getInt(KEY_CATEGORY_POS, -1) ?: -1
                if (savedPos >= 0) {
                    // Поворот экрана: возвращаем ту категорию, которую выбрал пользователь
                    categorySpinner.setSelection(savedPos)
                } else if (editing) {
                    // Правка: ставим категорию рецепта
                    val index = categories.indexOfFirst { it.second == initialCategory }
                    if (index >= 0) categorySpinner.setSelection(index)
                }
            }
        }.start()

        saveButton.setOnClickListener {
            val titleText = titleInput.text.toString().trim()
            val timeText = timeInput.text.toString().trim()
            val descriptionText = descriptionInput.text.toString().trim()

            // Валидация: пустые поля не падают и не сохраняются
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
            val position = categorySpinner.selectedItemPosition
            if (position < 0 || position >= categories.size) {
                Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryId = categories[position].first
            val photo = imagePath

            // Запись в базу в фоне: обновляем рецепт или добавляем новый
            Thread {
                if (editing) {
                    dbHelper.updateRecipe(recipeId, titleText, descriptionText, cookTime, categoryId, photo)
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.recipe_updated), Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else {
                    dbHelper.addRecipe(titleText, descriptionText, cookTime, categoryId, Session.getUserId(this), photo)
                    runOnUiThread {
                        Toast.makeText(this, "Рецепт сохранён", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }.start()
        }
    }

    // Сохраняем фото и выбранную категорию, чтобы пережить поворот экрана
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_IMAGE_PATH, imagePath)
        outState.putInt(KEY_CATEGORY_POS, categorySpinner.selectedItemPosition)
    }

    // Показываем фото в превью. Декодируем здесь, файл маленький, после сжатия.
    private fun showPreview(path: String) {
        findViewById<ImageView>(R.id.photoPreview).setImageBitmap(BitmapFactory.decodeFile(path))
    }
}