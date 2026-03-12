package site.elahady.alkaukaba.viewmodel.hilal

import Site.elahady.alkaukaba.model.HilalInput
import Site.elahady.alkaukaba.model.HilalResult
import Site.elahady.alkaukaba.utils.EphemerisCalculator
import Site.elahady.alkaukaba.utils.HilalPdfService
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

class HilalViewModel : ViewModel() {

    private val _calculationResult = MutableLiveData<HilalResult>()
    val calculationResult: LiveData<HilalResult> = _calculationResult

    // Fungsi Hitung
    fun calculateHilal(lat: Double, lng: Double, alt: String, ref: String, date: Calendar) {
        val altitudeVal = alt.toDoubleOrNull() ?: 10.0 // Default 10m
        val refractionVal = ref.toDoubleOrNull() ?: 0.034 // Default refraksi

        val input = HilalInput(lat, lng, altitudeVal, refractionVal, date)

        // Panggil Calculator
        val result = EphemerisCalculator.calculate(input)
        _calculationResult.value = result
    }

    fun generatePdf(context: Context) {
        _calculationResult.value?.let {
            HilalPdfService.generatePdf(context, it)
        }
    }
}