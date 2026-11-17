package ru.shtrih.recipesapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

// Экран деталей рецепта: фото, оценка, избранное, комментарии и рекомендации
class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IMAGE = "extra_image"
    }

    private lateinit var dbHelper: DBHelper
    private var recipeId = -1L
    private var userId = -1L

    // Данные для экрана редактирования
    private var recipeName = ""
    private var recipeCategory = ""
    private var recipeTime = 0
    private var recipeText = ""
    private var recipeImage: String? = null

    private lateinit var detailTitle: TextView
    private lateinit var detailCategory: TextView
    private lateinit var detailTime: TextView
    private lateinit var detailAuthor: TextView
    private lateinit var detailDescription: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var ratingSummary: TextView
    private lateinit var favoriteButton: MaterialButton
    private lateinit var commentsContainer: LinearLayout
    private lateinit var commentInput: EditText
    private lateinit var recommendContainer: LinearLayout

    // Если правка сохранилась, закрываем детали: список обновится сам
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        dbHelper = DBHelper(this)
        userId = Session.getUserId(this)
        recipeId = intent.getLongExtra(EXTRA_ID, -1L)
        if (recipeId == -1L) {
            finish()
            return
        }

        recipeName = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        recipeCategory = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        recipeTime = intent.getIntExtra(EXTRA_TIME, 0)
        recipeText = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        recipeImage = intent.getStringExtra(EXTRA_IMAGE)

        detailTitle = findViewById(R.id.detailTitle)
        detailCategory = findViewById(R.id.detailCategory)
        detailTime = findViewById(R.id.detailTime)
        detailAuthor = findViewById(R.id.detailAuthor)
        detailDescription = findViewById(R.id.detailDescription)
        ratingBar = findViewById(R.id.ratingBar)
        ratingSummary = findViewById(R.id.ratingSummary)
        favoriteButton = findViewById(R.id.favoriteButton)
        commentsContainer = findViewById(R.id.commentsContainer)
        commentInput = findViewById(R.id.commentInput)
        recommendContainer = findViewById(R.id.recommendContainer)

        // Пока грузим из базы, показываем данные из Intent
        detailTitle.text = recipeName
        detailCategory.text = recipeCategory
        detailTime.text = getString(R.string.minutes_format, recipeTime)
        detailDescription.text = recipeText
        loadImage(recipeImage)

        // Оценка: реагируем только на действия пользователя, а не на программную установку
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser && rating >= 1f) saveRating(rating.toInt())
        }

        favoriteButton.setOnClickListener { toggleFavorite() }
        findViewById<Button>(R.id.sendCommentButton).setOnClickListener { sendComment() }
        findViewById<Button>(R.id.refreshRecommendButton).setOnClickListener { loadRecommendations() }

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

        loadAll()
        loadRecommendations()
    }

    // Фото декодируем в фоне, файл может быть большим
    private fun loadImage(path: String?) {
        if (path == null) return
        Thread {
            val bitmap = BitmapFactory.decodeFile(path)
            runOnUiThread {
                findViewById<ImageView>(R.id.detailImage).setImageBitmap(bitmap)
            }
        }.start()
    }

    // Загружаем свежие данные рецепта, комментарии и мою оценку
    private fun loadAll() {
        Thread {
            val recipe = dbHelper.getRecipeById(userId, recipeId)
            val comments = dbHelper.getComments(recipeId)
            val myRating = dbHelper.getUserRating(recipeId, userId)
            val role = dbHelper.getUserRole(userId)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (recipe == null) {
                    Toast.makeText(this, R.string.recipe_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }
                showRecipe(recipe, myRating)
                showComments(comments, role)
            }
        }.start()
    }

    private fun showRecipe(recipe: Recipe, myRating: Int) {
        recipeName = recipe.title
        recipeCategory = recipe.categoryName
        recipeTime = recipe.cookTime
        recipeText = recipe.description

        detailTitle.text = recipe.title
        detailCategory.text = recipe.categoryName
        detailTime.text = getString(R.string.minutes_format, recipe.cookTime)
        detailAuthor.text = getString(R.string.author_label, recipe.authorName)
        detailDescription.text = recipe.description

        ratingSummary.text = if (recipe.ratingCount == 0) {
            getString(R.string.rating_none)
        } else {
            getString(R.string.rating_summary, recipe.avgRating, recipe.ratingCount)
        }
        ratingBar.rating = myRating.toFloat()
        favoriteButton.setText(
            if (recipe.isFavorite) R.string.favorite_remove else R.string.favorite_add
        )
    }

    private fun saveRating(score: Int) {
        Thread {
            dbHelper.setRating(recipeId, userId, score)
            runOnUiThread {
                Toast.makeText(this, R.string.rating_saved, Toast.LENGTH_SHORT).show()
                loadAll()
            }
        }.start()
    }

    private fun toggleFavorite() {
        Thread {
            dbHelper.toggleFavorite(recipeId, userId)
            runOnUiThread { loadAll() }
        }.start()
    }

    private fun sendComment() {
        val text = commentInput.text.toString().trim()
        if (text.isEmpty()) {
            commentInput.error = getString(R.string.err_comment_empty)
            return
        }
        Thread {
            dbHelper.addComment(recipeId, userId, text)
            runOnUiThread {
                commentInput.setText("")
                loadAll()
            }
        }.start()
    }

    private fun showComments(comments: List<RecipeComment>, role: String) {
        commentsContainer.removeAllViews()
        if (comments.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.no_comments)
                setPadding(0, resources.getDimensionPixelSize(R.dimen.spacing_small), 0, 0)
            }
            commentsContainer.addView(empty)
            return
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        comments.forEach { comment ->
            val card = layoutInflater.inflate(R.layout.item_comment, commentsContainer, false)
            card.findViewById<TextView>(R.id.commentAvatar).text = comment.authorName.take(1).uppercase()
            card.findViewById<TextView>(R.id.commentAuthor).text = comment.authorName
            card.findViewById<TextView>(R.id.commentDate).text = dateFormat.format(Date(comment.createdAt))
            card.findViewById<TextView>(R.id.commentText).text = comment.text

            // Долгое нажатие: удалить можно свой комментарий или любой, если ты админ
            card.setOnLongClickListener {
                if (comment.authorId == userId || role == "admin") {
                    confirmDeleteComment(comment.id)
                }
                true
            }
            commentsContainer.addView(card)
        }
    }

    private fun confirmDeleteComment(commentId: Long) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_comment_title)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Thread {
                    dbHelper.deleteComment(commentId)
                    runOnUiThread {
                        Toast.makeText(this, R.string.comment_deleted, Toast.LENGTH_SHORT).show()
                        loadAll()
                    }
                }.start()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Случайные рецепты из каталога, без текущего
    private fun loadRecommendations() {
        Thread {
            val items = dbHelper.getRandomRecipes(userId, 4)
                .filter { it.id != recipeId }
                .take(3)
            runOnUiThread { showRecommendations(items) }
        }.start()
    }

    private fun showRecommendations(items: List<Recipe>) {
        recommendContainer.removeAllViews()
        items.forEach { recipe ->
            val card = layoutInflater.inflate(R.layout.item_recipe, recommendContainer, false)
            card.findViewById<TextView>(R.id.recipeTitle).text = recipe.title
            card.findViewById<TextView>(R.id.recipeCategory).text = recipe.categoryName
            card.findViewById<TextView>(R.id.recipeTime).text =
                getString(R.string.minutes_format, recipe.cookTime)
            // В рекомендациях удаление не нужно
            card.findViewById<Button>(R.id.deleteButton).visibility = android.view.View.GONE

            val image = card.findViewById<ImageView>(R.id.recipeImage)
            recipe.imagePath?.let { path ->
                Thread {
                    val bitmap = BitmapFactory.decodeFile(path)
                    runOnUiThread { image.setImageBitmap(bitmap) }
                }.start()
            }

            card.setOnClickListener { openRecipe(recipe) }
            recommendContainer.addView(card)
        }
    }

    // Открываем рекомендованный рецепт на отдельном экране
    private fun openRecipe(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra(EXTRA_ID, recipe.id)
            putExtra(EXTRA_TITLE, recipe.title)
            putExtra(EXTRA_CATEGORY, recipe.categoryName)
            putExtra(EXTRA_TIME, recipe.cookTime)
            putExtra(EXTRA_DESCRIPTION, recipe.description)
            putExtra(EXTRA_IMAGE, recipe.imagePath)
        }
        startActivity(intent)
    }
}