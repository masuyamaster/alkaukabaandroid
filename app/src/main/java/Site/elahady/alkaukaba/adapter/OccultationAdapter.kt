package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemOkultasiBinding
import site.elahady.alkaukaba.model.OccultationItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class OccultationAdapter : RecyclerView.Adapter<OccultationAdapter.ViewHolder>() {

    private val items = ArrayList<OccultationItem>()

    fun setData(newItems: List<OccultationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemOkultasiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OccultationItem) {
            val context = binding.root.context
            binding.tvTitle.text = "🪐 Okultasi ${item.bodyLabel}"
            binding.tvDate.text = "📅 ${item.eventDateLabel}"
            binding.tvPeakTime.text = "${item.peakTimeLabel.substringAfter(", ")} WIB"
            binding.tvMinSeparation.text = "%.1f′".format(Locale.US, item.minSeparationArcmin)

            if (item.isOccultation && item.ingressTimeLabel != null && item.egressTimeLabel != null) {
                binding.rowIngress.visibility = View.VISIBLE
                binding.rowEgress.visibility = View.VISIBLE
                binding.tvIngress.text = "${item.ingressTimeLabel.substringAfter(", ")} WIB"
                binding.tvEgress.text = "${item.egressTimeLabel.substringAfter(", ")} WIB"

                binding.tvKind.text = "🪐 Okultasi Penuh"
                binding.tvKind.setBackgroundResource(R.drawable.bg_pill_green)
                binding.tvKind.setTextColor(ContextCompat.getColor(context, R.color.pill_green_text))
            } else {
                binding.rowIngress.visibility = View.GONE
                binding.rowEgress.visibility = View.GONE

                binding.tvKind.text = "✨ Konjungsi Dekat (Tidak Tertutup Penuh)"
                binding.tvKind.setBackgroundResource(R.drawable.bg_pill_navy_light)
                binding.tvKind.setTextColor(ContextCompat.getColor(context, R.color.text_label_gold))
            }

            if (item.visibleFromLocation) {
                binding.tvVisibility.text = "👁️ Terlihat dari lokasimu"
                binding.tvVisibility.setBackgroundResource(R.drawable.bg_pill_green)
                binding.tvVisibility.setTextColor(ContextCompat.getColor(context, R.color.pill_green_text))
            } else {
                binding.tvVisibility.text = "🚫 Tidak terlihat dari lokasimu"
                binding.tvVisibility.setBackgroundResource(R.drawable.bg_pill_red)
                binding.tvVisibility.setTextColor(ContextCompat.getColor(context, R.color.pill_red_text))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOkultasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
