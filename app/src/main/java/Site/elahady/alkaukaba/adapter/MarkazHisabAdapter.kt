package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemMarkazHisabBinding
import site.elahady.alkaukaba.model.MarkazHisabResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MarkazHisabAdapter : RecyclerView.Adapter<MarkazHisabAdapter.ViewHolder>() {

    private val items = ArrayList<MarkazHisabResult>()

    fun setData(newItems: List<MarkazHisabResult>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemMarkazHisabBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MarkazHisabResult) {
            val context = binding.root.context
            binding.tvMarkazName.text = "📍 ${item.markaz.nama}, ${item.markaz.provinsi}"
            binding.tvGhurub.text = item.hasil.ghurubTime.substringAfter(", ")
            binding.tvTinggiHilal.text = "%.2f°".format(item.hasil.tinggiHilal)
            binding.tvElongasi.text = "%.2f°".format(item.hasil.elongasi)

            if (item.hasil.hilalMemenuhiKriteria) {
                binding.tvStatusBadge.text = "Memenuhi"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.pill_green_text))
            } else {
                binding.tvStatusBadge.text = "Belum Memenuhi"
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_red)
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.pill_red_text))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarkazHisabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
