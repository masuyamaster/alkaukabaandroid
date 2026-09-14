package site.elahady.alkaukaba.viewmodel.doa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import site.elahady.alkaukaba.model.DoaCategory
import site.elahady.alkaukaba.repo.DoaRepository
import site.elahady.alkaukaba.utils.Resource

class DaftarKategoriDoaViewModel : ViewModel() {

    private val _categories = MutableLiveData<Resource<List<DoaCategory>>>()
    val categories: LiveData<Resource<List<DoaCategory>>> = _categories

    init {
        fetchCategories()
    }

    fun fetchCategories() {
        _categories.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _categories.postValue(DoaRepository.getCategories())
        }
    }
}
