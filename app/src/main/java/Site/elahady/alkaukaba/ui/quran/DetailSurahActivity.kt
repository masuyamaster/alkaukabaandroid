package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.AyatAdapter
import site.elahady.alkaukaba.databinding.ActivityDetailSurahBinding
import site.elahady.alkaukaba.model.Ayat
import site.elahady.alkaukaba.model.SurahDetail
import site.elahady.alkaukaba.repo.DEFAULT_QORI_KEY
import site.elahady.alkaukaba.utils.MushafTextBuilder
import site.elahady.alkaukaba.utils.QuranDisplayPrefs
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.quran.DetailSurahViewModel
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class DetailSurahActivity : AppCompatActivity() {

    /** TERJEMAHAN = tampilan lama (kartu per-ayat: Arab + transliterasi + terjemahan + tombol
     * audio). MUSHAF = teks Arab mengalir jadi satu paragraf tanpa terjemahan/transliterasi,
     * meniru halaman mushaf fisik - baca-saja, tombol audio tetap di mode Terjemahan. */
    private enum class ReadingMode { TERJEMAHAN, MUSHAF }

    companion object {
        const val EXTRA_NOMOR_SURAH = "EXTRA_NOMOR_SURAH"
        const val EXTRA_HIGHLIGHT_AYAT = "EXTRA_HIGHLIGHT_AYAT"
        private const val HIGHLIGHT_DURATION_MS = 3000L
    }

    private lateinit var binding: ActivityDetailSurahBinding
    private lateinit var viewModel: DetailSurahViewModel
    private lateinit var adapter: AyatAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var playingAyatNomor: Int? = null
    private var isFullSurahPlaying = false
    private var isDeskripsiExpanded = false
    private var highlightAyatNomor: Int = -1
    private var highlightApplied = false
    private var readingMode = ReadingMode.TERJEMAHAN
    private var currentAyatList: List<Ayat> = emptyList()
    private var currentAudioFull: Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailSurahBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.btnBack.setOnClickListener { finish() }
        setupDisplaySettingsButton()

        val nomorSurah = intent.getIntExtra(EXTRA_NOMOR_SURAH, 1)
        highlightAyatNomor = intent.getIntExtra(EXTRA_HIGHLIGHT_AYAT, -1)
        viewModel = ViewModelProvider(this)[DetailSurahViewModel::class.java]

        setupRecyclerView()
        setupObserver()
        setupDeskripsiToggle()
        setupModeToggle()
        setupPlaySurahButton()

        viewModel.fetchSurahDetail(nomorSurah)
    }

    private fun setupRecyclerView() {
        adapter = AyatAdapter { ayat -> onPlayAyatClicked(ayat) }
        binding.rvAyat.layoutManager = LinearLayoutManager(this)
        binding.rvAyat.adapter = adapter
    }

    private fun setupDeskripsiToggle() {
        binding.tvToggleDeskripsi.setOnClickListener {
            isDeskripsiExpanded = !isDeskripsiExpanded
            binding.tvDeskripsi.maxLines = if (isDeskripsiExpanded) Int.MAX_VALUE else 2
            binding.tvToggleDeskripsi.text = if (isDeskripsiExpanded) "Sembunyikan" else "Baca selengkapnya"
        }
    }

    private fun setupPlaySurahButton() {
        binding.btnPlaySurah.setOnClickListener { onPlaySurahClicked() }
    }

    /** Tombol aksi toolbar khusus layar ini (bukan di layar Konfigurasi global) buat atur
     * ukuran huruf & spasi kartu ayat - lihat [QuranDisplayPrefs]/[QuranDisplaySettingsSheet]. */
    private fun setupDisplaySettingsButton() {
        binding.includeToolbar.btnToolbarAction.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_settings)
            contentDescription = getString(R.string.quran_display_settings)
            setOnClickListener {
                QuranDisplaySettingsSheet.show(this@DetailSurahActivity) {
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

        binding.rvAyat.visibility = if (mode == ReadingMode.TERJEMAHAN) View.VISIBLE else View.GONE
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
            currentAyatList,
            circleColor = ContextCompat.getColor(this, R.color.gold_accent),
            numberColor = ContextCompat.getColor(this, R.color.navy_dongker)
        )
    }

    private fun setupObserver() {
        viewModel.surahDetail.observe(this) { resource ->
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
                    resource.data?.let { bindSurahDetail(it) }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutContent.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun bindSurahDetail(detail: SurahDetail) {
        binding.includeToolbar.tvToolbarTitle.text = detail.namaLatin
        binding.tvNamaArab.text = detail.nama
        binding.tvArti.text = detail.arti
        binding.tvTempatTurun.text = detail.tempatTurun
        binding.tvJumlahAyat.text = "${detail.jumlahAyat} Ayat"
        binding.tvDeskripsi.text = android.text.Html.fromHtml(detail.deskripsi, android.text.Html.FROM_HTML_MODE_COMPACT)
        currentAyatList = detail.ayat
        currentAudioFull = detail.audioFull
        adapter.setData(detail.ayat)
        if (readingMode == ReadingMode.MUSHAF) {
            renderMushafText()
        }
        scrollToHighlightedAyatIfNeeded()
    }

    /** Dipanggil dari layar pencarian ayat (DaftarSurahActivity) - scroll ke ayat yang dicari
     * lalu kasih highlight sementara (dibersihkan otomatis setelah [HIGHLIGHT_DURATION_MS])
     * supaya user langsung lihat ayat yang dimaksud tanpa perlu scroll manual. Cuma dijalankan
     * sekali (bukan tiap kali surahDetail LiveData emit ulang, mis. habis putar audio). */
    private fun scrollToHighlightedAyatIfNeeded() {
        if (highlightApplied || highlightAyatNomor < 0) return
        val position = adapter.indexOf(highlightAyatNomor)
        if (position < 0) return
        highlightApplied = true

        binding.rvAyat.post {
            (binding.rvAyat.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
        }
        adapter.setHighlightedAyat(highlightAyatNomor)
        binding.rvAyat.postDelayed({ adapter.setHighlightedAyat(null) }, HIGHLIGHT_DURATION_MS)
    }

    private fun onPlayAyatClicked(ayat: Ayat) {
        val wasPlayingThisAyat = playingAyatNomor == ayat.nomorAyat
        stopPlayback()
        if (wasPlayingThisAyat) return // tap ulang di ayat yang sedang diputar = stop saja

        val audioUrl = ayat.audio[DEFAULT_QORI_KEY]
        if (audioUrl.isNullOrBlank()) {
            Toast.makeText(this, "Audio ayat ini tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        playingAyatNomor = ayat.nomorAyat
        adapter.setPlayingAyat(ayat.nomorAyat)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { stopPlayback() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@DetailSurahActivity, "Gagal memutar audio ayat", Toast.LENGTH_SHORT).show()
                stopPlayback()
                true
            }
            try {
                prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(this@DetailSurahActivity, "Gagal memutar audio ayat", Toast.LENGTH_SHORT).show()
                stopPlayback()
            }
        }
    }

    /** Sengaja satu MediaPlayer dipakai gantian untuk audio per-ayat maupun full-surah -
     * keduanya tidak pernah diputar bersamaan, jadi mulai salah satu otomatis menghentikan
     * yang lain lewat pemanggilan [stopPlayback] ini. */
    private fun onPlaySurahClicked() {
        val wasPlayingFullSurah = isFullSurahPlaying
        stopPlayback()
        if (wasPlayingFullSurah) return // tap ulang saat sedang diputar = stop saja

        val audioUrl = currentAudioFull?.get(DEFAULT_QORI_KEY)
        if (audioUrl.isNullOrBlank()) {
            Toast.makeText(this, "Audio surah ini tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        isFullSurahPlaying = true
        updatePlaySurahButtonUi()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { stopPlayback() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@DetailSurahActivity, "Gagal memutar audio surah", Toast.LENGTH_SHORT).show()
                stopPlayback()
                true
            }
            try {
                prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(this@DetailSurahActivity, "Gagal memutar audio surah", Toast.LENGTH_SHORT).show()
                stopPlayback()
            }
        }
    }

    private fun updatePlaySurahButtonUi() {
        binding.ivPlaySurahIcon.setImageResource(if (isFullSurahPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        binding.tvPlaySurahLabel.text = getString(
            if (isFullSurahPlaying) R.string.quran_pause_surah_label else R.string.quran_play_surah_label
        )
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingAyatNomor = null
        adapter.setPlayingAyat(null)
        isFullSurahPlaying = false
        updatePlaySurahButtonUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
