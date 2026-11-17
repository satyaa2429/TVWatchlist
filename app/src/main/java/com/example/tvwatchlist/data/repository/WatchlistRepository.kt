package com.example.tvwatchlist.data.repository

import androidx.lifecycle.LiveData
import com.example.tvwatchlist.data.api.RetrofitClient
import com.example.tvwatchlist.data.api.TvShow
import com.example.tvwatchlist.data.db.WatchlistDao
import com.example.tvwatchlist.data.db.WatchlistShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchlistRepository(private val dao: WatchlistDao) {

    suspend fun searchShows(query: String): Result<List<TvShow>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.searchShows(query)
            if (response.isSuccessful) {
                val results = response.body()?.map { it.show } ?: emptyList()
                Result.success(results)
            } else {
                Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShowDetails(id: Int): Result<TvShow> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.getShowDetails(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Show details not found (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllWatchlistShows(): LiveData<List<WatchlistShow>> = dao.getAllShows()

    fun getWatchlistShowsByStatus(status: String): LiveData<List<WatchlistShow>> =
        dao.getShowsByStatus(status)

    fun getWatchlistShowById(id: Int): LiveData<WatchlistShow?> = dao.getShowById(id)

    suspend fun getWatchlistShowByIdDirect(id: Int): WatchlistShow? = withContext(Dispatchers.IO) {
        dao.getShowByIdDirect(id)
    }

    suspend fun saveOrUpdateShow(show: WatchlistShow) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(show)
    }

    suspend fun deleteShow(show: WatchlistShow) = withContext(Dispatchers.IO) {
        dao.delete(show)
    }

    suspend fun deleteShowById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
}
