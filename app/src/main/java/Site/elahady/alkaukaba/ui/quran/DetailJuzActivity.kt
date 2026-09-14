package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.JuzAyatAdapter
import site.elahady.alkaukaba.databinding.ActivityDetailJuzBinding
import site.elahady.alkaukaba.model.JuzAyat
import site.elahady.alkaukaba.model.JuzDetail
import site.elahady.alkaukaba.repo.DEFAULT_QORI_KEY
import site.elahady.alkaukaba.utils.MushafTextBuilder
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.quran.DetailJuzViewModel
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

/** Sama seperti [DetailSurahActivity] (kartu per-ayat + toggle mode Terjemahan/Mushaf + audio
 * per-ayat), tapi menampilkan satu Juz yang bisa merentang beberapa surah - lihat
 * [site.elahady.alkaukaba.model.JuzBoundaries]. Sengaja tidak ada tombol "Putar Surah" seperti
 * di layar surah (tidak ada audio full-Juz dari equran.id, dan menggabung banyak file audio
 * per-surah jadi satu playback berurutan di luar scope saat ini). */
class DetailJuzActivity : AppCompatActivity() {

    private enum class ReadingMode { TERJEMAHAN, MUSHAF }

    companion object {
        const val EXTRA_NOMOR_JUZ = "EXTRA_NOMOR_JUZ"
    }

    private lateinit var binding: ActivityDetailJuzBinding
    private lateinit var viewModel: DetailJuzViewModel
    private lateinit var adapter: JuzAyatAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var playingKey: Pair<Int, Int>? = null
    private var readingMode = ReadingMode.TERJEMAHAN
    private var currentAyatList: List<JuzAyat> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailJuzBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        val nomorJuz = intent.getIntExtra(EXTRA_NOMOR_JUZ, 1)
        binding.includeToolbar.tvToolbarTitle.text = "Juz $nomorJuz"
        binding.tvJudulJuz.text = "Juz $nomorJuz"

        viewModel = ViewModelProvider(this)[DetailJuzViewModel::class.java]

        setupRecyclerView()
        setupObserver()
        setupModeToggle()

        viewModel.fetchJuzDetail(nomorJuz)
    }

    private fun setupRecyclerView() {
        adapter = JuzAyatAdapter { juzAyat -> onPlayAyatClicked(juzAyat) }
        binding.rvJuzAyat.layoutManager = LinearLayoutManager(this)
        binding.rvJuzAyat.adapter = adapter
    }

    private fun setupModeToggle() {
        binding.tvModeTerjemahan.setOnClickListener { switchMode(ReadingMode.TERJEMAHAN) }
        binding.tvModeMushaf.setOnClickListener { switchMode(ReadingMode.MUSHAF) }
    }

    private fun switchMode(mode: ReadingMode) {
        readingMode = mode

        val activePill = if (mode == ReadingMode.TERJEMAHAN) binding.tvModeTerjemahan else binding.tvModeMushaf
        val inactivePill = if (mode == ReadingMode.TERJEMAHAN) binding.tvModeMushaf else binding.tvModeTerjemahan
        activePill.setBackgroundResource(R.drawable.bg_toggle_pill_active)
        activePill.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
        inactivePill.background = null
        inactivePill.setTextColor(ContextCompat.getColor(this, R.color.text_unselected))

        binding.rvJuzAyat.visibility = if (mode == ReadingMode.TERJEMAHAN) View.VISIBLE else View.GONE
        binding.scrollMushaf.visibility = if (mode == ReadingMode.MUSHAF) View.VISIBLE else View.GONE

        if (mode == ReadingMode.MUSHAF) {
            renderMushafText()
        }
    }

    private fun renderMushafText() {
        binding.tvMushaf.text = MushafTextBuilder.build(
            currentAyatList.map { it.ayat },
            circleColor = ContextCompat.getColor(this, R.color.gold_accent),
            numberColor = ContextCompat.getColor(this, R.color.navy_dongker)
        )
    }

    private fun setupObserver() {
        viewModel.juzDetail.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutContent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.layoutContent.visibility = View.VISIBLE
                    resource.data?.let { bindJuzDetail(it) }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutContent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun bindJuzDetail(detail: JuzDetail) {
        currentAyatList = detail.ayatList
        val first = detail.ayatList.firstOrNull()
        val last = detail.ayatList.lastOrNull()
        binding.tvRentangJuz.text = if (first != null && last != null) {
            "${first.surahNamaLatin} ${first.surahNomor}:${first.ayat.nomorAyat} - " +
                "${last.surahNamaLatin} ${last.surahNomor}:${last.ayat.nomorAyat}"
        } else {
            ""
        }
        adapter.setData(detail.ayatList)
        if (readingMode == ReadingMode.MUSHAF) {
            renderMushafText()
        }
    }

    private fun onPlayAyatClicked(juzAyat: JuzAyat) {
        val key = juzAyat.surahNomor to juzAyat.ayat.nomorAyat
        val wasPlayingThisAyat = playingKey == key
        stopPlayback()
        if (wasPlayingThisAyat) return // tap ulang di ayat yang sedang diputar = stop saja

        val audioUrl = juzAyat.ayat.audio[DEFAULT_QORI_KEY]
        if (audioUrl.isNullOrBlank()) {
            Toast.makeText(this, "Audio ayat ini tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        playingKey = key
        adapter.setPlayingAyat(juzAyat.surahNomor, juzAyat.ayat.nomorAyat)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { stopPlayback() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@DetailJuzActivity, "Gagal memutar audio ayat", Toast.LENGTH_SHORT).show()
                stopPlayback()
                true
            }
            try {
                prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(this@DetailJuzActivity, "Gagal memutar audio ayat", Toast.LENGTH_SHORT).show()
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingKey = null
        adapter.setPlayingAyat(null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
