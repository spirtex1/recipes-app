package ru.shtrih.recipesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onDelete — функция, которую адаптер вызовет при нажатии «Удалить».
// Сам адаптер не лезет в базу, он только передаёт событие наверх.
class RecipeAdapter(
    private var items: List<Recipe>,
    private val onDelete: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        holder.title.text = recipe.title
        holder.category.text = recipe.categoryName
        holder.time.text = "${recipe.cookTime} мин"
        holder.deleteButton.setOnClickListener { onDelete(recipe) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Recipe>) {
        items = newItems
        notifyDataSetChanged()
    }
}