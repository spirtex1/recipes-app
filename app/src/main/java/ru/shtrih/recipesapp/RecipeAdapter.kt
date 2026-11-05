package ru.shtrih.recipesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Адаптер берёт список рецептов и решает, что показать в каждой карточке.
// RecyclerView сам переиспользует карточки, адаптер только заполняет их данными.
class RecipeAdapter(
    private var items: List<Recipe>
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    // ViewHolder хранит ссылки на элементы одной карточки.
    // Так не нужно каждый раз заново искать TextView через findViewById.
    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.recipeTitle)
        val category: TextView = itemView.findViewById(R.id.recipeCategory)
        val time: TextView = itemView.findViewById(R.id.recipeTime)
    }

    // Создаёт новую карточку из разметки item_recipe.xml.
    // Вызывается, только когда RecyclerView нужна новая карточка.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    // Заполняет карточку данными конкретного рецепта.
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = items[position]
        holder.title.text = recipe.title
        holder.category.text = recipe.categoryName
        holder.time.text = "${recipe.cookTime} мин"
    }

    // Сколько всего карточек нужно показать.
    override fun getItemCount(): Int = items.size

    // Обновляет список и перерисовывает RecyclerView.
    // Используем после добавления или удаления рецепта.
    fun updateItems(newItems: List<Recipe>) {
        items = newItems
        notifyDataSetChanged()
    }
}