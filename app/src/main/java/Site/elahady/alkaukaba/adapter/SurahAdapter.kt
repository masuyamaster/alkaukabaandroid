package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.databinding.ItemSurahBinding
import site.elahady.alkaukaba.model.Surah
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SurahAdapter(private val onClick: (Surah) -> Unit) : RecyclerView.Adapter<SurahAdapter.ViewHolder>() {

    private val items = ArrayList<Surah>()

    fun setData(newItems: List<Surah>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemSurahBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Surah) {
            binding.tvNomor.text = item.nomor.toString()
            binding.tvNamaLatin.text = item.namaLatin
            binding.tvArti.text = item.arti
            binding.tvTempatTurun.text = item.tempatTurun
            binding.tvJumlahAyat.text = "${item.jumlahAyat} ayat"
            binding.tvNamaArab.text = item.nama
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSurahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
