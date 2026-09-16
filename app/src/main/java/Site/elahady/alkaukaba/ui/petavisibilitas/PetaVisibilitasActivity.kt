package site.elahady.alkaukaba.ui.petavisibilitas

import site.elahady.alkaukaba.databinding.ActivityPetaVisibilitasBinding
import site.elahady.alkaukaba.model.WorldVisibilityResult
import site.elahady.alkaukaba.utils.applySystemBarInsetsPadding
import site.elahady.alkaukaba.utils.applyTopSystemBarInsetAsMargin
import site.elahady.alkaukaba.utils.applyStatusBarIconsForTheme
import site.elahady.alkaukaba.viewmodel.petavisibilitas.PetaVisibilitasViewModel
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider

class PetaVisibilitasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetaVisibilitasBinding
    private lateinit var viewModel: PetaVisibilitasViewModel

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

        viewModel.calculatePeta()
    }

    private fun renderResult(result: WorldVisibilityResult) {
        binding.tvBulanLabel.text = result.bulanHijriyahLabel
        binding.tvIjtimaLabel.text = "Ijtima': ${result.ghurubRefLabel}"
        binding.worldMapView.setData(result.points)
    }
}
