package site.elahady.alkaukaba.viewmodel.waktusholat

import site.elahady.alkaukaba.repo.PrayerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PrayerViewModelFactory(private val repository: PrayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrayerTimesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrayerTimesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}