package site.elahady.alkaukaba.ui.hisabnasional

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.MarkazHisabAdapter
import site.elahady.alkaukaba.databinding.ActivityHisabNasionalBinding
import site.elahady.alkaukaba.model.MarkazHisabResult
import site.elahady.alkaukaba.utils.HisabNasionalCalculator
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.hisabnasional.HisabNasionalViewModel
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

class HisabNasionalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHisabNasionalBinding
    private lateinit var viewModel: HisabNasionalViewModel
    private lateinit var markazAdapter: MarkazHisabAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHisabNasionalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[HisabNasionalViewModel::class.java]

        setupUI()
        setupRecyclerView()
        setupObservers()
    }

    // onResume (bukan onCreate) supaya pilihan markaz yang baru disimpan dari
    // PilihMarkazActivity langsung terpakai begitu user kembali ke layar ini.
    override fun onResume() {
        super.onResume()
        val selectedIds = sessionManager.getSelectedMarkazNasionalIds() ?: HisabNasionalCalculator.defaultMarkazIds
        viewModel.calculateNasional(selectedIds)
    }

    private fun setupUI() {
        binding.includeToolbar.tvToolbarTitle.text = "Hisab Awal Bulan Nasional"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.includeToolbar.btnToolbarAction.apply {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_settings)
            setOnClickListener { startActivity(Intent(this@HisabNasionalActivity, PilihMarkazActivity::class.java)) }
        }
    }

    private fun setupRecyclerView() {
        markazAdapter = MarkazHisabAdapter()
        binding.rvMarkaz.apply {
            layoutManager = LinearLayoutManager(this@HisabNasionalActivity)
            adapter = markazAdapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.rvMarkaz.visibility = if (loading) View.GONE else View.VISIBLE
        }
        viewModel.results.observe(this) { results ->
            markazAdapter.setData(results)
            renderKesimpulan(results)
        }
    }

    private fun renderKesimpulan(results: List<MarkazHisabResult>) {
        if (results.isEmpty()) return

        binding.tvBulanLabel.text = results.first().hasil.bulanHijriyahLabel

        val memenuhi = results.count { it.hasil.hilalMemenuhiKriteria }
        val total = results.size
        binding.tvKesimpulanNasional.text =
            "$memenuhi dari $total titik markaz memenuhi kriteria Imkanu Rukyat (Neo-MABIMS)"

        val (badgeText, badgeBg, badgeColor) = when (memenuhi) {
            total -> Triple("Kriteria Terpenuhi Secara Nasional", R.drawable.bg_pill_green, R.color.pill_green_text)
            0 -> Triple("Kriteria Belum Terpenuhi di Semua Markaz", R.drawable.bg_pill_red, R.color.pill_red_text)
            else -> Triple("Kriteria Terpenuhi Sebagian Wilayah", R.drawable.bg_pill_navy_light, R.color.text_label_gold)
        }
        binding.tvKesimpulanBadge.text = badgeText
        binding.tvKesimpulanBadge.setBackgroundResource(badgeBg)
        binding.tvKesimpulanBadge.setTextColor(ContextCompat.getColor(this, badgeColor))
    }
}
