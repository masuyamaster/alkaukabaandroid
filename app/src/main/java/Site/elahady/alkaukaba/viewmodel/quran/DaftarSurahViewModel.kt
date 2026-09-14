package site.elahady.alkaukaba.viewmodel.quran

import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.repo.QuranRepository
import site.elahady.alkaukaba.utils.Resource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DaftarSurahViewModel : ViewModel() {

    private val _surahList = MutableLiveData<Resource<List<Surah>>>()
    val surahList: LiveData<Resource<List<Surah>>> = _surahList

    init {
        fetchSurahList()
    }

    fun fetchSurahList() {
        _surahList.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _surahList.postValue(QuranRepository.getSurahList())
        }
    }
}
