package ru.shtrih.recipesapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator

// Экран статистики: сводка и карточка на каждую категорию с полоской доли
class StatsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        dbHelper = DBHelper(this)

        findViewById<Button>(R.id.statsBackButton).setOnClickListener { finish() }

        // Считаем статистику в фоне, карточки строим в главном потоке
        Thread {
            val stats = dbHelper.getCategoryStats(Session.getUserId(this))
            runOnUiThread { showStats(stats) }
        }.start()
    }

    private fun showStats(stats: List<Pair<String, Int>>) {
        val total = stats.sumOf { it.second }
        val container = findViewById<LinearLayout>(R.id.statsContainer)
        val summary = findViewById<TextView>(R.id.statsSummary)
        val empty = findViewById<TextView>(R.id.emptyStats)

        container.removeAllViews()

        // Пустая статистика: показываем заглушку и сводку с нулём
        if (total == 0) {
            summary.text = getString(R.string.stats_total_label, 0)
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE

        // Самая популярная категория для сводки
        val top = stats.maxByOrNull { it.second }
        summary.text = getString(R.string.stats_total_label, total) + "\n" +
                getString(R.string.stats_top_label, top?.first.orEmpty(), top?.second ?: 0)

        // Карточка на каждую категорию. Полоска показывает долю от общего количества.
        stats.forEach { (name, count) ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_stat, container, false)
            item.findViewById<TextView>(R.id.statName).text = name
            item.findViewById<TextView>(R.id.statCount).text = count.toString()
            item.findViewById<LinearProgressIndicator>(R.id.statBar).progress = count * 100 / total
            container.addView(item)
        }
    }
}