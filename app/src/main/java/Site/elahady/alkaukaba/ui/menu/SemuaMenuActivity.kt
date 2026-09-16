package site.elahady.alkaukaba.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import site.elahady.alkaukaba.R
import site.elahady.alkaukaba.adapter.MenuGridAdapter
import site.elahady.alkaukaba.databinding.ActivitySemuaMenuBinding
import site.elahady.alkaukaba.model.MenuItem
import site.elahady.alkaukaba.ui.arahkiblat.KiblatActivity
import site.elahady.alkaukaba.ui.awalbulan.AwalBulanActivity
import site.elahady.alkaukaba.ui.doa.DaftarKategoriDoaActivity
import site.elahady.alkaukaba.ui.gerhana.GerhanaActivity
import site.elahady.alkaukaba.ui.hisabnasional.HisabNasionalActivity
import site.elahady.alkaukaba.ui.kalkulator.KalkulatorActivity
import site.elahady.alkaukaba.ui.masjidterdekat.MasjidTerdekatActivity
import site.elahady.alkaukaba.ui.okultasi.OkultasiActivity
import site.elahady.alkaukaba.ui.quran.DaftarSurahActivity
import site.elahady.alkaukaba.ui.tasbih.TasbihActivity
import site.elahady.alkaukaba.ui.waktusholat.WaktuSholatActivity
import site.elahady.alkaukaba.ui.zakat.ZakatActivity
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme

class SemuaMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySemuaMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySemuaMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Semua Menu"
        binding.includeToolbar.btnBack.setOnClickListener { finish() }

        val adapter = MenuGridAdapter(menuItems()) { item ->
            startActivity(Intent(this, item.activityClass))
        }
        binding.rvSemuaMenu.layoutManager = GridLayoutManager(this, 4)
        binding.rvSemuaMenu.adapter = adapter
    }

    private fun menuItems(): List<MenuItem> = listOf(
        MenuItem(R.drawable.ic_menu_compass, "Arah\nKiblat", R.color.bg_teal_light, R.color.icon_teal, KiblatActivity::class.java),
        MenuItem(R.drawable.ic_menu_pray, "Waktu\nSholat", R.color.bg_purple_light, R.color.icon_purple, WaktuSholatActivity::class.java),
        MenuItem(R.drawable.ic_menu_hilal, "Awal\nBulan", R.color.bg_yellow_light, R.color.icon_yellow, AwalBulanActivity::class.java),
        MenuItem(R.drawable.ic_menu_moon, "Gerhana", R.color.bg_blue_light, R.color.icon_blue, GerhanaActivity::class.java),
        MenuItem(R.drawable.ic_menu_star, "Okultasi", R.color.bg_orange_light, R.color.icon_orange, OkultasiActivity::class.java),
        MenuItem(R.drawable.ic_menu_document, "Hisab Awal\nBulan Nasional", R.color.bg_cyan_light, R.color.icon_cyan, HisabNasionalActivity::class.java),
        MenuItem(R.drawable.ic_menu_calculator, "Kalkulator", R.color.bg_indigo_light, R.color.icon_indigo, KalkulatorActivity::class.java),
        MenuItem(R.drawable.ic_menu_quran, "Al-Qur'an", R.color.bg_gold_light, R.color.text_label_gold, DaftarSurahActivity::class.java),
        MenuItem(R.drawable.ic_menu_tasbih, "Doa &\nDzikir", R.color.bg_green_light, R.color.icon_green, DaftarKategoriDoaActivity::class.java),
        MenuItem(R.drawable.ic_menu_mosque, "Masjid\nTerdekat", R.color.bg_sky_light, R.color.icon_sky, MasjidTerdekatActivity::class.java),
        MenuItem(R.drawable.ic_menu_tasbih, "Tasbih\nDigital", R.color.bg_rose_light, R.color.icon_rose, TasbihActivity::class.java),
        MenuItem(R.drawable.ic_menu_zakat, "Kalkulator\nZakat", R.color.bg_emerald_light, R.color.icon_emerald, ZakatActivity::class.java)
    )
}
