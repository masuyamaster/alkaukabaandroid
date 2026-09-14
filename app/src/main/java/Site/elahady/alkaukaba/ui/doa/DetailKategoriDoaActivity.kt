package site.elahady.alkaukaba.ui.doa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import site.elahady.alkaukaba.adapter.DoaItemAdapter
import site.elahady.alkaukaba.databinding.ActivityDetailKategoriDoaBinding
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.doa.DetailKategoriDoaViewModel

class DetailKategoriDoaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SLUG = "extra_slug"
        const val EXTRA_NAMA = "extra_nama"
    }

    private lateinit var binding: ActivityDetailKategoriDoaBinding
    private lateinit var viewModel: DetailKategoriDoaViewModel
    private lateinit var adapter: DoaItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailKategoriDoaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        val slug = intent.getStringExtra(EXTRA_SLUG)
        val nama = intent.getStringExtra(EXTRA_NAMA) ?: "Doa & Dzikir"
        binding.includeToolbar.tvToolbarTitle.text = nama
        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[DetailKategoriDoaViewModel::class.java]

        adapter = DoaItemAdapter()
        binding.rvDoa.layoutManager = LinearLayoutManager(this)
        binding.rvDoa.adapter = adapter

        setupObserver()

        if (slug != null) {
            viewModel.fetchItems(slug)
        } else {
            binding.progressBar.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        }
    }

    private fun setupObserver() {
        viewModel.detail.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    val hasData = resource.data != null
                    binding.progressBar.visibility = if (hasData) View.GONE else View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    adapter.setData(resource.data?.items ?: emptyList())
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (resource.data == null) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                    } else {
                        adapter.setData(resource.data.items)
                    }
                }
            }
        }
    }
}
