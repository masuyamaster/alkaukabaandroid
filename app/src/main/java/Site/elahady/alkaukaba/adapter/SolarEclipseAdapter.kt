package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemGerhanaMatahariBinding
import site.elahady.alkaukaba.model.SolarEclipseItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SolarEclipseAdapter : RecyclerView.Adapter<SolarEclipseAdapter.ViewHolder>() {

    private val items = ArrayList<SolarEclipseItem>()

    fun setData(newItems: List<SolarEclipseItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemGerhanaMatahariBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SolarEclipseItem) {
            val context = binding.root.context
            binding.tvTitle.text = "Gerhana Matahari ${item.kindLabel}"
            binding.tvDate.text = item.peakDateLabel
            binding.tvPartialBegin.text = "Mulai: ${item.partialBeginLabel}"
            binding.tvPeakTime.text = "Puncak: ${item.peakTimeLabel}"
            binding.tvPartialEnd.text = "Berakhir: ${item.partialEndLabel}"
            binding.tvMagnitude.text = "Magnitude: %.1f%%".format(Locale.US, item.magnitudePercent)

            if (item.totalBeginLabel != null && item.totalEndLabel != null) {
                binding.tvTotalRange.visibility = View.VISIBLE
                binding.tvTotalRange.text = "Fase Total/Cincin: ${item.totalBeginLabel} – ${item.totalEndLabel}"
            } else {
                binding.tvTotalRange.visibility = View.GONE
            }

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
        val binding = ItemGerhanaMatahariBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
