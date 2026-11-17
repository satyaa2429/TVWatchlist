package com.example.tvwatchlist.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_shows")
data class WatchlistShow(
    @field:PrimaryKey
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val rating: Double?,
    val summary: String?,
    val genres: String?,
    val language: String?,
    val premiered: String?,
    var status: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PLAN = "Plan to Watch"
        const val STATUS_WATCHING = "Watching"
        const val STATUS_COMPLETED = "Completed"
    }
}
