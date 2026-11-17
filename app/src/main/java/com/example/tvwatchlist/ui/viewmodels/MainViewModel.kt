package com.example.tvwatchlist.ui.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.tvwatchlist.data.api.TvShow
import com.example.tvwatchlist.data.db.AppDatabase
import com.example.tvwatchlist.data.db.WatchlistShow
import com.example.tvwatchlist.data.repository.WatchlistRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WatchlistRepository

    val searchResults = MutableLiveData<Result<List<TvShow>>?>()
    val isLoadingSearch = MutableLiveData<Boolean>(false)

    val currentFilter = MutableLiveData<String>("All")

    val watchlistShows: LiveData<List<WatchlistShow>>

    init {
        val dao = AppDatabase.getInstance(application).watchlistDao()
        repository = WatchlistRepository(dao)

        watchlistShows = currentFilter.switchMap { filter ->
            if (filter == "All") {
                repository.getAllWatchlistShows()
            } else {
                repository.getWatchlistShowsByStatus(filter)
            }
        }
    }

    fun searchShows(query: String) {
        if (query.isBlank()) return

        isLoadingSearch.value = true
        viewModelScope.launch {
            val result = repository.searchShows(query)
            searchResults.value = result
            isLoadingSearch.value = false
        }
    }

    fun setFilter(filter: String) {
        currentFilter.value = filter
    }

    fun deleteShow(show: WatchlistShow) {
        viewModelScope.launch {
            repository.deleteShow(show)
        }
    }

    fun saveOrUpdateShow(show: WatchlistShow) {
        viewModelScope.launch {
            repository.saveOrUpdateShow(show)
        }
    }

    suspend fun getWatchlistShowByIdDirect(id: Int): WatchlistShow? {
        return repository.getWatchlistShowByIdDirect(id)
    }

    suspend fun getShowDetailsApi(id: Int): Result<TvShow> {
        return repository.getShowDetails(id)
    }

    fun deleteShowById(id: Int) {
        viewModelScope.launch {
            repository.deleteShowById(id)
        }
    }
}
