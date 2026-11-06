package ru.shtrih.recipesapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Экран статистики: сколько рецептов пользователя в каждой категории
class StatsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.statsBackButton).setOnClickListener { finish() }

        // Считаем статистику в фоне, результат показываем в главном потоке
        Thread {
            val stats = dbHelper.getCategoryStats(Session.getUserId(this))
            runOnUiThread {
                findViewById<TextView>(R.id.statsText).text =
                    if (stats.all { it.second == 0 }) {
                        getString(R.string.stats_empty)
                    } else {
                        stats.joinToString("\n") { (name, count) ->
                            getString(R.string.stats_item, name, count)
                        }
                    }
            }
        }.start()
    }
}