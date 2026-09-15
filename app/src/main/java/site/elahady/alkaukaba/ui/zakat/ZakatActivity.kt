package site.elahady.alkaukaba.ui.zakat

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import site.elahady.alkaukaba.databinding.ActivityZakatBinding
import site.elahady.alkaukaba.utils.Resource
import site.elahady.alkaukaba.utils.ZakatCalculator
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.viewmodel.zakat.ZakatViewModel
import java.text.NumberFormat
import java.util.Locale

/** Kalkulator zakat fitrah, mal (harta), dan profesi - satu Activity dengan 3 tab (bukan
 * ViewPager2, cukup toggle visibility 3 blok konten karena semuanya statis/tidak butuh
 * lazy-load). Harga emas untuk nisab zakat mal & profesi diambil otomatis dari
 * [ZakatViewModel] (API logam-mulia-api, lihat docs/features/zakat.md), zakat fitrah tidak
 * butuh harga emas (murni input manual harga beras). */
class ZakatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityZakatBinding
    private lateinit var viewModel: ZakatViewModel

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private var hargaEmasPerGram: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityZakatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Kalkulator Zakat"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        viewModel = ViewModelProvider(this)[ZakatViewModel::class.java]

        setupTabs()
        setupHargaEmasRetry()
        setupObserver()
        setupFitrah()
        setupMal()
        setupProfesi()
    }

    private fun setupTabs() {
        binding.tabLayoutZakat.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.layoutFitrah.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                binding.layoutMal.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
                binding.layoutProfesi.visibility = if (tab.position == 2) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupHargaEmasRetry() {
        binding.includeHargaEmasMal.btnRetryHargaEmas.setOnClickListener { viewModel.muatHargaEmas() }
        binding.includeHargaEmasProfesi.btnRetryHargaEmas.setOnClickListener { viewModel.muatHargaEmas() }
    }

    private fun setupObserver() {
        viewModel.hargaEmas.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> renderHargaEmasState(isLoading = true, errorMessage = null, harga = null)
                is Resource.Success -> {
                    hargaEmasPerGram = resource.data
                    renderHargaEmasState(isLoading = false, errorMessage = null, harga = resource.data)
                }
                is Resource.Error -> {
                    hargaEmasPerGram = null
                    renderHargaEmasState(isLoading = false, errorMessage = resource.message, harga = null)
                }
            }
        }
    }

    private fun renderHargaEmasState(isLoading: Boolean, errorMessage: String?, harga: Double?) {
        listOf(binding.includeHargaEmasMal, binding.includeHargaEmasProfesi).forEach { view ->
            view.progressHargaEmas.visibility = if (isLoading) View.VISIBLE else View.GONE
            view.btnRetryHargaEmas.visibility = if (!isLoading && errorMessage != null) View.VISIBLE else View.GONE
            when {
                isLoading -> {
                    view.tvHargaEmas.text = "Memuat harga emas..."
                    view.tvNisabEmas.visibility = View.GONE
                }
                errorMessage != null -> {
                    view.tvHargaEmas.text = "Gagal memuat harga emas"
                    view.tvNisabEmas.visibility = View.GONE
                }
                harga != null -> {
                    view.tvHargaEmas.text = "${rupiahFormat.format(harga)} / gram"
                    view.tvNisabEmas.text = "Nisab 85 gr: ${rupiahFormat.format(harga * 85.0)}"
                    view.tvNisabEmas.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupFitrah() {
        binding.btnHitungFitrah.setOnClickListener {
            val jumlahJiwa = binding.etJumlahJiwa.text.toString().toIntOrNull()
            val hargaBeras = binding.etHargaBeras.text.toString().toDoubleOrNull()
            if (jumlahJiwa == null || jumlahJiwa <= 0) {
                Toast.makeText(this, "Isi jumlah jiwa terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (hargaBeras == null || hargaBeras <= 0) {
                Toast.makeText(this, "Isi harga beras per kg terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasil = ZakatCalculator.hitungFitrah(jumlahJiwa, hargaBeras)
            binding.tvHasilFitrahBeras.text =
                "Total beras: ${formatDesimal(hasil.totalBerasKg)} kg ($jumlahJiwa jiwa x 2.5 kg)"
            binding.tvHasilFitrahUang.text = rupiahFormat.format(hasil.totalUang)
            binding.cardHasilFitrah.visibility = View.VISIBLE
        }
    }

    private fun setupMal() {
        binding.btnHitungMal.setOnClickListener {
            val harga = hargaEmasPerGram
            if (harga == null) {
                Toast.makeText(this, "Harga emas belum termuat, coba lagi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val totalHarta = binding.etTotalHarta.text.toString().toDoubleOrNull()
            val totalHutang = binding.etTotalHutangMal.text.toString().toDoubleOrNull() ?: 0.0
            if (totalHarta == null || totalHarta <= 0) {
                Toast.makeText(this, "Isi total harta terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasil = ZakatCalculator.hitungMal(totalHarta, totalHutang, harga)
            binding.tvHasilMalNisab.text = "Nisab (85 gr emas): ${rupiahFormat.format(hasil.nisabRupiah)}"
            binding.tvHasilMalStatus.text = if (hasil.wajibZakat) {
                "Harta bersih ${rupiahFormat.format(hasil.hartaBersih)} sudah mencapai nisab - wajib zakat"
            } else {
                "Harta bersih ${rupiahFormat.format(hasil.hartaBersih)} belum mencapai nisab - belum wajib zakat"
            }
            binding.tvHasilMalJumlah.text = rupiahFormat.format(hasil.jumlahZakat)
            binding.cardHasilMal.visibility = View.VISIBLE
        }
    }

    private fun setupProfesi() {
        binding.btnHitungProfesi.setOnClickListener {
            val harga = hargaEmasPerGram
            if (harga == null) {
                Toast.makeText(this, "Harga emas belum termuat, coba lagi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val penghasilan = binding.etPenghasilanSetahun.text.toString().toDoubleOrNull()
            if (penghasilan == null || penghasilan <= 0) {
                Toast.makeText(this, "Isi total penghasilan setahun terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasil = ZakatCalculator.hitungProfesi(penghasilan, harga)
            binding.tvHasilProfesiNisab.text = "Nisab (85 gr emas): ${rupiahFormat.format(hasil.nisabRupiah)}"
            binding.tvHasilProfesiStatus.text = if (hasil.wajibZakat) {
                "Penghasilan ${rupiahFormat.format(penghasilan)} sudah mencapai nisab - wajib zakat"
            } else {
                "Penghasilan ${rupiahFormat.format(penghasilan)} belum mencapai nisab - belum wajib zakat"
            }
            binding.tvHasilProfesiJumlah.text = rupiahFormat.format(hasil.jumlahZakat)
            binding.cardHasilProfesi.visibility = View.VISIBLE
        }
    }

    private fun formatDesimal(value: Double): String {
        return if (value == Math.floor(value)) value.toLong().toString() else value.toString()
    }
}
