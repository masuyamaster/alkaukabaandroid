package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.databinding.ItemJuzBinding
import site.elahady.alkaukaba.model.JuzSummary
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class JuzAdapter(private val onClick: (JuzSummary) -> Unit) : RecyclerView.Adapter<JuzAdapter.ViewHolder>() {

    private val items = ArrayList<JuzSummary>()

    fun setData(newItems: List<JuzSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemJuzBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: JuzSummary) {
            binding.tvNomorJuz.text = item.nomor.toString()
            binding.tvJudulJuz.text = "Juz ${item.nomor}"
            binding.tvRentangJuz.text = if (item.startSurahNamaLatin == item.endSurahNamaLatin) {
                item.startSurahNamaLatin
            } else {
                "${item.startSurahNamaLatin} - ${item.endSurahNamaLatin}"
            }
            binding.tvJumlahAyatJuz.text = "${item.jumlahAyat} ayat"
            // Nama Arab cuma ditampilkan kalau satu Juz murni satu surah (mis. Juz 2 = Al-Baqarah
            // saja) - kalau Juz merentang dua surah (mis. Juz 1 = Al-Fatihah - Al-Baqarah),
            // menampilkan nama Arab surah awal saja bikin kesan tidak sesuai dengan rentang yang
            // tertulis di tvRentangJuz, jadi disembunyikan saja.
            if (item.startSurahNamaLatin == item.endSurahNamaLatin) {
                binding.tvNamaArabJuz.text = item.startSurahNamaArab
                binding.tvNamaArabJuz.visibility = View.VISIBLE
            } else {
                binding.tvNamaArabJuz.visibility = View.GONE
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJuzBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
