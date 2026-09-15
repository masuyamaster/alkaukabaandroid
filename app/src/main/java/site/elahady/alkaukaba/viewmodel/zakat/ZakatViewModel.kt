package site.elahady.alkaukaba.viewmodel.zakat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import site.elahady.alkaukaba.repo.zakat.GoldPriceRepository
import site.elahady.alkaukaba.utils.Resource

class ZakatViewModel : ViewModel() {

    private val _hargaEmas = MutableLiveData<Resource<Double>>()
    val hargaEmas: LiveData<Resource<Double>> = _hargaEmas

    init {
        muatHargaEmas()
    }

    fun muatHargaEmas() {
        _hargaEmas.value = Resource.Loading()
        viewModelScope.launch {
            _hargaEmas.value = GoldPriceRepository.getHargaEmasPerGram()
        }
    }
}
