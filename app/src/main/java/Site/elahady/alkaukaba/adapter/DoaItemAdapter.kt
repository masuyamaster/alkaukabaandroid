package site.elahady.alkaukaba.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import site.elahady.alkaukaba.databinding.ItemDoaBinding
import site.elahady.alkaukaba.model.DoaItem

class DoaItemAdapter : RecyclerView.Adapter<DoaItemAdapter.ViewHolder>() {

    private val items = ArrayList<DoaItem>()

    fun setData(newItems: List<DoaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemDoaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DoaItem, nomor: Int) {
            binding.tvNomor.text = nomor.toString()
            binding.tvJudul.text = item.title
            binding.tvTeksArab.text = item.arabic
            binding.tvTeksLatin.text = item.latin
            binding.tvTerjemahan.text = item.translation

            bindOptional(binding.tvCatatan, item.notes)
            bindOptional(binding.tvFawaid, item.fawaid)
            bindOptional(binding.tvSumber, item.source?.let { "Sumber: $it" })
        }

        private fun bindOptional(view: android.widget.TextView, value: String?) {
            if (value.isNullOrBlank()) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                view.text = value
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size
}
