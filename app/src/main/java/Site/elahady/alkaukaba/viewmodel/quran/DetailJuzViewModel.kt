package site.elahady.alkaukaba.viewmodel.quran

import site.elahady.alkaukaba.model.JuzDetail
import site.elahady.alkaukaba.repo.QuranRepository
import site.elahady.alkaukaba.utils.Resource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailJuzViewModel : ViewModel() {

    private val _juzDetail = MutableLiveData<Resource<JuzDetail>>()
    val juzDetail: LiveData<Resource<JuzDetail>> = _juzDetail

    fun fetchJuzDetail(nomor: Int) {
        _juzDetail.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _juzDetail.postValue(QuranRepository.getJuzDetail(nomor))
        }
    }
}
