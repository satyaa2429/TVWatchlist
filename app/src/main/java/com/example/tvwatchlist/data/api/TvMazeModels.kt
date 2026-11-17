package com.example.tvwatchlist.data.api

import com.google.gson.annotations.SerializedName

data class TvMazeSearchResult(
    @field:SerializedName("score") val score: Double?,
    @field:SerializedName("show") val show: TvShow
)

data class TvShow(
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("name") val name: String?,
    @field:SerializedName("language") val language: String?,
    @field:SerializedName("genres") val genres: List<String>?,
    @field:SerializedName("premiered") val premiered: String?,
    @field:SerializedName("rating") val rating: ShowRating?,
    @field:SerializedName("image") val image: ShowImage?,
    @field:SerializedName("summary") val summary: String?
)

data class ShowRating(
    @field:SerializedName("average") val average: Double?
)

data class ShowImage(
    @field:SerializedName("medium") val medium: String?,
    @field:SerializedName("original") val original: String?
)
