package site.elahady.alkaukaba.viewmodel.okultasi

import site.elahady.alkaukaba.model.OccultationResult
import site.elahady.alkaukaba.utils.OccultationCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OkultasiViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<OccultationResult>()
    val result: LiveData<OccultationResult> = _result

    // Pencarian konjungsi & kontak okultasi butuh banyak iterasi, jadi dihitung
    // di background thread supaya UI tidak nge-freeze.
    fun calculateOccultations(latitude: Double, longitude: Double, heightMeters: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            val calculated = withContext(Dispatchers.Default) {
                OccultationCalculator.calculate(latitude, longitude, heightMeters)
            }
            _result.value = calculated
            _isLoading.value = false
        }
    }
}
