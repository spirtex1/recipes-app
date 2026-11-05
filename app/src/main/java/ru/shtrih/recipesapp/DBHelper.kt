package ru.shtrih.recipesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Модель для вывода списка рецептов на экран.
// Это не таблица, а просто удобная структура с данными из JOIN-запроса.
data class Recipe(
    val id: Long,
    val title: String,
    val cookTime: Int,
    val categoryName: String
)

// DBHelper отвечает за всю работу с базой SQLite.
// Наследуемся от SQLiteOpenHelper: он создаёт базу при первом запуске
// и вызывает onUpgrade при изменении версии.
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Имя файла базы на телефоне
        const val DATABASE_NAME = "recipes.db"
        // Версия схемы. Увеличиваем, когда меняем структуру таблиц.
        // Версия 1 была только с Users, версия 2 добавила Categories и Recipes.
        const val DATABASE_VERSION = 2

        // Таблица пользователей
        const val TABLE_USERS = "Users"
        const val COL_USER_ID = "id"
        const val COL_USER_LOGIN = "login"
        const val COL_USER_PASSWORD = "password"

        // Таблица категорий (Завтрак, Обед и т.д.)
        const val TABLE_CATEGORIES = "Categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"

        // Таблица рецептов. Связана с Categories и Users через внешние ключи.
        const val TABLE_RECIPES = "Recipes"
        const val COL_RECIPE_ID = "id"
        const val COL_RECIPE_TITLE = "title"
        const val COL_RECIPE_DESCRIPTION = "description"
        const val COL_RECIPE_COOK_TIME = "cook_time"
        const val COL_RECIPE_CATEGORY_ID = "category_id"
        const val COL_RECIPE_USER_ID = "user_id"
    }

    // Вызывается до создания или обновления базы.
    // Включаем проверку внешних ключей: SQLite по умолчанию её не выполняет.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // Вызывается, если базы ещё нет (первый запуск).
    // Создаём все таблицы и заполняем категории.
    override fun onCreate(db: SQLiteDatabase) {
        createUsers(db)
        createCategories(db)
        createRecipes(db)
        insertDefaultCategories(db)
    }

    // Вызывается, если версия базы на телефоне меньше DATABASE_VERSION.
    // Здесь мы не удаляем Users, а только добавляем новые таблицы.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createCategories(db)
            createRecipes(db)
            insertDefaultCategories(db)
        }
    }

    // CREATE TABLE IF NOT EXISTS: создаёт таблицу, только если её ещё нет.
    // Это защищает от ошибки при повторном вызове.
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

    // Категории: id и уникальное название.
    // UNIQUE не даст добавить одну и ту же категорию дважды.
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

    // Рецепты. FOREIGN KEY означает, что category_id должен существовать в Categories,
    // а user_id — в Users. Это и есть целостность связей из методички.
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

    // Добавляем стартовые категории.
    // CONFLICT_IGNORE: если категория уже есть, запись пропускается без ошибки.
    private fun insertDefaultCategories(db: SQLiteDatabase) {
        listOf("Завтрак", "Обед", "Ужин", "Десерт").forEach { name ->
            val values = ContentValues().apply { put(COL_CAT_NAME, name) }
            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    // Регистрация: добавляем строку в Users.
    // Возвращает false, если логин уже занят (сработал UNIQUE).
    fun registerUser(login: String, password: String): Boolean {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, login)
            put(COL_USER_PASSWORD, password)
        }
        return writableDatabase.insert(TABLE_USERS, null, values) != -1L
    }

    // Проверка входа: ищем строку с таким логином и паролем.
    // Знаки ? защищают от SQL-инъекций: значения передаются отдельно.
    fun checkUser(login: String, password: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USER_LOGIN = ? AND $COL_USER_PASSWORD = ?",
            arrayOf(login, password)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    // Добавляем рецепт. user_id пока не заполняем, его привяжем позже, при сохранении сессии.
    fun addRecipe(title: String, description: String, cookTime: Int, categoryId: Long): Long {
        val values = ContentValues().apply {
            put(COL_RECIPE_TITLE, title)
            put(COL_RECIPE_DESCRIPTION, description)
            put(COL_RECIPE_COOK_TIME, cookTime)
            put(COL_RECIPE_CATEGORY_ID, categoryId)
        }
        return writableDatabase.insert(TABLE_RECIPES, null, values)
    }

    // Список рецептов с названием категории.
    // JOIN соединяет Recipes и Categories по category_id, поэтому в списке видно не id, а название.
    fun getRecipes(): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val sql = """
            SELECT r.$COL_RECIPE_ID, r.$COL_RECIPE_TITLE, r.$COL_RECIPE_COOK_TIME, c.$COL_CAT_NAME
            FROM $TABLE_RECIPES r
            JOIN $TABLE_CATEGORIES c ON c.$COL_CAT_ID = r.$COL_RECIPE_CATEGORY_ID
            ORDER BY r.$COL_RECIPE_ID DESC
        """.trimIndent()

        readableDatabase.rawQuery(sql, null).use { cursor ->
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

    // Категории для выпадающего списка при добавлении рецепта.
    // Возвращаем пары (id, название), чтобы потом передать id в addRecipe.
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