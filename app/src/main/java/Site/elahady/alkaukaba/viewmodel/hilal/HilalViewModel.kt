package site.elahady.alkaukaba.viewmodel.hilal

import site.elahady.alkaukaba.model.HilalInput
import site.elahady.alkaukaba.model.HilalResult
import site.elahady.alkaukaba.utils.EphemerisCalculator
import site.elahady.alkaukaba.utils.HilalPdfService
import site.elahady.alkaukaba.utils.SessionManager
import site.elahady.alkaukaba.utils.addurrulaniq.AdDurrulAniqCalculator
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HilalViewModel : ViewModel() {

    private val _calculationResult = MutableLiveData<HilalResult>()
    val calculationResult: LiveData<HilalResult> = _calculationResult

    /** [method] salah satu [SessionManager.HISAB_AWAL_BULAN_ASTRONOMY_ENGINE]/[SessionManager.HISAB_AWAL_BULAN_DURRUL_ANIQ]. */
    fun calculateHilal(
        lat: Double,
        lng: Double,
        heightMeters: Double,
        method: String = SessionManager.HISAB_AWAL_BULAN_ASTRONOMY_ENGINE
    ) {
        val input = HilalInput(lat, lng, heightMeters)
        _calculationResult.value = if (method == SessionManager.HISAB_AWAL_BULAN_DURRUL_ANIQ) {
            AdDurrulAniqCalculator.calculate(input)
        } else {
            EphemerisCalculator.calculate(input)
        }
    }

    fun generatePdf(context: Context) {
        _calculationResult.value?.let {
            HilalPdfService.generatePdf(context, it)
        }
    }
}
