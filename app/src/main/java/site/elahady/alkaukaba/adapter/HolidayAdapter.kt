package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.api.HolidayItem
import site.elahady.alkaukaba.databinding.ItemHolidayBinding
import site.elahady.alkaukaba.model.EventJenis
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HolidayAdapter : RecyclerView.Adapter<HolidayAdapter.ViewHolder>() {

    private val listHoliday = ArrayList<HolidayItem>()

    fun setData(items: List<HolidayItem>) {
        listHoliday.clear()
        listHoliday.addAll(items)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemHolidayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HolidayItem) {
            // Format Tanggal dari YYYY-MM-DD ke format Indonesia
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

            val date = inputFormat.parse(item.tanggal)
            val dateStr = if (date != null) outputFormat.format(date) else item.tanggal

            binding.tvDate.text = dateStr
            binding.tvName.text = item.keterangan

            // Logika sederhana untuk label tipe (API ini dominan nasional/cuti bersama)
            binding.tvType.text = item.tanggalHijriah

            // Catatan hanya diisi event astronomi (jam puncak + penjelasan singkat)
            binding.tvNote.text = item.catatan
            binding.tvNote.visibility = if (item.catatan.isNullOrBlank()) View.GONE else View.VISIBLE

            binding.ivHolidayIcon.setImageResource(
                if (item.jenis == EventJenis.ASTRONOMI) R.drawable.ic_sparkle else R.drawable.ic_menu_star
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHolidayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listHoliday[position])
    }

    override fun getItemCount(): Int = listHoliday.size
}