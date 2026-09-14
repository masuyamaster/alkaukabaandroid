package site.elahady.alkaukaba.adapter

import site.elahady.alkaukaba.databinding.ItemAyatSearchResultBinding
import site.elahady.alkaukaba.databinding.ItemSearchHeaderBinding
import site.elahady.alkaukaba.databinding.ItemSurahBinding
import site.elahady.alkaukaba.model.AyatSearchMatch
import site.elahady.alkaukaba.model.Surah
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

sealed class SearchResultItem {
    data class Header(val title: String) : SearchResultItem()
    data class SurahResult(val surah: Surah) : SearchResultItem()
    data class AyatResult(val match: AyatSearchMatch, val namaSurah: String) : SearchResultItem()
}

class SearchResultAdapter(
    private val onSurahClick: (Surah) -> Unit,
    private val onAyatClick: (AyatSearchMatch) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SURAH = 1
        private const val TYPE_AYAT = 2
    }

    private val items = ArrayList<SearchResultItem>()

    fun setData(newItems: List<SearchResultItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SearchResultItem.Header -> TYPE_HEADER
        is SearchResultItem.SurahResult -> TYPE_SURAH
        is SearchResultItem.AyatResult -> TYPE_AYAT
    }

    class HeaderViewHolder(val binding: ItemSearchHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class SurahViewHolder(val binding: ItemSurahBinding) : RecyclerView.ViewHolder(binding.root)
    class AyatViewHolder(val binding: ItemAyatSearchResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemSearchHeaderBinding.inflate(inflater, parent, false))
            TYPE_SURAH -> SurahViewHolder(ItemSurahBinding.inflate(inflater, parent, false))
            else -> AyatViewHolder(ItemAyatSearchResultBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchResultItem.Header -> {
                (holder as HeaderViewHolder).binding.root.text = item.title
            }
            is SearchResultItem.SurahResult -> {
                val binding = (holder as SurahViewHolder).binding
                val surah = item.surah
                binding.tvNomor.text = surah.nomor.toString()
                binding.tvNamaLatin.text = surah.namaLatin
                binding.tvArti.text = surah.arti
                binding.tvTempatTurun.text = surah.tempatTurun
                binding.tvJumlahAyat.text = "${surah.jumlahAyat} ayat"
                binding.tvNamaArab.text = surah.nama
                binding.root.setOnClickListener { onSurahClick(surah) }
            }
            is SearchResultItem.AyatResult -> {
                val binding = (holder as AyatViewHolder).binding
                binding.tvNamaSurah.text = item.namaSurah
                binding.tvNomorAyat.text = "Ayat ${item.match.numberInSurah}"
                binding.tvCuplikan.text = item.match.text
                binding.root.setOnClickListener { onAyatClick(item.match) }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
