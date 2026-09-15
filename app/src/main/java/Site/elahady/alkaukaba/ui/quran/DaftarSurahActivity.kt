package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.JuzAdapter
import site.elahady.alkaukaba.adapter.SearchResultAdapter
import site.elahady.alkaukaba.adapter.SearchResultItem
import site.elahady.alkaukaba.adapter.SurahAdapter
import site.elahady.alkaukaba.databinding.ActivityDaftarSurahBinding
import site.elahady.alkaukaba.model.AyatSearchMatch
import site.elahady.alkaukaba.model.JuzBoundaries
import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.utils.Resource
import androidx.core.content.ContextCompat
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.quran.DaftarSurahViewModel
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class DaftarSurahActivity : AppCompatActivity() {

    private enum class BrowseMode { SURAT, JUZ }

    private lateinit var binding: ActivityDaftarSurahBinding
    private lateinit var viewModel: DaftarSurahViewModel
    private lateinit var adapter: SurahAdapter
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var juzAdapter: JuzAdapter

    private var allSurah: List<Surah> = emptyList()
    private var currentQuery: String = ""
    private var latestAyatResult: List<AyatSearchMatch> = emptyList()
    private var isAyatSearchLoading: Boolean = false
    private var browseMode = BrowseMode.SURAT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarSurahBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Al-Qur'an"
        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[DaftarSurahViewModel::class.java]

        setupRecyclerView()
        setupSearchResultRecyclerView()
        setupJuzRecyclerView()
        setupObserver()
        setupSearchInput()
        setupBrowseModeToggle()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchSurahList()
        }
    }

    private fun setupRecyclerView() {
        adapter = SurahAdapter { surah -> openSurah(surah.nomor) }
        binding.rvSurah.layoutManager = LinearLayoutManager(this)
        binding.rvSurah.adapter = adapter
    }

    private fun setupJuzRecyclerView() {
        juzAdapter = JuzAdapter { juz -> openJuz(juz.nomor) }
        binding.rvJuz.layoutManager = LinearLayoutManager(this)
        binding.rvJuz.adapter = juzAdapter
    }

    /** Selector "Per Surat"/"Per Juz" di atas layar. Mode Juz sengaja menyembunyikan kotak
     * pencarian - pencarian surat/ayat yang ada sekarang berbasis nomor surah, tidak punya
     * makna yang jelas dalam konteks per-Juz, jadi daripada dipaksakan lebih baik disembunyikan
     * dulu untuk mode ini. */
    private fun setupBrowseModeToggle() {
        binding.tvModePerSurat.setOnClickListener { switchBrowseMode(BrowseMode.SURAT) }
        binding.tvModePerJuz.setOnClickListener { switchBrowseMode(BrowseMode.JUZ) }
    }

    private fun switchBrowseMode(mode: BrowseMode) {
        if (browseMode == mode) return
        browseMode = mode

        val activePill = if (mode == BrowseMode.SURAT) binding.tvModePerSurat else binding.tvModePerJuz
        val inactivePill = if (mode == BrowseMode.SURAT) binding.tvModePerJuz else binding.tvModePerSurat
        activePill.setBackgroundResource(R.drawable.bg_toggle_pill_active)
        activePill.setTextColor(ContextCompat.getColor(this, R.color.text_selected))
        inactivePill.background = null
        inactivePill.setTextColor(ContextCompat.getColor(this, R.color.text_unselected))

        when (mode) {
            BrowseMode.SURAT -> {
                binding.layoutSearch.visibility = View.VISIBLE
                binding.rvJuz.visibility = View.GONE
                onQueryChanged(currentQuery) // pulihkan tampilan daftar surat/hasil pencarian
            }
            BrowseMode.JUZ -> {
                binding.layoutSearch.visibility = View.GONE
                binding.swipeRefresh.visibility = View.GONE
                binding.rvSearchResults.visibility = View.GONE
                binding.tvSearchEmptyState.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.GONE
                binding.rvJuz.visibility = View.VISIBLE
                buildJuzSummaries()
            }
        }
    }

    private fun buildJuzSummaries() {
        if (allSurah.isEmpty()) return
        val summaries = (1..30).mapNotNull { nomorJuz ->
            JuzBoundaries.buildSummary(
                nomorJuz,
                namaLatinOf = { nomorSurah -> allSurah.find { it.nomor == nomorSurah }?.namaLatin ?: "Surat $nomorSurah" },
                namaArabOf = { nomorSurah -> allSurah.find { it.nomor == nomorSurah }?.nama.orEmpty() }
            )
        }
        juzAdapter.setData(summaries)
    }

    private fun openJuz(nomorJuz: Int) {
        val intent = Intent(this, DetailJuzActivity::class.java)
        intent.putExtra(DetailJuzActivity.EXTRA_NOMOR_JUZ, nomorJuz)
        startActivity(intent)
    }

    private fun setupSearchResultRecyclerView() {
        searchAdapter = SearchResultAdapter(
            onSurahClick = { surah -> openSurah(surah.nomor) },
            onAyatClick = { match -> openSurah(match.surah.number, match.numberInSurah) }
        )
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun openSurah(nomorSurah: Int, highlightAyat: Int? = null) {
        val intent = Intent(this, DetailSurahActivity::class.java)
        intent.putExtra(DetailSurahActivity.EXTRA_NOMOR_SURAH, nomorSurah)
        if (highlightAyat != null) {
            intent.putExtra(DetailSurahActivity.EXTRA_HIGHLIGHT_AYAT, highlightAyat)
        }
        startActivity(intent)
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onQueryChanged(s?.toString().orEmpty())
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun onQueryChanged(query: String) {
        currentQuery = query
        binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

        if (query.isBlank()) {
            binding.swipeRefresh.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
            binding.tvSearchEmptyState.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            return
        }

        binding.swipeRefresh.visibility = View.GONE
        viewModel.searchAyat(query)
        renderSearchResults()
    }

    private fun renderSearchResults() {
        val matchedSurah = allSurah.filter { surah ->
            surah.namaLatin.contains(currentQuery, ignoreCase = true) ||
                surah.nama.contains(currentQuery, ignoreCase = true) ||
                surah.arti.contains(currentQuery, ignoreCase = true)
        }

        val items = mutableListOf<SearchResultItem>()
        if (matchedSurah.isNotEmpty()) {
            items.add(SearchResultItem.Header("Surat (${matchedSurah.size})"))
            matchedSurah.forEach { items.add(SearchResultItem.SurahResult(it)) }
        }
        if (latestAyatResult.isNotEmpty()) {
            items.add(SearchResultItem.Header("Ayat (${latestAyatResult.size})"))
            latestAyatResult.forEach { match ->
                val namaSurah = viewModel.getCachedSurahName(match.surah.number) ?: "Surat ${match.surah.number}"
                items.add(SearchResultItem.AyatResult(match, namaSurah))
            }
        }

        searchAdapter.setData(items)
        binding.rvSearchResults.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        binding.progressBar.visibility = if (items.isEmpty() && isAyatSearchLoading) View.VISIBLE else View.GONE
        binding.tvSearchEmptyState.visibility =
            if (items.isEmpty() && !isAyatSearchLoading) View.VISIBLE else View.GONE
    }

    private fun setupObserver() {
        viewModel.surahList.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = false
                    val hasData = !resource.data.isNullOrEmpty()
                    binding.progressBar.visibility = if (hasData) View.GONE else View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmptyState.visibility = View.GONE
                    allSurah = resource.data ?: emptyList()
                    adapter.setData(allSurah)
                    if (currentQuery.isNotBlank()) renderSearchResults()
                    if (browseMode == BrowseMode.JUZ) buildJuzSummaries()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    if (resource.data.isNullOrEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    } else {
                        allSurah = resource.data
                        adapter.setData(allSurah)
                    }
                }
            }
        }

        viewModel.ayatSearchResult.observe(this) { resource ->
            if (currentQuery.length < DaftarSurahViewModel.MIN_AYAT_SEARCH_LENGTH) return@observe
            when (resource) {
                is Resource.Loading -> {
                    isAyatSearchLoading = true
                }
                is Resource.Success -> {
                    isAyatSearchLoading = false
                    latestAyatResult = resource.data ?: emptyList()
                    renderSearchResults()
                }
                is Resource.Error -> {
                    isAyatSearchLoading = false
                    latestAyatResult = emptyList()
                    renderSearchResults()
                }
            }
        }
    }
}
