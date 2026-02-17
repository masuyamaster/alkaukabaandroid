package site.elahady.alkaukaba.viewmodel.waktusholat

import PrayerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PrayerViewModelFactory(private val repository: PrayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrayerTimesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrayerTimesViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}