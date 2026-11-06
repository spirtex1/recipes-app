package ru.shtrih.recipesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// Экран деталей рецепта. Получает данные через Intent и умеет открыть редактирование.
class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    private var recipeId: Long = -1L
    private var recipeName: String = ""
    private var recipeCategory: String = ""
    private var recipeTime: Int = 0
    private var recipeText: String = ""

    // Если редактирование сохранило изменения, закрываем детали: список обновится сам
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

        findViewById<TextView>(R.id.detailTitle).text = recipeName
        findViewById<TextView>(R.id.detailCategory).text = recipeCategory
        findViewById<TextView>(R.id.detailTime).text = "$recipeTime мин"
        findViewById<TextView>(R.id.detailDescription).text = recipeText

        // Передаём текущие данные в экран редактирования
        findViewById<Button>(R.id.editButton).setOnClickListener {
            val editIntent = Intent(this, AddRecipeActivity::class.java).apply {
                putExtra(AddRecipeActivity.EXTRA_RECIPE_ID, recipeId)
                putExtra(AddRecipeActivity.EXTRA_TITLE, recipeName)
                putExtra(AddRecipeActivity.EXTRA_CATEGORY, recipeCategory)
                putExtra(AddRecipeActivity.EXTRA_TIME, recipeTime)
                putExtra(AddRecipeActivity.EXTRA_DESCRIPTION, recipeText)
            }
            editLauncher.launch(editIntent)
        }

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
    }
}