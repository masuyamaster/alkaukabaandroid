package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemAyatBinding
import site.elahady.alkaukaba.model.Ayat
import site.elahady.alkaukaba.utils.applyDisplayPrefs
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AyatAdapter(private val onPlayClick: (Ayat) -> Unit) : RecyclerView.Adapter<AyatAdapter.ViewHolder>() {

    private val items = ArrayList<Ayat>()
    private var playingAyatNomor: Int? = null
    private var highlightedAyatNomor: Int? = null

    fun setData(newItems: List<Ayat>) {
        items.clear()
        items.addAll(newItems)
        playingAyatNomor = null
        highlightedAyatNomor = null
        notifyDataSetChanged()
    }

    fun indexOf(nomorAyat: Int): Int = items.indexOfFirst { it.nomorAyat == nomorAyat }

    /** Dipanggil dari Activity setiap kali MediaPlayer ganti ayat/berhenti, supaya highlight
     * kartu & ikon play/pause ikut pindah tanpa refresh seluruh list. */
    fun setPlayingAyat(nomorAyat: Int?) {
        val previousIndex = items.indexOfFirst { it.nomorAyat == playingAyatNomor }
        playingAyatNomor = nomorAyat
        val newIndex = items.indexOfFirst { it.nomorAyat == playingAyatNomor }
        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    /** Highlight sementara (beda dari [setPlayingAyat]) dipakai saat user datang dari hasil
     * pencarian ayat di DaftarSurahActivity - menandai kartu yang dicari tanpa ikut memutar
     * audionya (ikon play/pause tetap mengikuti status [playingAyatNomor] saja). */
    fun setHighlightedAyat(nomorAyat: Int?) {
        val previousIndex = items.indexOfFirst { it.nomorAyat == highlightedAyatNomor }
        highlightedAyatNomor = nomorAyat
        val newIndex = items.indexOfFirst { it.nomorAyat == highlightedAyatNomor }
        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class ViewHolder(private val binding: ItemAyatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Ayat) {
            val context = binding.root.context
            applyDisplayPrefs(binding, context)
            binding.tvNomorAyat.text = item.nomorAyat.toString()
            binding.tvTeksArab.text = item.teksArab
            binding.tvTeksLatin.text = item.teksLatin
            binding.tvTeksIndonesia.text = item.teksIndonesia

            val isPlaying = item.nomorAyat == playingAyatNomor
            val isHighlighted = item.nomorAyat == highlightedAyatNomor
            binding.cardAyat.setCardBackgroundColor(
                ContextCompat.getColor(context, if (isPlaying || isHighlighted) R.color.card_gold_tint else R.color.card_white)
            )
            if (isPlaying) {
                binding.btnPlayAyat.setBackgroundResource(R.drawable.bg_circle_button)
                binding.btnPlayAyat.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gold_accent)
                binding.btnPlayAyat.setImageResource(R.drawable.ic_pause)
                binding.btnPlayAyat.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
            } else {
                binding.btnPlayAyat.setBackgroundResource(R.drawable.bg_circle_outline_gold)
                binding.btnPlayAyat.backgroundTintList = null
                binding.btnPlayAyat.setImageResource(R.drawable.ic_play)
                binding.btnPlayAyat.imageTintList = ContextCompat.getColorStateList(context, R.color.gold_accent)
            }

            binding.btnPlayAyat.setOnClickListener { onPlayClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAyatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
