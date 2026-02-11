package com.megaapp.zvonilnik.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.alarm.ZvonilnikScheduler
import com.megaapp.zvonilnik.data.DbProvider
import com.megaapp.zvonilnik.data.ZvonilnikEntity
import java.util.concurrent.Executors

class ZvonilnikListActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ZvonilnikAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zvonilnik_list)

        rv = findViewById(R.id.rv)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ZvonilnikAdapter(
            onToggle = { item, enabled -> onToggle(item, enabled) },
            onDelete = { item -> onDelete(item) }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnCreateFromList).setOnClickListener {
            startActivity(Intent(this, NewZvonilnikActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        io.execute {
            runCatching { DbProvider.init(applicationContext) }
            val items = runCatching { DbProvider.dao().getAllSorted() }.getOrElse {
                Log.e("Zvonilnik", "getAllSorted failed", it)
                emptyList()
            }
            runOnUiThread {
                adapter.submit(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onToggle(item: ZvonilnikEntity, enabled: Boolean) {
        io.execute {
            try {
                val dao = DbProvider.dao()
                val updated = if (enabled) {
                    val next = com.megaapp.zvonilnik.alarm.ZvonilnikScheduler.computeNextTriggerMillis(
                        item.hour, item.minute, item.repeatMask
                    )
                    item.copy(enabled = true, nextTriggerAtMillis = next)
                } else {
                    item.copy(enabled = false)
                }

                dao.update(updated)

                if (enabled) {
                    ZvonilnikScheduler.scheduleExact(this, updated.id, updated.nextTriggerAtMillis)
                } else {
                    ZvonilnikScheduler.cancel(this, updated.id)
                }

                runOnUiThread { reload() }
            } catch (t: Throwable) {
                Log.e("Zvonilnik", "toggle failed", t)
                runOnUiThread {
                    Toast.makeText(this, "Ошибка при переключении (см. logcat)", Toast.LENGTH_LONG).show()
                    reload()
                }
            }
        }
    }

    private fun onDelete(item: ZvonilnikEntity) {
        io.execute {
            try {
                val dao = DbProvider.dao()

                // 1) снять аларм
                ZvonilnikScheduler.cancel(this, item.id)

                // 2) удалить из базы (через @Delete — самый надежный вариант)
                dao.delete(item)

                runOnUiThread {
                    Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show()
                    reload()
                }
            } catch (t: Throwable) {
                Log.e("Zvonilnik", "delete failed", t)
                runOnUiThread {
                    Toast.makeText(this, "Ошибка удаления (см. logcat)", Toast.LENGTH_LONG).show()
                    reload()
                }
            }
        }
    }
}
