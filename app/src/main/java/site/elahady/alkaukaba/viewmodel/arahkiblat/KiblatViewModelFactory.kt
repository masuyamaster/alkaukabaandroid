package site.elahady.alkaukaba.viewmodel.arahkiblat

import site.elahady.alkaukaba.api.RetrofitClient
import site.elahady.alkaukaba.repo.arahkiblat.KiblatRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class KiblatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val api = RetrofitClient.instance
        val repository = KiblatRepository(api)

        return KiblatViewModel(repository) as T
    }
}