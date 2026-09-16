package site.elahady.alkaukaba.viewmodel.hisabnasional

import site.elahady.alkaukaba.model.MarkazHisabResult
import site.elahady.alkaukaba.utils.HisabNasionalCalculator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HisabNasionalViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _results = MutableLiveData<List<MarkazHisabResult>>()
    val results: LiveData<List<MarkazHisabResult>> = _results

    // Hisab dijalankan berkali-kali (satu per markaz terpilih), jadi tetap di background
    // thread walau tiap panggilan sendiri relatif cepat.
    fun calculateNasional(selectedIds: Set<String>, monthOffset: Int = 0) {
        _isLoading.value = true
        viewModelScope.launch {
            val calculated = withContext(Dispatchers.Default) {
                HisabNasionalCalculator.calculate(monthOffset, selectedIds)
            }
            _results.value = calculated
            _isLoading.value = false
        }
    }
}
