package ru.shtrih.recipesapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

// Копируем фото из галереи в папку приложения и уменьшаем.
// Так фото не зависит от галереи и не забивает память большими картинками.
object ImageStorage {

    private const val MAX_SIDE = 800

    // Сохраняем фото и возвращаем путь к файлу, либо null, если что-то пошло не так
    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val options = BitmapFactory.Options().apply { inSampleSize = calcSample(bytes) }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

            val dir = File(context.filesDir, "recipe_images").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Считаем, во сколько раз уменьшить картинку, чтобы стороны были примерно до MAX_SIDE
    private fun calcSample(bytes: ByteArray): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE && bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }
        return sample
    }
}