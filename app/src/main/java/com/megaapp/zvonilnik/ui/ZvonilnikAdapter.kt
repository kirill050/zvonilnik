package com.megaapp.zvonilnik.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.data.ZvonilnikEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ZvonilnikAdapter(
    private val onToggle: (ZvonilnikEntity, Boolean) -> Unit,
    private val onDelete: (ZvonilnikEntity) -> Unit
) : RecyclerView.Adapter<ZvonilnikAdapter.H>() {

    private val items = mutableListOf<ZvonilnikEntity>()

    fun submit(newItems: List<ZvonilnikEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): H {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_zvonilnik, parent, false)
        return H(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: H, position: Int) {
        holder.bind(items[position])
    }

    inner class H(v: View) : RecyclerView.ViewHolder(v) {
        private val tvTime: TextView = v.findViewById(R.id.tvTime)
        private val tvWho: TextView = v.findViewById(R.id.tvWho)
        private val tvNext: TextView = v.findViewById(R.id.tvNext)
        private val sw: SwitchCompat = v.findViewById(R.id.swEnabled)
        private val btnDelete: Button = v.findViewById(R.id.btnDelete)

        fun bind(item: ZvonilnikEntity) {
            tvTime.text = "%02d:%02d".format(item.hour, item.minute)

            val who = if (!item.contactName.isNullOrBlank()) "#0${item.contactName}" else ":${item.phoneNumber}"
            tvWho.text = who

            tvNext.text = "Следующий: ${formatNext(item.nextTriggerAtMillis)}"

            sw.setOnCheckedChangeListener(null)
            sw.isChecked = item.enabled
            sw.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }

            btnDelete.isEnabled = true
            btnDelete.setOnClickListener {
                // блокируем повторные нажатия, пока идет удаление
                btnDelete.isEnabled = false
                onDelete(item)
            }
        }
    }

    private fun formatNext(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        val date = zdt.toLocalDate()
        val time = zdt.toLocalTime()

        val today = LocalDate.now(zone)
        val label = when (date) {
            today -> "Сегодня"
            today.plusDays(1) -> "Завтра"
            else -> date.format(DateTimeFormatter.ofPattern("dd.MM"))
        }
        return "$label %02d:%02d".format(time.hour, time.minute)
    }
}
