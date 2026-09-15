package site.elahady.alkaukaba.ui.doa

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import site.elahady.alkaukaba.adapter.DoaCategoryAdapter
import site.elahady.alkaukaba.databinding.ActivityDaftarKategoriDoaBinding
import site.elahady.alkaukaba.model.DoaCategory
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.doa.DaftarKategoriDoaViewModel

class DaftarKategoriDoaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarKategoriDoaBinding
    private lateinit var viewModel: DaftarKategoriDoaViewModel
    private lateinit var adapter: DoaCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarKategoriDoaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Doa & Dzikir"
        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[DaftarKategoriDoaViewModel::class.java]

        adapter = DoaCategoryAdapter { category -> openKategori(category) }
        binding.rvKategori.layoutManager = LinearLayoutManager(this)
        binding.rvKategori.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchCategories()
        }

        setupObserver()
    }

    private fun openKategori(category: DoaCategory) {
        val intent = Intent(this, DetailKategoriDoaActivity::class.java)
        intent.putExtra(DetailKategoriDoaActivity.EXTRA_SLUG, category.slug)
        intent.putExtra(DetailKategoriDoaActivity.EXTRA_NAMA, category.name)
        startActivity(intent)
    }

    private fun setupObserver() {
        viewModel.categories.observe(this) { resource ->
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
