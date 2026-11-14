package ru.shtrih.recipesapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Адаптер заполняет карточки. Удаление и открытие он только передаёт наверх через колбэки.
class RecipeAdapter(
    private var items: List<Recipe>,
    private val onDelete: (Recipe) -> Unit,
    private val onOpen: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.recipeImage)
        val title: TextView = itemView.findViewById(R.id.recipeTitle)
        val category: TextView = itemView.findViewById(R.id.recipeCategory)
        val time: TextView = itemView.findViewById(R.id.recipeTime)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = items[position]
        holder.image.setImageBitmap(loadThumb(recipe.imagePath))
        holder.title.text = recipe.title
        holder.category.text = recipe.categoryName
        holder.time.text = "${recipe.cookTime} мин"
        holder.deleteButton.setOnClickListener { onDelete(recipe) }
        // Тап по карточке открывает детали. Кнопка «Удалить» обрабатывает свой клик сама.
        holder.itemView.setOnClickListener { onOpen(recipe) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Recipe>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Миниатюра для списка. inSampleSize = 2 уменьшает картинку, чтобы список не тормозил.
    private fun loadThumb(path: String?): Bitmap? {
        if (path == null) return null
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        return BitmapFactory.decodeFile(path, options)
    }
}