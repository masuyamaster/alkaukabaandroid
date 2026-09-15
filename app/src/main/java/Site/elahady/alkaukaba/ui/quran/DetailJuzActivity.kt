package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.JuzAyatAdapter
import site.elahady.alkaukaba.databinding.ActivityDetailJuzBinding
import site.elahady.alkaukaba.model.JuzAyat
import site.elahady.alkaukaba.model.JuzDetail
import site.elahady.alkaukaba.repo.DEFAULT_QORI_KEY
import site.elahady.alkaukaba.utils.MushafTextBuilder
import site.elahady.alkaukaba.utils.QuranDisplayPrefs
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
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
 * per-ayat + pengaturan ukuran huruf/spasi), tapi menampilkan satu Juz yang bisa merentang
 * beberapa surah - lihat [site.elahady.alkaukaba.model.JuzBoundaries]. Tombol "Putar Juz"
 * beda mekanisme dari "Putar Surah" di [DetailSurahActivity] (yang pakai satu file audioFull) -
 * di sini diimplementasi sebagai playlist berantai per-ayat, lihat [playJuzAyatAt]. */
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
    private var isJuzPlaying = false
    private var readingMode = ReadingMode.TERJEMAHAN
    private var currentAyatList: List<JuzAyat> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailJuzBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.btnBack.setOnClickListener { finish() }
        setupDisplaySettingsButton()

        val nomorJuz = intent.getIntExtra(EXTRA_NOMOR_JUZ, 1)
        binding.includeToolbar.tvToolbarTitle.text = "Juz $nomorJuz"
        binding.tvJudulJuz.text = "Juz $nomorJuz"

        viewModel = ViewModelProvider(this)[DetailJuzViewModel::class.java]

        setupRecyclerView()
        setupObserver()
        setupModeToggle()
        setupPlayJuzButton()

        viewModel.fetchJuzDetail(nomorJuz)
    }

    private fun setupRecyclerView() {
        adapter = JuzAyatAdapter { juzAyat -> onPlayAyatClicked(juzAyat) }
        binding.rvJuzAyat.layoutManager = LinearLayoutManager(this)
        binding.rvJuzAyat.adapter = adapter
    }

    private fun setupPlayJuzButton() {
        binding.btnPlayJuz.setOnClickListener { onPlayJuzClicked() }
    }

    /** Tombol aksi toolbar khusus layar ini (bukan di layar Konfigurasi global) buat atur
     * ukuran huruf & spasi kartu ayat - lihat [QuranDisplayPrefs]/[QuranDisplaySettingsSheet]. */
    private fun setupDisplaySettingsButton() {
        binding.includeToolbar.btnToolbarAction.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_settings)
            contentDescription = getString(R.string.quran_display_settings)
            setOnClickListener {
                QuranDisplaySettingsSheet.show(this@DetailJuzActivity) {
                    adapter.notifyDataSetChanged()
                    if (readingMode == ReadingMode.MUSHAF) renderMushafText()
                }
            }
        }
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
        val textSize = QuranDisplayPrefs.getTextSizeLevel(this)
        val spacing = QuranDisplayPrefs.getSpacingLevel(this)
        binding.tvMushaf.textSize = textSize.mushafSp
        binding.tvMushaf.setLineSpacing(0f, spacing.mushafLineSpacing)
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

    /** equran.id cuma punya audio per-ayat & audio full-surah (bukan per-Juz), dan audio
     * full-surah tidak bisa dipakai untuk Juz karena batasnya sering jatuh di tengah surah
     * (mis. Juz 1 cuma sampai Al-Baqarah ayat 141 dari 286 ayat). Jadi "Putar Juz" diimplementasi
     * sebagai playlist berantai: putar ayat pertama, begitu kelar (onCompletion) otomatis lanjut
     * ke ayat berikutnya dalam [currentAyatList], sampai ayat terakhir baru berhenti. */
    private fun onPlayJuzClicked() {
        val wasPlayingJuz = isJuzPlaying
        stopPlayback()
        if (wasPlayingJuz) return // tap ulang saat sedang diputar = stop saja

        if (currentAyatList.isEmpty()) {
            Toast.makeText(this, "Tidak ada ayat untuk diputar", Toast.LENGTH_SHORT).show()
            return
        }

        isJuzPlaying = true
        updatePlayJuzButtonUi()
        playJuzAyatAt(0)
    }

    private fun playJuzAyatAt(index: Int) {
        if (index >= currentAyatList.size) {
            stopPlayback()
            return
        }

        val juzAyat = currentAyatList[index]
        val audioUrl = juzAyat.ayat.audio[DEFAULT_QORI_KEY]
        if (audioUrl.isNullOrBlank()) {
            playJuzAyatAt(index + 1) // lewati ayat yang audionya tidak tersedia
            return
        }

        mediaPlayer?.release()
        playingKey = juzAyat.surahNomor to juzAyat.ayat.nomorAyat
        adapter.setPlayingAyat(juzAyat.surahNomor, juzAyat.ayat.nomorAyat)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { playJuzAyatAt(index + 1) }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@DetailJuzActivity, "Gagal memutar audio juz", Toast.LENGTH_SHORT).show()
                stopPlayback()
                true
            }
            try {
                prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(this@DetailJuzActivity, "Gagal memutar audio juz", Toast.LENGTH_SHORT).show()
                stopPlayback()
            }
        }
    }

    private fun updatePlayJuzButtonUi() {
        binding.ivPlayJuzIcon.setImageResource(if (isJuzPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        binding.tvPlayJuzLabel.text = getString(
            if (isJuzPlaying) R.string.quran_pause_juz_label else R.string.quran_play_juz_label
        )
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingKey = null
        adapter.setPlayingAyat(null, null)
        isJuzPlaying = false
        updatePlayJuzButtonUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
