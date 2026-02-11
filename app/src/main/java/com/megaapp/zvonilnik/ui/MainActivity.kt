package com.megaapp.zvonilnik.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.megaapp.zvonilnik.R

class MainActivity : AppCompatActivity() {

    private lateinit var permBlock: LinearLayout
    private lateinit var btnSetup: Button
    private lateinit var btnOpenList: Button
    private lateinit var btnCreate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permBlock = findViewById(R.id.permBlock)
        btnSetup = findViewById(R.id.btnSetup)
        btnOpenList = findViewById(R.id.btnOpenList)
        btnCreate = findViewById(R.id.btnCreate)

        btnSetup.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        btnOpenList.setOnClickListener {
            startActivity(Intent(this, ZvonilnikListActivity::class.java))
        }

        btnCreate.setOnClickListener {
            startActivity(Intent(this, NewZvonilnikActivity::class.java))
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val ready = SetupChecker.isReady(this)

        permBlock.visibility = if (ready) View.GONE else View.VISIBLE

        // Пока не выдали разрешения — блокируем создание/список (иначе “не верю”, будет путаница)
        btnOpenList.isEnabled = ready
        btnCreate.isEnabled = ready
    }
}
