package ru.shtrih.recipesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Модель для вывода списка рецептов на экран.
data class Recipe(
    val id: Long,
    val title: String,
    val cookTime: Int,
    val categoryName: String
)

// DBHelper отвечает за всю работу с базой SQLite.
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "recipes.db"
        // Версия 1 была только с Users, версия 2 добавила Categories и Recipes.
        const val DATABASE_VERSION = 2

        const val TABLE_USERS = "Users"
        const val COL_USER_ID = "id"
        const val COL_USER_LOGIN = "login"
        const val COL_USER_PASSWORD = "password"

        const val TABLE_CATEGORIES = "Categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"

        const val TABLE_RECIPES = "Recipes"
        const val COL_RECIPE_ID = "id"
        const val COL_RECIPE_TITLE = "title"
        const val COL_RECIPE_DESCRIPTION = "description"
        const val COL_RECIPE_COOK_TIME = "cook_time"
        const val COL_RECIPE_CATEGORY_ID = "category_id"
        const val COL_RECIPE_USER_ID = "user_id"
    }

    // Включаем проверку внешних ключей: SQLite по умолчанию её не выполняет.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createUsers(db)
        createCategories(db)
        createRecipes(db)
        insertDefaultCategories(db)
    }

    // Не удаляем Users, а только добавляем новые таблицы.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createCategories(db)
            createRecipes(db)
            insertDefaultCategories(db)
        }
    }

    private fun createUsers(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_LOGIN TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createCategories(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL UNIQUE
            )
            """.trimIndent()
        )
    }

    // FOREIGN KEY: category_id должен быть в Categories, user_id — в Users.
    private fun createRecipes(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_RECIPES (
                $COL_RECIPE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RECIPE_TITLE TEXT NOT NULL,
                $COL_RECIPE_DESCRIPTION TEXT,
                $COL_RECIPE_COOK_TIME INTEGER NOT NULL DEFAULT 0,
                $COL_RECIPE_CATEGORY_ID INTEGER NOT NULL,
                $COL_RECIPE_USER_ID INTEGER,
                FOREIGN KEY ($COL_RECIPE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID),
                FOREIGN KEY ($COL_RECIPE_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
            """.trimIndent()
        )
    }

    private fun insertDefaultCategories(db: SQLiteDatabase) {
        listOf("Завтрак", "Обед", "Ужин", "Десерт").forEach { name ->
            val values = ContentValues().apply { put(COL_CAT_NAME, name) }
            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    // Регистрация. Возвращаем id новой строки или -1, если логин занят.
    fun registerUser(login: String, password: String): Long {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, login)
            put(COL_USER_PASSWORD, password)
        }
        return writableDatabase.insert(TABLE_USERS, null, values)
    }

    // Вход. Возвращаем id пользователя или -1, если логин/пароль не подошли.
    fun getUserId(login: String, password: String): Long {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USER_LOGIN = ? AND $COL_USER_PASSWORD = ?",
            arrayOf(login, password)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    // Добавляем рецепт и привязываем его к пользователю через user_id.
    fun addRecipe(title: String, description: String, cookTime: Int, categoryId: Long, userId: Long): Long {
        val values = ContentValues().apply {
            put(COL_RECIPE_TITLE, title)
            put(COL_RECIPE_DESCRIPTION, description)
            put(COL_RECIPE_COOK_TIME, cookTime)
            put(COL_RECIPE_CATEGORY_ID, categoryId)
            put(COL_RECIPE_USER_ID, userId)
        }
        return writableDatabase.insert(TABLE_RECIPES, null, values)
    }

    // Список рецептов только текущего пользователя.
    fun getRecipes(userId: Long): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val sql = """
            SELECT r.$COL_RECIPE_ID, r.$COL_RECIPE_TITLE, r.$COL_RECIPE_COOK_TIME, c.$COL_CAT_NAME
            FROM $TABLE_RECIPES r
            JOIN $TABLE_CATEGORIES c ON c.$COL_CAT_ID = r.$COL_RECIPE_CATEGORY_ID
            WHERE r.$COL_RECIPE_USER_ID = ?
            ORDER BY r.$COL_RECIPE_ID DESC
        """.trimIndent()

        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Recipe(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        cookTime = cursor.getInt(2),
                        categoryName = cursor.getString(3)
                    )
                )
            }
        }
        return list
    }
    // Удаляем рецепт по id. Возвращаем true, если строка действительно удалилась.
    fun deleteRecipe(recipeId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_RECIPES,
            "$COL_RECIPE_ID = ?",
            arrayOf(recipeId.toString())
        ) > 0
    }
    // Категории для выпадающего списка: пары (id, название).
    fun getCategories(): List<Pair<Long, String>> {
        val list = mutableListOf<Pair<Long, String>>()
        readableDatabase.rawQuery(
            "SELECT $COL_CAT_ID, $COL_CAT_NAME FROM $TABLE_CATEGORIES ORDER BY $COL_CAT_ID",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getLong(0) to cursor.getString(1))
            }
        }
        return list
    }
}