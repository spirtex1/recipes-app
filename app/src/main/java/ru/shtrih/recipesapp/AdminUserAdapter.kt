package ru.shtrih.recipesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Пользователь для админки: данные из базы одной строкой
data class AdminUser(
    val id: Long,
    val login: String,
    val role: String,
    val recipesCount: Int
)

// Адаптер списка пользователей. Своё действие сам не выполняет, передаёт наверх.
class AdminUserAdapter(
    private val currentUserId: Long,
    private val onToggleRole: (AdminUser) -> Unit,
    private val onDelete: (AdminUser) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    private var items: List<AdminUser> = emptyList()

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.userAvatar)
        val login: TextView = view.findViewById(R.id.userLogin)
        val info: TextView = view.findViewById(R.id.userInfo)
        val roleButton: Button = view.findViewById(R.id.userRoleButton)
        val deleteButton: Button = view.findViewById(R.id.userDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = items[position]
        val context = holder.itemView.context
        val isMe = user.id == currentUserId
        val isAdmin = user.role == "admin"

        holder.avatar.text = user.login.take(1).uppercase()
        holder.login.text = if (isMe) "${user.login} (вы)" else user.login
        holder.info.text = context.getString(
            R.string.admin_user_info,
            context.getString(if (isAdmin) R.string.profile_role_admin else R.string.profile_role_user),
            user.recipesCount
        )

        holder.roleButton.text = context.getString(
            if (isAdmin) R.string.admin_remove_admin else R.string.admin_make_admin
        )
        // Собственный аккаунт нельзя ни понизить, ни удалить
        holder.roleButton.isEnabled = !isMe
        holder.deleteButton.isEnabled = !isMe

        holder.roleButton.setOnClickListener { onToggleRole(user) }
        holder.deleteButton.setOnClickListener { onDelete(user) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<AdminUser>) {
        items = newItems
        notifyDataSetChanged()
    }
}