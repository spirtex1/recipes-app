package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Экран деталей рецепта. Получает данные через Intent и показывает их на экране.
class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        // Ключи для передачи данных из главного экрана
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        findViewById<TextView>(R.id.detailTitle).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<TextView>(R.id.detailCategory).text = intent.getStringExtra(EXTRA_CATEGORY)
        findViewById<TextView>(R.id.detailTime).text =
            "${intent.getIntExtra(EXTRA_TIME, 0)} мин"
        findViewById<TextView>(R.id.detailDescription).text =
            intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
    }
}