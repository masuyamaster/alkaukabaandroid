package Site.elahady.alkaukaba.viewmodel.arahkiblat

import Site.elahady.alkaukaba.repo.arahkiblat.KiblatRepository
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class KiblatViewModel(private val repository: KiblatRepository) : ViewModel() {
    private val _qiblaAngle = MutableLiveData<Double>()
    val qiblaAngle: LiveData<Double> = _qiblaAngle

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchQiblaAngle(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val angle = repository.getQiblaAngle(lat, lon)
                _qiblaAngle.value = angle
            } catch (e: Exception) {
                _error.value = "Gagal mengambil arah kiblat"
            }
        }
    }
}