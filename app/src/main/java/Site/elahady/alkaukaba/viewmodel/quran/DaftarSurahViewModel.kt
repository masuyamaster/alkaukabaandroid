package site.elahady.alkaukaba.viewmodel.quran

import site.elahady.alkaukaba.model.AyatSearchMatch
import site.elahady.alkaukaba.model.Surah
import site.elahady.alkaukaba.repo.QuranRepository
import site.elahady.alkaukaba.utils.Resource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DaftarSurahViewModel : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_AYAT_SEARCH_LENGTH = 3
    }

    private val _surahList = MutableLiveData<Resource<List<Surah>>>()
    val surahList: LiveData<Resource<List<Surah>>> = _surahList

    private val _ayatSearchResult = MutableLiveData<Resource<List<AyatSearchMatch>>>()
    val ayatSearchResult: LiveData<Resource<List<AyatSearchMatch>>> = _ayatSearchResult

    private var searchJob: Job? = null

    init {
        fetchSurahList()
    }

    fun fetchSurahList() {
        _surahList.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _surahList.postValue(QuranRepository.getSurahList())
        }
    }

    /** Debounce 400ms supaya tidak nembak API tiap keystroke. Query di bawah
     * [MIN_AYAT_SEARCH_LENGTH] langsung dikosongkan tanpa panggil API sama sekali. */
    fun searchAyat(keyword: String) {
        searchJob?.cancel()
        val trimmed = keyword.trim()
        if (trimmed.length < MIN_AYAT_SEARCH_LENGTH) {
            _ayatSearchResult.value = Resource.Success(emptyList())
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(SEARCH_DEBOUNCE_MS)
            _ayatSearchResult.postValue(Resource.Loading())
            _ayatSearchResult.postValue(QuranRepository.searchAyat(trimmed))
        }
    }

    fun getCachedSurahName(nomor: Int): String? = QuranRepository.getCachedSurahName(nomor)
}
