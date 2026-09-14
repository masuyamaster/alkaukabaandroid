package site.elahady.alkaukaba.ui.quran

import site.elahady.alkaukaba.adapter.SurahAdapter
import site.elahady.alkaukaba.databinding.ActivityDaftarSurahBinding
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.quran.DaftarSurahViewModel
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class DaftarSurahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarSurahBinding
    private lateinit var viewModel: DaftarSurahViewModel
    private lateinit var adapter: SurahAdapter

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
        setupObserver()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchSurahList()
        }
    }

    private fun setupRecyclerView() {
        adapter = SurahAdapter { surah ->
            val intent = Intent(this, DetailSurahActivity::class.java)
            intent.putExtra(DetailSurahActivity.EXTRA_NOMOR_SURAH, surah.nomor)
            startActivity(intent)
        }
        binding.rvSurah.layoutManager = LinearLayoutManager(this)
        binding.rvSurah.adapter = adapter
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
                    adapter.setData(resource.data ?: emptyList())
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    if (resource.data.isNullOrEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    } else {
                        adapter.setData(resource.data)
                    }
                }
            }
        }
    }
}
