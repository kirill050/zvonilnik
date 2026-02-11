package com.megaapp.zvonilnik.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.alarm.ZvonilnikScheduler
import com.megaapp.zvonilnik.data.DbProvider
import com.megaapp.zvonilnik.data.ZvonilnikEntity
import java.time.LocalTime
import java.util.concurrent.Executors

class NewZvonilnikActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    private lateinit var etName: EditText
    private lateinit var etNumber: EditText
    private lateinit var tvTime: TextView

    private var hour: Int = LocalTime.now().hour
    private var minute: Int = (LocalTime.now().minute + 1) % 60

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val uri = res.data?.data ?: return@registerForActivityResult
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(0) ?: ""
                    val number = it.getString(1) ?: ""
                    etName.setText(name)
                    etNumber.setText(number)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_zvonilnik)

        etName = findViewById(R.id.etName)
        etNumber = findViewById(R.id.etNumber)
        tvTime = findViewById(R.id.tvTime)

        updateTimeLabel()

        findViewById<Button>(R.id.btnPickContact).setOnClickListener {
            // сразу Phone.CONTENT_URI чтобы сразу выбрать номер
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnPickTime).setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, m ->
                    hour = h
                    minute = m
                    updateTimeLabel()
                },
                hour, minute, true
            ).show()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            save()
        }
    }

    private fun updateTimeLabel() {
        tvTime.text = "%02d:%02d".format(hour, minute)
    }

    private fun save() {
        val rawName = etName.text?.toString()?.trim().orEmpty()
        val rawNumber = etNumber.text?.toString()?.trim().orEmpty()

        if (rawNumber.isBlank()) {
            Toast.makeText(this, "Нужен номер телефона (можно любой, это локальная имитация)", Toast.LENGTH_LONG).show()
            return
        }

        io.execute {
            val dao = DbProvider.dao()

            // Жёсткое правило: не допускаем два звонильника в одно и то же время (час+минута)
            val conflicts = dao.countEnabledAtTime(hour, minute)
            if (conflicts > 0) {
                runOnUiThread {
                    Toast.makeText(this, "На %02d:%02d уже есть звонильник".format(hour, minute), Toast.LENGTH_LONG).show()
                }
                return@execute
            }

            val next = ZvonilnikScheduler.computeNextTriggerMillis(hour, minute, repeatMask = 0)

            val entity = ZvonilnikEntity(
                enabled = true,
                repeatMask = 0,
                hour = hour,
                minute = minute,
                contactName = rawName.ifBlank { null },
                phoneNumber = rawNumber,
                nextTriggerAtMillis = next
            )

            val id = dao.insert(entity)
            ZvonilnikScheduler.scheduleExact(this, id, next)

            runOnUiThread {
                Toast.makeText(this, "Создано: %02d:%02d".format(hour, minute), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
