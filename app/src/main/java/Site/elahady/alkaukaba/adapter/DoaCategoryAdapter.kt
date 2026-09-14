package site.elahady.alkaukaba.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import site.elahady.alkaukaba.databinding.ItemDoaKategoriBinding
import site.elahady.alkaukaba.model.DoaCategory

class DoaCategoryAdapter(private val onClick: (DoaCategory) -> Unit) :
    RecyclerView.Adapter<DoaCategoryAdapter.ViewHolder>() {

    private val items = ArrayList<DoaCategory>()

    fun setData(newItems: List<DoaCategory>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemDoaKategoriBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DoaCategory) {
            binding.tvNamaKategori.text = item.name
            binding.tvJumlahItem.text = "${item.items_count} doa/dzikir"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoaKategoriBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
