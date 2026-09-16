package site.elahady.alkaukaba.viewmodel.petavisibilitas

import site.elahady.alkaukaba.model.WorldVisibilityResult
import site.elahady.alkaukaba.utils.WorldVisibilityCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PetaVisibilitasViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _result = MutableLiveData<WorldVisibilityResult>()
    val result: LiveData<WorldVisibilityResult> = _result

    // Grid 240 titik (10 lintang x 24 bujur), tiap titik satu pemanggilan penuh
    // EphemerisCalculator — jelas lebih berat dari Hisab Nasional (maks 38 titik), jadi tetap
    // di background thread walau makan waktu lebih lama.
    fun calculatePeta(monthOffset: Int = 0) {
        _isLoading.value = true
        viewModelScope.launch {
            val calculated = withContext(Dispatchers.Default) {
                WorldVisibilityCalculator.calculate(monthOffset)
            }
            _result.value = calculated
            _isLoading.value = false
        }
    }
}
