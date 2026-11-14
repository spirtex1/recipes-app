package ru.shtrih.recipesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Модель рецепта для списка и деталей. imagePath — путь к фото или null.
data class Recipe(
    val id: Long,
    val title: String,
    val cookTime: Int,
    val categoryName: String,
    val description: String,
    val imagePath: String?
)

// DBHelper отвечает за всю работу с базой SQLite.
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "recipes.db"
        // 1: Users. 2: Categories и Recipes. 3: роль. 4: фото блюд (image_path).
        const val DATABASE_VERSION = 4

        const val ADMIN_LOGIN = "admin"
        const val ADMIN_PASSWORD = "admin123"

        const val TABLE_USERS = "Users"
        const val COL_USER_ID = "id"
        const val COL_USER_LOGIN = "login"
        const val COL_USER_PASSWORD = "password"
        const val COL_USER_ROLE = "role"

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
        const val COL_RECIPE_IMAGE = "image_path"
    }

    // Включаем проверку внешних ключей: SQLite по умолчанию её не выполняет.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // Первый запуск: создаём все таблицы, категории и админа.
    override fun onCreate(db: SQLiteDatabase) {
        createUsers(db)
        createCategories(db)
        createRecipes(db)
        insertDefaultCategories(db)
        insertAdmin(db)
    }

    // Обновление базы. Данные не удаляем, только добавляем новое.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createCategories(db)
            createRecipes(db)
            insertDefaultCategories(db)
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_USER_ROLE TEXT NOT NULL DEFAULT 'user'")
            insertAdmin(db)
        }
        // Таблица Recipes версии 2 и 3 ещё без колонки фото. Для версии 1 она уже создана с колонкой.
        if (oldVersion in 2..3) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COL_RECIPE_IMAGE TEXT")
        }
    }

    private fun createUsers(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_LOGIN TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL,
                $COL_USER_ROLE TEXT NOT NULL DEFAULT 'user'
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
                $COL_RECIPE_IMAGE TEXT,
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

    // Стартовый админ. Если уже есть, строка пропускается.
    private fun insertAdmin(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, ADMIN_LOGIN)
            put(COL_USER_PASSWORD, ADMIN_PASSWORD)
            put(COL_USER_ROLE, "admin")
        }
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // Регистрация. Возвращаем id новой строки или -1, если логин занят.
    // Новому пользователю сразу добавляем пару примеров, чтобы список не был пустым.
    fun registerUser(login: String, password: String): Long {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, login)
            put(COL_USER_PASSWORD, password)
        }
        val id = writableDatabase.insert(TABLE_USERS, null, values)
        if (id != -1L) insertSampleRecipes(id)
        return id
    }

    // Два примера рецептов для нового пользователя
    private fun insertSampleRecipes(userId: Long) {
        addRecipe(
            title = "Омлет с сыром",
            description = "Яйца 2 шт, молоко 50 мл, сыр 30 г, соль.\nВзбить яйца с молоком, вылить на сковороду, посыпать тёртым сыром.",
            cookTime = 10,
            categoryId = getCategoryId("Завтрак"),
            userId = userId
        )
        addRecipe(
            title = "Борщ",
            description = "Свёкла, капуста, картофель, морковь, лук, томатная паста, мясной бульон.\nВарить до готовности, в конце добавить зелень и сметану.",
            cookTime = 90,
            categoryId = getCategoryId("Обед"),
            userId = userId
        )
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

    // Роль пользователя по id: "admin" или "user"
    fun getUserRole(userId: Long): String {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_ROLE FROM $TABLE_USERS WHERE $COL_USER_ID = ?",
            arrayOf(userId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else "user"
        }
    }

    // Логин пользователя по id, нужен для приветствия в профиле
    fun getUserLogin(userId: Long): String {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_LOGIN FROM $TABLE_USERS WHERE $COL_USER_ID = ?",
            arrayOf(userId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else ""
        }
    }

    // Все пользователи для админки: пары (логин, роль)
    fun getAllUsers(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery(
            "SELECT $COL_USER_LOGIN, $COL_USER_ROLE FROM $TABLE_USERS ORDER BY $COL_USER_ID",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0) to cursor.getString(1))
            }
        }
        return list
    }

    fun getUsersCount(): Int = countRows(TABLE_USERS)

    fun getRecipesCount(): Int = countRows(TABLE_RECIPES)

    // Считаем строки одним запросом COUNT(*)
    private fun countRows(table: String): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    // id категории по названию. Возвращаем -1, если такой нет.
    fun getCategoryId(name: String): Long {
        readableDatabase.rawQuery(
            "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = ?",
            arrayOf(name)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    // Добавляем рецепт, привязываем к пользователю и сохраняем путь к фото
    fun addRecipe(
        title: String,
        description: String,
        cookTime: Int,
        categoryId: Long,
        userId: Long,
        imagePath: String? = null
    ): Long {
        val values = ContentValues().apply {
            put(COL_RECIPE_TITLE, title)
            put(COL_RECIPE_DESCRIPTION, description)
            put(COL_RECIPE_COOK_TIME, cookTime)
            put(COL_RECIPE_CATEGORY_ID, categoryId)
            put(COL_RECIPE_USER_ID, userId)
            put(COL_RECIPE_IMAGE, imagePath)
        }
        return writableDatabase.insert(TABLE_RECIPES, null, values)
    }

    // Список рецептов только текущего пользователя
    fun getRecipes(userId: Long): List<Recipe> {
        return queryRecipes(
            "WHERE r.$COL_RECIPE_USER_ID = ?",
            arrayOf(userId.toString())
        )
    }

    // Рецепты пользователя одной категории
    fun getRecipesByCategory(userId: Long, categoryId: Long): List<Recipe> {
        return queryRecipes(
            "WHERE r.$COL_RECIPE_USER_ID = ? AND r.$COL_RECIPE_CATEGORY_ID = ?",
            arrayOf(userId.toString(), categoryId.toString())
        )
    }

    // Поиск по названию, без учёта регистра
    fun searchRecipes(userId: Long, query: String): List<Recipe> {
        return queryRecipes(
            "WHERE r.$COL_RECIPE_USER_ID = ? AND LOWER(r.$COL_RECIPE_TITLE) LIKE ?",
            arrayOf(userId.toString(), "%${query.lowercase()}%")
        )
    }

    // Общая выборка рецептов: один SELECT и один маппинг в Recipe для всех запросов
    private fun queryRecipes(whereClause: String, args: Array<String>): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val sql = """
            SELECT r.$COL_RECIPE_ID, r.$COL_RECIPE_TITLE, r.$COL_RECIPE_COOK_TIME,
                   c.$COL_CAT_NAME, r.$COL_RECIPE_DESCRIPTION, r.$COL_RECIPE_IMAGE
            FROM $TABLE_RECIPES r
            JOIN $TABLE_CATEGORIES c ON c.$COL_CAT_ID = r.$COL_RECIPE_CATEGORY_ID
            $whereClause
            ORDER BY r.$COL_RECIPE_ID DESC
        """.trimIndent()

        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Recipe(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        cookTime = cursor.getInt(2),
                        categoryName = cursor.getString(3),
                        description = cursor.getString(4) ?: "",
                        imagePath = cursor.getString(5)
                    )
                )
            }
        }
        return list
    }

    // Удаляем рецепт по id
    fun deleteRecipe(recipeId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_RECIPES,
            "$COL_RECIPE_ID = ?",
            arrayOf(recipeId.toString())
        ) > 0
    }

    // Обновляем существующий рецепт, включая фото
    fun updateRecipe(
        recipeId: Long,
        title: String,
        description: String,
        cookTime: Int,
        categoryId: Long,
        imagePath: String?
    ): Boolean {
        val values = ContentValues().apply {
            put(COL_RECIPE_TITLE, title)
            put(COL_RECIPE_DESCRIPTION, description)
            put(COL_RECIPE_COOK_TIME, cookTime)
            put(COL_RECIPE_CATEGORY_ID, categoryId)
            put(COL_RECIPE_IMAGE, imagePath)
        }
        return writableDatabase.update(
            TABLE_RECIPES,
            values,
            "$COL_RECIPE_ID = ?",
            arrayOf(recipeId.toString())
        ) > 0
    }

    // Категории для выпадающих списков: пары (id, название)
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

    // Количество рецептов пользователя по каждой категории
    fun getCategoryStats(userId: Long): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val sql = """
            SELECT c.$COL_CAT_NAME, COUNT(r.$COL_RECIPE_ID)
            FROM $TABLE_CATEGORIES c
            LEFT JOIN $TABLE_RECIPES r
                ON r.$COL_RECIPE_CATEGORY_ID = c.$COL_CAT_ID
                AND r.$COL_RECIPE_USER_ID = ?
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NAME
            ORDER BY c.$COL_CAT_ID
        """.trimIndent()

        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0) to cursor.getInt(1))
            }
        }
        return list
    }
}