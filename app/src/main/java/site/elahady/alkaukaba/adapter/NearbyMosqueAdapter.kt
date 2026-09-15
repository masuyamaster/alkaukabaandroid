package site.elahady.alkaukaba.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import site.elahady.alkaukaba.databinding.ItemNearbyMosqueBinding
import site.elahady.alkaukaba.model.NearbyMosque
import java.util.Locale

class NearbyMosqueAdapter(private val onClick: (NearbyMosque) -> Unit) :
    RecyclerView.Adapter<NearbyMosqueAdapter.ViewHolder>() {

    private val items = ArrayList<NearbyMosque>()

    fun setData(newItems: List<NearbyMosque>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemNearbyMosqueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NearbyMosque) {
            binding.tvNamaMasjid.text = item.name
            binding.tvJarakMasjid.text = formatDistance(item.distanceMeters)

            if (item.address.isNullOrBlank()) {
                binding.tvAlamatMasjid.visibility = android.view.View.GONE
            } else {
                binding.tvAlamatMasjid.text = item.address
                binding.tvAlamatMasjid.visibility = android.view.View.VISIBLE
            }

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun formatDistance(meters: Float): String {
            return if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                String.format(Locale("id", "ID"), "%.1f km", meters / 1000)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyMosqueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
