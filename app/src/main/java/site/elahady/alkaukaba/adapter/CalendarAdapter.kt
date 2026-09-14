package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

// Model Data Sederhana untuk UI
data class DayUIModel(
    val date: Date?,
    val dayValue: String,
    val hijriDay: String,
    val isHoliday: Boolean,
    val isToday: Boolean,
    val isEmpty: Boolean,
    val isSelected: Boolean = false
)

class CalendarAdapter(
    private val onDayClick: ((Date) -> Unit)? = null
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    private val listDays = ArrayList<DayUIModel>()

    fun setData(items: List<DayUIModel>) {
        listDays.clear()
        listDays.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.findViewById(R.id.containerDay)
        val tvDateNum: TextView = itemView.findViewById(R.id.tvDateNum)
        val tvHijriNum: TextView = itemView.findViewById(R.id.tvHijriNum)
        val bgToday: View = itemView.findViewById(R.id.bgToday)
        val bgSelected: View = itemView.findViewById(R.id.bgSelected)
        val dotHoliday: View = itemView.findViewById(R.id.dotHoliday)

        fun bind(item: DayUIModel) {
            if (item.isEmpty) {
                // Sembunyikan isi jika ini adalah kotak kosong (padding layout)
                container.visibility = View.INVISIBLE
                container.setOnClickListener(null)
                return
            } else {
                container.visibility = View.VISIBLE
            }

            tvDateNum.text = item.dayValue
            tvHijriNum.text = item.hijriDay
            tvDateNum.setTextColor(itemView.context.getColor(android.R.color.white))

            bgToday.visibility = if (item.isToday) View.VISIBLE else View.GONE
            // Cincin gold hanya ditampilkan kalau tanggal terpilih BUKAN hari ini,
            // supaya tidak dobel dengan bgToday yang sudah solid.
            bgSelected.visibility = if (item.isSelected && !item.isToday) View.VISIBLE else View.GONE

            dotHoliday.visibility = if (item.isHoliday) View.VISIBLE else View.GONE

            container.setOnClickListener {
                item.date?.let { onDayClick?.invoke(it) }
            }
        }
    }

    // onCreateViewHolder sama seperti sebelumnya ...
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listDays[position])
    }

    override fun getItemCount(): Int = listDays.size
}