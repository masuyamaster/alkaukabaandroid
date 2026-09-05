package site.elahady.alkaukaba.ui.fasebulan

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.MoonQuarterInfo
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.nextMoonQuarter
import io.github.cosinekitty.astronomy.searchMoonQuarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.elahady.alkaukaba.databinding.ActivityFaseBulanBinding
import site.elahady.alkaukaba.utils.MoonPhaseLabel
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FaseBulanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaseBulanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaseBulanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        binding.includeToolbar.tvToolbarTitle.text = "Fase Bulan"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        showCurrentPhase()
        loadUpcomingQuarters()
    }

    private fun showCurrentPhase() {
        val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
        val synodicAngle = moonPhase(now)
        val illum = illumination(Body.Moon, now)
        binding.moonPhaseView.setPhase(synodicAngle, illum.phaseFraction)
        binding.tvMoonPhaseName.text = MoonPhaseLabel.forAngle(synodicAngle)
        binding.tvMoonIllumination.text = "%.0f%% permukaan tersinari".format(illum.phaseFraction * 100)
    }

    private fun loadUpcomingQuarters() {
        lifecycleScope.launch(Dispatchers.Default) {
            val now = Time.fromMillisecondsSince1970(System.currentTimeMillis())
            var mq = searchMoonQuarter(now)
            val quarters = mutableListOf(mq)
            repeat(3) {
                mq = nextMoonQuarter(mq)
                quarters.add(mq)
            }
            withContext(Dispatchers.Main) {
                bindQuarters(quarters)
            }
        }
    }

    private fun bindQuarters(quarters: List<MoonQuarterInfo>) {
        val format = SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale("in", "ID"))
        fun label(quarter: Int) = quarters.firstOrNull { it.quarter == quarter }
            ?.let { format.format(Date(it.time.toMillisecondsSince1970())) }
            ?: "-"

        binding.tvNextNewMoon.text = label(0)
        binding.tvNextFirstQuarter.text = label(1)
        binding.tvNextFullMoon.text = label(2)
        binding.tvNextLastQuarter.text = label(3)
    }
}
