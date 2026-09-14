package site.elahady.alkaukaba.viewmodel.doa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import site.elahady.alkaukaba.model.DoaCategoryDetail
import site.elahady.alkaukaba.repo.DoaRepository
import site.elahady.alkaukaba.utils.Resource

class DetailKategoriDoaViewModel : ViewModel() {

    private val _detail = MutableLiveData<Resource<DoaCategoryDetail>>()
    val detail: LiveData<Resource<DoaCategoryDetail>> = _detail

    fun fetchItems(slug: String) {
        _detail.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _detail.postValue(DoaRepository.getCategoryItems(slug))
        }
    }
}
