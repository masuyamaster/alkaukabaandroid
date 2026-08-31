package site.elahady.alkaukaba.viewmodel.gerhana

import site.elahady.alkaukaba.model.GerhanaResult
import site.elahady.alkaukaba.utils.EclipseCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GerhanaViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<GerhanaResult>()
    val result: LiveData<GerhanaResult> = _result

    // Pencarian gerhana Matahari lokal bisa perlu iterasi beberapa lunasi,
    // jadi dihitung di background thread supaya UI tidak nge-freeze.
    fun calculateEclipses(latitude: Double, longitude: Double, heightMeters: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            val calculated = withContext(Dispatchers.Default) {
                EclipseCalculator.calculate(latitude, longitude, heightMeters)
            }
            _result.value = calculated
            _isLoading.value = false
        }
    }
}
