package site.elahady.alkaukaba.ui.hisabnasional

import site.elahady.alkaukaba.adapter.MarkazCheckboxAdapter
import site.elahady.alkaukaba.databinding.ActivityPilihMarkazBinding
import site.elahady.alkaukaba.utils.HisabNasionalCalculator
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager

class PilihMarkazActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPilihMarkazBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MarkazCheckboxAdapter

    // Mutable & dipegang di sini (bukan di adapter) supaya tombol "Pilih Semua"/"Pakai
    // Default" bisa langsung memodifikasi lalu minta adapter refresh tampilan.
    private val selectedIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPilihMarkazBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        applyStatusBarIconsForTheme()
        binding.includeToolbar.toolbarDefault.applyTopSystemBarInsetAsMargin()
        binding.root.applySystemBarInsetsPadding(applyBottom = true)

        sessionManager = SessionManager(this)
        selectedIds.addAll(sessionManager.getSelectedMarkazNasionalIds() ?: HisabNasionalCalculator.defaultMarkazIds)

        binding.includeToolbar.tvToolbarTitle.text = "Pilih Markaz"
        binding.includeToolbar.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = MarkazCheckboxAdapter(HisabNasionalCalculator.allMarkaz, selectedIds) { _, _ -> }
        binding.rvMarkazCheckbox.layoutManager = LinearLayoutManager(this)
        binding.rvMarkazCheckbox.adapter = adapter

        binding.btnPilihSemua.setOnClickListener {
            selectedIds.clear()
            selectedIds.addAll(HisabNasionalCalculator.allMarkaz.map { it.id })
            adapter.notifyDataSetChanged()
        }
        binding.btnPakaiDefault.setOnClickListener {
            selectedIds.clear()
            selectedIds.addAll(HisabNasionalCalculator.defaultMarkazIds)
            adapter.notifyDataSetChanged()
        }
        binding.btnSimpan.setOnClickListener {
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 markaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sessionManager.setSelectedMarkazNasionalIds(selectedIds)
            finish()
        }
    }
}
