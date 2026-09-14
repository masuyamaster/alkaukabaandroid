package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.adapter.AyatAdapter
import site.elahady.alkaukaba.databinding.ActivityDetailSurahBinding
import site.elahady.alkaukaba.model.Ayat
import site.elahady.alkaukaba.model.SurahDetail
import site.elahady.alkaukaba.repo.DEFAULT_QORI_KEY
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.quran.DetailSurahViewModel
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class DetailSurahActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOMOR_SURAH = "EXTRA_NOMOR_SURAH"
    }

    private lateinit var binding: ActivityDetailSurahBinding
    private lateinit var viewModel: DetailSurahViewModel
    private lateinit var adapter: AyatAdapter

    private var mediaPlayer: MediaPlayer? = null
    private var playingAyatNomor: Int? = null
    private var isDeskripsiExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailSurahBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        val nomorSurah = intent.getIntExtra(EXTRA_NOMOR_SURAH, 1)
        viewModel = ViewModelProvider(this)[DetailSurahViewModel::class.java]

        setupRecyclerView()
        setupObserver()
        setupDeskripsiToggle()

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
        adapter.setData(detail.ayat)
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

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingAyatNomor = null
        adapter.setPlayingAyat(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
