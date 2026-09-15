package site.elahady.alkaukaba.viewmodel.masjidterdekat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import site.elahady.alkaukaba.model.NearbyMosque
import site.elahady.alkaukaba.repo.masjidterdekat.NearbyMosqueRepository
import site.elahady.alkaukaba.utils.Resource

class MasjidTerdekatViewModel : ViewModel() {

    private val _result = MutableLiveData<Resource<List<NearbyMosque>>>()
    val result: LiveData<Resource<List<NearbyMosque>>> = _result

    fun search(lat: Double, lon: Double, radiusMeters: Int) {
        _result.value = Resource.Loading()
        viewModelScope.launch {
            _result.value = NearbyMosqueRepository.findNearby(lat, lon, radiusMeters)
        }
    }
}
