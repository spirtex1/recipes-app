package ru.shtrih.recipesapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// Экран деталей рецепта. Данные приходят через Intent, отсюда можно открыть редактирование.
class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IMAGE = "extra_image"
    }

    private var recipeId = -1L
    private var recipeName = ""
    private var recipeCategory = ""
    private var recipeTime = 0
    private var recipeText = ""
    private var recipeImage: String? = null

    // Если правка сохранилась, закрываем детали: список обновится сам
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        recipeId = intent.getLongExtra(EXTRA_ID, -1L)
        recipeName = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        recipeCategory = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        recipeTime = intent.getIntExtra(EXTRA_TIME, 0)
        recipeText = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        recipeImage = intent.getStringExtra(EXTRA_IMAGE)

        findViewById<TextView>(R.id.detailTitle).text = recipeName
        findViewById<TextView>(R.id.detailCategory).text = recipeCategory
        findViewById<TextView>(R.id.detailTime).text = "$recipeTime мин"
        findViewById<TextView>(R.id.detailDescription).text = recipeText

        // Фото декодируем в фоне, файл может быть большим
        recipeImage?.let { path ->
            Thread {
                val bitmap = BitmapFactory.decodeFile(path)
                runOnUiThread { findViewById<ImageView>(R.id.detailImage).setImageBitmap(bitmap) }
            }.start()
        }

        // Передаём все данные в экран редактирования, включая фото
        findViewById<Button>(R.id.editButton).setOnClickListener {
            val editIntent = Intent(this, AddRecipeActivity::class.java).apply {
                putExtra(AddRecipeActivity.EXTRA_RECIPE_ID, recipeId)
                putExtra(AddRecipeActivity.EXTRA_TITLE, recipeName)
                putExtra(AddRecipeActivity.EXTRA_CATEGORY, recipeCategory)
                putExtra(AddRecipeActivity.EXTRA_TIME, recipeTime)
                putExtra(AddRecipeActivity.EXTRA_DESCRIPTION, recipeText)
                putExtra(AddRecipeActivity.EXTRA_IMAGE, recipeImage)
            }
            editLauncher.launch(editIntent)
        }

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
    }
}