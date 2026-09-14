package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ItemAyatBinding
import site.elahady.alkaukaba.databinding.ItemJuzSurahHeaderBinding
import site.elahady.alkaukaba.model.JuzAyat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

sealed class JuzListItem {
    data class SurahHeader(val namaLatin: String) : JuzListItem()
    data class AyatRow(val juzAyat: JuzAyat) : JuzListItem()
}

/** Sama seperti [AyatAdapter] (play/pause per-ayat, highlight), tapi untuk layar Juz yang bisa
 * menampilkan ayat dari beberapa surah berurutan - menyisipkan [JuzListItem.SurahHeader] setiap
 * kali surah ganti (dari [JuzAyat.isFirstOfSurah]), pola multi-view-type sama seperti
 * [SearchResultAdapter]. "Sedang diputar"/"di-highlight" diidentifikasi lewat pasangan
 * (surahNomor, nomorAyat) karena nomor ayat bisa sama di surah berbeda. */
class JuzAyatAdapter(private val onPlayClick: (JuzAyat) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_AYAT = 1
    }

    private val items = ArrayList<JuzListItem>()
    private var playingKey: Pair<Int, Int>? = null

    fun setData(juzAyatList: List<JuzAyat>) {
        items.clear()
        juzAyatList.forEach { juzAyat ->
            if (juzAyat.isFirstOfSurah) items.add(JuzListItem.SurahHeader(juzAyat.surahNamaLatin))
            items.add(JuzListItem.AyatRow(juzAyat))
        }
        playingKey = null
        notifyDataSetChanged()
    }

    /** Dipanggil dari Activity setiap kali MediaPlayer ganti ayat/berhenti - null berarti
     * berhenti total (tidak ada ayat yang sedang diputar). */
    fun setPlayingAyat(surahNomor: Int?, nomorAyat: Int?) {
        val previousIndex = indexOfKey(playingKey)
        playingKey = if (surahNomor != null && nomorAyat != null) surahNomor to nomorAyat else null
        val newIndex = indexOfKey(playingKey)
        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    private fun indexOfKey(key: Pair<Int, Int>?): Int {
        if (key == null) return -1
        return items.indexOfFirst { it is JuzListItem.AyatRow && it.juzAyat.surahNomor == key.first && it.juzAyat.ayat.nomorAyat == key.second }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is JuzListItem.SurahHeader -> TYPE_HEADER
        is JuzListItem.AyatRow -> TYPE_AYAT
    }

    class HeaderViewHolder(val binding: ItemJuzSurahHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AyatViewHolder(val binding: ItemAyatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemJuzSurahHeaderBinding.inflate(inflater, parent, false))
        } else {
            AyatViewHolder(ItemAyatBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is JuzListItem.SurahHeader -> {
                (holder as HeaderViewHolder).binding.root.text = item.namaLatin
            }
            is JuzListItem.AyatRow -> {
                val binding = (holder as AyatViewHolder).binding
                val context = binding.root.context
                val juzAyat = item.juzAyat
                binding.tvNomorAyat.text = juzAyat.ayat.nomorAyat.toString()
                binding.tvTeksArab.text = juzAyat.ayat.teksArab
                binding.tvTeksLatin.text = juzAyat.ayat.teksLatin
                binding.tvTeksIndonesia.text = juzAyat.ayat.teksIndonesia

                val isPlaying = playingKey == (juzAyat.surahNomor to juzAyat.ayat.nomorAyat)
                binding.cardAyat.setCardBackgroundColor(
                    ContextCompat.getColor(context, if (isPlaying) R.color.card_gold_tint else R.color.card_white)
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

                binding.btnPlayAyat.setOnClickListener { onPlayClick(juzAyat) }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
