package site.elahady.alkaukaba.viewmodel.quran

import site.elahady.alkaukaba.model.SurahDetail
import site.elahady.alkaukaba.repo.QuranRepository
import site.elahady.alkaukaba.utils.Resource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailSurahViewModel : ViewModel() {

    private val _surahDetail = MutableLiveData<Resource<SurahDetail>>()
    val surahDetail: LiveData<Resource<SurahDetail>> = _surahDetail

    fun fetchSurahDetail(nomor: Int) {
        _surahDetail.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _surahDetail.postValue(QuranRepository.getSurahDetail(nomor))
        }
    }
}
