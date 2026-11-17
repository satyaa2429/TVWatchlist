package com.example.tvwatchlist.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvMazeApiService {

    @GET("search/shows")
    suspend fun searchShows(
        @Query("q") query: String
    ): Response<List<TvMazeSearchResult>>

    @GET("shows/{id}")
    suspend fun getShowDetails(
        @Path("id") id: Int
    ): Response<TvShow>
}
