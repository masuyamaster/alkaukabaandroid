package site.elahady.alkaukaba.ui.petavisibilitas

import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.databinding.ActivityPetaVisibilitasBinding
import site.elahady.alkaukaba.model.WorldVisibilityResult
import site.elahady.alkaukaba.utils.HijriDateUtil
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.petavisibilitas.PetaVisibilitasViewModel
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import java.util.Calendar

class PetaVisibilitasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetaVisibilitasBinding
    private lateinit var viewModel: PetaVisibilitasViewModel

    // 0 = bulan terdekat ke depan dari sekarang (default), + = maju N bulan, - = mundur N bulan.
    // Diturunkan dari selisih pilihan spinnerBulan/TahunHijriyah terhadap baseline, sama pola
    // dengan AwalBulanActivity.
    private var currentMonthOffset = 0

    private var baselineHijriYear = 0
    private var baselineHijriMonth = 1
    private lateinit var hijriYearRange: IntRange

    // Sama seperti AwalBulanActivity: spinner otomatis fire onItemSelected sekali per spinner
    // begitu adapter/selection awal dipasang (bukan aksi user) -- counter ini menghitung mundur
    // 2 callback awal itu sebelum callback berikutnya dianggap pilihan user.
    private var pendingInitialSpinnerCallbacks = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetaVisibilitasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        viewModel = ViewModelProvider(this)[PetaVisibilitasViewModel::class.java]

        binding.includeToolbar.tvToolbarTitle.text = "Peta Visibilitas Hilal"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.result.observe(this) { result -> renderResult(result) }

        setupBulanSelectors()
        viewModel.calculatePeta(currentMonthOffset)
    }

    // Isi spinner bulan (12 nama Hijriyah) & tahun (baseline +-10 tahun) dengan default = bulan
    // terdekat ke depan dari sekarang (offset 0), lalu recalculate begitu user ganti salah satu.
    private fun setupBulanSelectors() {
        val (baseYear, baseMonth) = HijriDateUtil.nextMonthYearMonth(Calendar.getInstance())
        baselineHijriYear = baseYear
        baselineHijriMonth = baseMonth
        hijriYearRange = (baseYear - 10)..(baseYear + 10)

        val monthAdapter = ArrayAdapter(this, R.layout.item_spinner_selector, HijriDateUtil.monthNames)
        monthAdapter.setDropDownViewResource(R.layout.item_spinner_selector_dropdown)
        binding.spinnerBulanHijriyah.adapter = monthAdapter
        binding.spinnerBulanHijriyah.setSelection(baseMonth - 1)

        val yearLabels = hijriYearRange.map { "$it H" }
        val yearAdapter = ArrayAdapter(this, R.layout.item_spinner_selector, yearLabels)
        yearAdapter.setDropDownViewResource(R.layout.item_spinner_selector_dropdown)
        binding.spinnerTahunHijriyah.adapter = yearAdapter
        binding.spinnerTahunHijriyah.setSelection(hijriYearRange.indexOf(baseYear))

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (pendingInitialSpinnerCallbacks > 0) {
                    pendingInitialSpinnerCallbacks--
                    return
                }
                val selectedMonth = binding.spinnerBulanHijriyah.selectedItemPosition + 1
                val selectedYear = hijriYearRange.first + binding.spinnerTahunHijriyah.selectedItemPosition
                currentMonthOffset = (selectedYear * 12 + selectedMonth) - (baselineHijriYear * 12 + baselineHijriMonth)
                viewModel.calculatePeta(currentMonthOffset)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerBulanHijriyah.onItemSelectedListener = listener
        binding.spinnerTahunHijriyah.onItemSelectedListener = listener
    }

    private fun renderResult(result: WorldVisibilityResult) {
        binding.tvBulanLabel.text = result.bulanHijriyahLabel
        binding.tvIjtimaLabel.text = "Ijtima': ${result.ghurubRefLabel}"
        binding.worldMapView.setData(result.points)
    }
}
