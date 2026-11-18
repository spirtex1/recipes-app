package ru.shtrih.recipesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.IOException

// Модель рецепта для списка и деталей. Рецепты общие, поэтому здесь есть автор, оценки и избранное.
data class Recipe(
    val id: Long,
    val title: String,
    val cookTime: Int,
    val categoryName: String,
    val description: String,
    val imagePath: String?,
    val createdAt: Long = 0L,
    val authorName: String = "",
    val avgRating: Double = 0.0,
    val ratingCount: Int = 0,
    val isFavorite: Boolean = false
)

// Комментарий к рецепту
data class RecipeComment(
    val id: Long,
    val authorId: Long,
    val authorName: String,
    val text: String,
    val createdAt: Long
)

// DBHelper отвечает за всю работу с базой SQLite.
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Нужен, чтобы читать фото из assets и сохранять их в файлы приложения
    private val appContext = context.applicationContext

    companion object {
        const val DATABASE_NAME = "recipes.db"
        // 1: Users. 2: Categories и Recipes. 3: роль. 4: фото. 5: дата.
        // 6: общий каталог, оценки, комментарии, избранное и стартовые данные.
        const val DATABASE_VERSION = 6

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
        const val COL_RECIPE_CREATED = "created_at"

        const val TABLE_RATINGS = "Ratings"
        const val TABLE_COMMENTS = "Comments"
        const val TABLE_FAVORITES = "Favorites"
    }

    // Включаем проверку внешних ключей: SQLite по умолчанию её не выполняет.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // Первый запуск: создаём все таблицы, категории, админа и стартовые данные.
    override fun onCreate(db: SQLiteDatabase) {
        createUsers(db)
        createCategories(db)
        createRecipes(db)
        createRatings(db)
        createComments(db)
        createFavorites(db)
        insertDefaultCategories(db)
        insertAdmin(db)
        seedDemoData(db)
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
        // Версии 2–3 ещё без колонки фото
        if (oldVersion in 2..3) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COL_RECIPE_IMAGE TEXT")
        }
        // Версии 2–4 ещё без даты. Старым рецептам ставим дату миграции.
        if (oldVersion in 2..4) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COL_RECIPE_CREATED INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "UPDATE $TABLE_RECIPES SET $COL_RECIPE_CREATED = ${System.currentTimeMillis()} " +
                        "WHERE $COL_RECIPE_CREATED = 0"
            )
        }
        // Версия 6: оценки, комментарии, избранное и стартовые рецепты
        if (oldVersion < 6) {
            createRatings(db)
            createComments(db)
            createFavorites(db)
            seedDemoData(db)
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

    // FOREIGN KEY: category_id должен быть в Categories, user_id — в Users (автор рецепта)
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
                $COL_RECIPE_CREATED INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_RECIPE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID),
                FOREIGN KEY ($COL_RECIPE_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
            """.trimIndent()
        )
    }

    // Оценки: один пользователь — одна оценка на рецепт
    private fun createRatings(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_RATINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipe_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
                UNIQUE (recipe_id, user_id),
                FOREIGN KEY (recipe_id) REFERENCES $TABLE_RECIPES($COL_RECIPE_ID) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    // Комментарии к рецептам
    private fun createComments(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_COMMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipe_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                text TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (recipe_id) REFERENCES $TABLE_RECIPES($COL_RECIPE_ID) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    // Избранное: пара (рецепт, пользователь) уникальна
    private fun createFavorites(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                recipe_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                UNIQUE (recipe_id, user_id),
                FOREIGN KEY (recipe_id) REFERENCES $TABLE_RECIPES($COL_RECIPE_ID) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES $TABLE_USERS($COL_USER_ID) ON DELETE CASCADE
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

    // Стартовые данные: демо-пользователи, 20 рецептов с фото, оценки и комментарии.
    // Все операции идут через переданный db: внутри onCreate/onUpgrade нельзя брать writableDatabase.
    private fun seedDemoData(db: SQLiteDatabase) {
        val userIds = RecipeSeed.demoUsers.associateWith { login -> findOrCreateUser(db, login, "123456") }
        val categoryIds = listOf("Завтрак", "Обед", "Ужин", "Десерт")
            .associateWith { name -> findCategoryId(db, name) }

        val recipeIds = RecipeSeed.recipes.mapIndexed { index, item ->
            val authorId = userIds[item.author] ?: return@mapIndexed -1L
            val values = ContentValues().apply {
                put(COL_RECIPE_TITLE, item.title)
                put(COL_RECIPE_DESCRIPTION, item.description)
                put(COL_RECIPE_COOK_TIME, item.cookTime)
                put(COL_RECIPE_CATEGORY_ID, categoryIds[item.category] ?: -1L)
                put(COL_RECIPE_USER_ID, authorId)
                put(COL_RECIPE_IMAGE, copyAssetPhoto(index + 1))
                // Разносим даты, чтобы фильтр по дате было что показать
                put(COL_RECIPE_CREATED, System.currentTimeMillis() - (20 - index) * 3_600_000L)
            }
            db.insert(TABLE_RECIPES, null, values)
        }

        RecipeSeed.ratings.forEach { (number, login, score) ->
            val recipeId = recipeIds.getOrNull(number - 1) ?: return@forEach
            val userId = userIds[login] ?: return@forEach
            val values = ContentValues().apply {
                put("recipe_id", recipeId)
                put("user_id", userId)
                put("score", score)
            }
            db.insertWithOnConflict(TABLE_RATINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        RecipeSeed.comments.forEach { (number, login, text) ->
            val recipeId = recipeIds.getOrNull(number - 1) ?: return@forEach
            val userId = userIds[login] ?: return@forEach
            val values = ContentValues().apply {
                put("recipe_id", recipeId)
                put("user_id", userId)
                put("text", text)
                put("created_at", System.currentTimeMillis() - number * 60_000L)
            }
            db.insert(TABLE_COMMENTS, null, values)
        }
    }

    // Возвращаем id пользователя, создавая его при необходимости
    private fun findOrCreateUser(db: SQLiteDatabase, login: String, password: String): Long {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, login)
            put(COL_USER_PASSWORD, password)
            put(COL_USER_ROLE, "user")
        }
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USER_LOGIN = ?",
            arrayOf(login)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    private fun findCategoryId(db: SQLiteDatabase, name: String): Long {
        db.rawQuery(
            "SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = ?",
            arrayOf(name)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else -1L
        }
    }

    // Копируем фото из assets/recipes в файлы приложения. Если фото нет, возвращаем null.
    private fun copyAssetPhoto(number: Int): String? {
        val dir = File(appContext.filesDir, "recipe_images").apply { mkdirs() }
        val target = File(dir, "seed_$number.jpg")
        return try {
            if (!target.exists()) {
                appContext.assets.open("recipes/recipe_$number.jpg").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            target.absolutePath
        } catch (e: IOException) {
            null
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

    // Роль пользователя по id: "admin" или "user"
    fun getUserRole(userId: Long): String {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_ROLE FROM $TABLE_USERS WHERE $COL_USER_ID = ?",
            arrayOf(userId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else "user"
        }
    }

    // Логин пользователя по id
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

    // Добавляем рецепт, автор — текущий пользователь
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
            put(COL_RECIPE_CREATED, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_RECIPES, null, values)
    }

    // Все рецепты каталога. userId нужен, чтобы отметить избранные.
    fun getRecipes(userId: Long): List<Recipe> {
        return queryRecipes(userId, "", emptyArray())
    }

    // Рецепты одной категории
    fun getRecipesByCategory(userId: Long, categoryId: Long): List<Recipe> {
        return queryRecipes(
            userId,
            "WHERE r.$COL_RECIPE_CATEGORY_ID = ?",
            arrayOf(categoryId.toString())
        )
    }

    // Поиск по названию, без учёта регистра
    fun searchRecipes(userId: Long, query: String): List<Recipe> {
        return queryRecipes(
            userId,
            "WHERE LOWER(r.$COL_RECIPE_TITLE) LIKE ?",
            arrayOf("%${query.lowercase()}%")
        )
    }

    // Рецепты из избранного пользователя
    fun getFavoriteRecipes(userId: Long): List<Recipe> {
        return queryRecipes(
            userId,
            "WHERE EXISTS (SELECT 1 FROM $TABLE_FAVORITES f WHERE f.recipe_id = r.$COL_RECIPE_ID AND f.user_id = ?)",
            arrayOf(userId.toString())
        )
    }

    // Случайные рецепты для блока «Вам может понравиться»
    fun getRandomRecipes(userId: Long, limit: Int): List<Recipe> {
        return queryRecipes(userId, "", emptyArray(), "ORDER BY RANDOM() LIMIT $limit")
    }

    // Один рецепт по id, с оценкой и флагом избранного
    fun getRecipeById(userId: Long, recipeId: Long): Recipe? {
        return queryRecipes(
            userId,
            "WHERE r.$COL_RECIPE_ID = ?",
            arrayOf(recipeId.toString())
        ).firstOrNull()
    }

    // Общая выборка рецептов: один SELECT, одна точка маппинга в Recipe.
    // Первый параметр в SQL — userId для флага избранного, дальше параметры условия.
    private fun queryRecipes(
        userId: Long,
        whereClause: String,
        args: Array<String>,
        orderClause: String = "ORDER BY r.$COL_RECIPE_ID DESC"
    ): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val sql = """
            SELECT r.$COL_RECIPE_ID, r.$COL_RECIPE_TITLE, r.$COL_RECIPE_COOK_TIME,
                   c.$COL_CAT_NAME, r.$COL_RECIPE_DESCRIPTION, r.$COL_RECIPE_IMAGE,
                   r.$COL_RECIPE_CREATED, COALESCE(u.$COL_USER_LOGIN, ''),
                   (SELECT AVG(score) FROM $TABLE_RATINGS WHERE recipe_id = r.$COL_RECIPE_ID),
                   (SELECT COUNT(*) FROM $TABLE_RATINGS WHERE recipe_id = r.$COL_RECIPE_ID),
                   EXISTS (SELECT 1 FROM $TABLE_FAVORITES WHERE recipe_id = r.$COL_RECIPE_ID AND user_id = ?)
            FROM $TABLE_RECIPES r
            JOIN $TABLE_CATEGORIES c ON c.$COL_CAT_ID = r.$COL_RECIPE_CATEGORY_ID
            LEFT JOIN $TABLE_USERS u ON u.$COL_USER_ID = r.$COL_RECIPE_USER_ID
            $whereClause
            $orderClause
        """.trimIndent()

        val allArgs = arrayOf(userId.toString(), *args)
        readableDatabase.rawQuery(sql, allArgs).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Recipe(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        cookTime = cursor.getInt(2),
                        categoryName = cursor.getString(3),
                        description = cursor.getString(4) ?: "",
                        imagePath = cursor.getString(5),
                        createdAt = cursor.getLong(6),
                        authorName = cursor.getString(7),
                        avgRating = cursor.getDouble(8),
                        ratingCount = cursor.getInt(9),
                        isFavorite = cursor.getInt(10) == 1
                    )
                )
            }
        }
        return list
    }

    // Удаляем рецепт по id. Оценки, комментарии и избранное удалятся каскадно.
    fun deleteRecipe(recipeId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_RECIPES,
            "$COL_RECIPE_ID = ?",
            arrayOf(recipeId.toString())
        ) > 0
    }

    // Обновляем существующий рецепт, включая фото. Автор и дата не меняются.
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

    // Ставим или меняем оценку (1–5). Одна оценка на пользователя и рецепт.
    fun setRating(recipeId: Long, userId: Long, score: Int): Boolean {
        require(score in 1..5) { "Оценка должна быть от 1 до 5" }
        val values = ContentValues().apply {
            put("recipe_id", recipeId)
            put("user_id", userId)
            put("score", score)
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_RATINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    // Оценка текущего пользователя, 0 если не ставил
    fun getUserRating(recipeId: Long, userId: Long): Int {
        readableDatabase.rawQuery(
            "SELECT score FROM $TABLE_RATINGS WHERE recipe_id = ? AND user_id = ?",
            arrayOf(recipeId.toString(), userId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    // Добавляем комментарий
    fun addComment(recipeId: Long, userId: Long, text: String): Long {
        val values = ContentValues().apply {
            put("recipe_id", recipeId)
            put("user_id", userId)
            put("text", text)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_COMMENTS, null, values)
    }

    // Комментарии к рецепту, от старых к новым
    fun getComments(recipeId: Long): List<RecipeComment> {
        val list = mutableListOf<RecipeComment>()
        readableDatabase.rawQuery(
            """
            SELECT c.id, c.user_id, u.$COL_USER_LOGIN, c.text, c.created_at
            FROM $TABLE_COMMENTS c
            JOIN $TABLE_USERS u ON u.$COL_USER_ID = c.user_id
            WHERE c.recipe_id = ?
            ORDER BY c.created_at ASC
            """.trimIndent(),
            arrayOf(recipeId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    RecipeComment(
                        id = cursor.getLong(0),
                        authorId = cursor.getLong(1),
                        authorName = cursor.getString(2),
                        text = cursor.getString(3),
                        createdAt = cursor.getLong(4)
                    )
                )
            }
        }
        return list
    }

    // Удаляем комментарий по id. Проверку прав делаем в UI.
    fun deleteComment(commentId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_COMMENTS,
            "id = ?",
            arrayOf(commentId.toString())
        ) > 0
    }

    // Добавляет рецепт в избранное или убирает. Возвращаем новое состояние: true — в избранном.
    fun toggleFavorite(recipeId: Long, userId: Long): Boolean {
        val db = writableDatabase
        val removed = db.delete(
            TABLE_FAVORITES,
            "recipe_id = ? AND user_id = ?",
            arrayOf(recipeId.toString(), userId.toString())
        )
        if (removed > 0) return false
        val values = ContentValues().apply {
            put("recipe_id", recipeId)
            put("user_id", userId)
        }
        return db.insert(TABLE_FAVORITES, null, values) != -1L
    }

    // Количество рецептов пользователя по каждой категории (только его рецепты, как автора)
    // Количество рецептов по каждой категории по всему общему каталогу
    fun getCategoryStats(@Suppress("UNUSED_PARAMETER") userId: Long): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val sql = """
            SELECT c.$COL_CAT_NAME, COUNT(r.$COL_RECIPE_ID)
            FROM $TABLE_CATEGORIES c
            LEFT JOIN $TABLE_RECIPES r ON r.$COL_RECIPE_CATEGORY_ID = c.$COL_CAT_ID
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NAME
            ORDER BY c.$COL_CAT_ID
        """.trimIndent()

        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0) to cursor.getInt(1))
            }
        }
        return list
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

    // Все пользователи с количеством их рецептов, для админки
    fun getAdminUsers(): List<AdminUser> {
        val list = mutableListOf<AdminUser>()
        val sql = """
        SELECT u.$COL_USER_ID, u.$COL_USER_LOGIN, u.$COL_USER_ROLE, COUNT(r.$COL_RECIPE_ID)
        FROM $TABLE_USERS u
        LEFT JOIN $TABLE_RECIPES r ON r.$COL_RECIPE_USER_ID = u.$COL_USER_ID
        GROUP BY u.$COL_USER_ID, u.$COL_USER_LOGIN, u.$COL_USER_ROLE
        ORDER BY u.$COL_USER_ID
    """.trimIndent()
        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    AdminUser(
                        id = cursor.getLong(0),
                        login = cursor.getString(1),
                        role = cursor.getString(2),
                        recipesCount = cursor.getInt(3)
                    )
                )
            }
        }
        return list
    }

    // Меняем роль пользователя: "admin" или "user"
    fun setUserRole(userId: Long, role: String): Boolean {
        val values = ContentValues().apply { put(COL_USER_ROLE, role) }
        return writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_USER_ID = ?",
            arrayOf(userId.toString())
        ) > 0
    }
    // Меняем пароль, только если старый совпадает. Возвращаем true при успехе.
    fun changePassword(userId: Long, oldPassword: String, newPassword: String): Boolean {
        val values = ContentValues().apply { put(COL_USER_PASSWORD, newPassword) }
        return writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_USER_ID = ? AND $COL_USER_PASSWORD = ?",
            arrayOf(userId.toString(), oldPassword)
        ) > 0
    }

    // Удаляем пользователя вместе с его рецептами в одной транзакции.
    fun deleteUser(userId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_RECIPES, "$COL_RECIPE_USER_ID = ?", arrayOf(userId.toString()))
            val rows = db.delete(TABLE_USERS, "$COL_USER_ID = ?", arrayOf(userId.toString()))
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }
}