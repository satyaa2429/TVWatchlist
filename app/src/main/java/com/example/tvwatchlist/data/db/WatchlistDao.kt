package com.example.tvwatchlist.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WatchlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(show: WatchlistShow)

    @Delete
    suspend fun delete(show: WatchlistShow)

    @Query("DELETE FROM watchlist_shows WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("SELECT * FROM watchlist_shows ORDER BY updatedAt DESC")
    fun getAllShows(): LiveData<List<WatchlistShow>>

    @Query("SELECT * FROM watchlist_shows WHERE status = :status ORDER BY updatedAt DESC")
    fun getShowsByStatus(status: String): LiveData<List<WatchlistShow>>

    @Query("SELECT * FROM watchlist_shows WHERE id = :id LIMIT 1")
    fun getShowById(id: Int): LiveData<WatchlistShow?>

    @Query("SELECT * FROM watchlist_shows WHERE id = :id LIMIT 1")
    suspend fun getShowByIdDirect(id: Int): WatchlistShow?
}
