package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemGerhanaBulanBinding
import site.elahady.alkaukaba.model.LunarEclipseItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class LunarEclipseAdapter : RecyclerView.Adapter<LunarEclipseAdapter.ViewHolder>() {

    private val items = ArrayList<LunarEclipseItem>()

    fun setData(newItems: List<LunarEclipseItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemGerhanaBulanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LunarEclipseItem) {
            val context = binding.root.context
            binding.tvTitle.text = "Gerhana Bulan ${item.kindLabel}"
            binding.tvDate.text = item.peakDateLabel
            binding.tvPeakTime.text = "Puncak: ${item.peakTimeLabel}"
            binding.tvMagnitude.text = "Magnitude: %.1f%%".format(Locale.US, item.magnitudePercent)

            if (item.visibleFromLocation) {
                binding.tvVisibility.text = "Terlihat dari lokasimu"
                binding.tvVisibility.setTextColor(ContextCompat.getColor(context, R.color.pill_green_text))
            } else {
                binding.tvVisibility.text = "Tidak terlihat dari lokasimu"
                binding.tvVisibility.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGerhanaBulanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
