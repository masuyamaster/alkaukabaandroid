package site.elahady.alkaukaba.viewmodel.jadwalimsakiyah

import site.elahady.alkaukaba.repo.PrayerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class JadwalImsakiyahViewModelFactory(private val repository: PrayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JadwalImsakiyahViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JadwalImsakiyahViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
