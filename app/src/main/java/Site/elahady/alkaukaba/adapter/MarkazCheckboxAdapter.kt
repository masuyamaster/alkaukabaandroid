package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.databinding.ItemMarkazCheckboxBinding
import site.elahady.alkaukaba.model.MarkazNasional
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// `selectedIds` sengaja dipegang di Activity (bukan state internal adapter) supaya tombol
// "Pilih Semua"/"Pakai Default" bisa mengubah banyak item sekaligus lalu cukup panggil
// notifyDataSetChanged() sekali, tanpa adapter perlu tahu logika bulk-select.
class MarkazCheckboxAdapter(
    private val items: List<MarkazNasional>,
    private val selectedIds: MutableSet<String>,
    private val onToggle: (MarkazNasional, Boolean) -> Unit
) : RecyclerView.Adapter<MarkazCheckboxAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMarkazCheckboxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MarkazNasional) {
            binding.tvKota.text = item.nama
            binding.tvProvinsi.text = item.provinsi
            binding.cbMarkaz.isChecked = item.id in selectedIds

            binding.rowMarkaz.setOnClickListener {
                val nowChecked = item.id !in selectedIds
                if (nowChecked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                binding.cbMarkaz.isChecked = nowChecked
                onToggle(item, nowChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarkazCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
