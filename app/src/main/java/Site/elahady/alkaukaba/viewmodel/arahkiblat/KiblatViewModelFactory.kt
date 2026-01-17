package Site.elahady.alkaukaba.viewmodel.arahkiblat

import Site.elahady.alkaukaba.api.AladhanApi
import Site.elahady.alkaukaba.api.RetrofitClient
import Site.elahady.alkaukaba.repo.arahkiblat.KiblatRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class KiblatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val api = RetrofitClient.instance
        val repository = KiblatRepository(api)

        return KiblatViewModel(repository) as T
    }
}