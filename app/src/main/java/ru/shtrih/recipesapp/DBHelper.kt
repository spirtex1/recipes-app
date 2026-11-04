package ru.shtrih.recipesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "recipes.db"
        const val DATABASE_VERSION = 1

        const val TABLE_USERS = "Users"
        const val COL_USER_ID = "id"
        const val COL_USER_LOGIN = "login"
        const val COL_USER_PASSWORD = "password"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_LOGIN TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(login: String, password: String): Boolean {
        val values = ContentValues().apply {
            put(COL_USER_LOGIN, login)
            put(COL_USER_PASSWORD, password)
        }
        return writableDatabase.insert(TABLE_USERS, null, values) != -1L
    }

    fun checkUser(login: String, password: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT $COL_USER_ID FROM $TABLE_USERS WHERE $COL_USER_LOGIN = ? AND $COL_USER_PASSWORD = ?",
            arrayOf(login, password)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }
}