package site.elahady.alkaukaba.viewmodel.hilal

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import site.elahady.alkaukaba.utils.EphemerisCalculator
import site.elahady.alkaukaba.utils.HilalPdfService
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HilalViewModel : ViewModel() {

    private val _calculationResult = MutableLiveData<HilalResult>()
    val calculationResult: LiveData<HilalResult> = _calculationResult

    fun calculateHilal(lat: Double, lng: Double, heightMeters: Double) {
        val input = HilalInput(lat, lng, heightMeters)
        _calculationResult.value = EphemerisCalculator.calculate(input)
    }

    fun generatePdf(context: Context) {
        _calculationResult.value?.let {
            HilalPdfService.generatePdf(context, it)
        }
    }
}
