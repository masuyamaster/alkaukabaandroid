package Site.elahady.alkaukaba.adapter

import Site.elahady.alkaukaba.api.HolidayItem
import Site.elahady.alkaukaba.databinding.ItemHolidayBinding
import android.view.LayoutInflater
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
            binding.tvType.text = if (item.is_cuti) "Cuti Bersama" else "Hari Libur Nasional"
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