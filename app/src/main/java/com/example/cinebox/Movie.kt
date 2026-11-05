package com.example.cinebox

import org.json.JSONArray
import org.json.JSONObject

data class Movie(
    val id: Int,
    val title: String,
    val year: Int,
    val genres: List<String>,
    val rating: Double,
    val overview: String,
    val posterUrl: String
) {
    val genresText: String get() = genres.joinToString(", ")

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("year", year)
        put("genres", JSONArray(genres))
        put("rating", rating)
        put("overview", overview)
        put("posterUrl", posterUrl)
    }

    companion object {
        fun fromJson(o: JSONObject): Movie {
            val g = o.optJSONArray("genres") ?: JSONArray()
            val genres = (0 until g.length()).map { g.getString(it) }
            return Movie(
                id = o.getInt("id"),
                title = o.getString("title"),
                year = o.optInt("year"),
                genres = genres,
                rating = o.optDouble("rating", 0.0),
                overview = o.optString("overview"),
                posterUrl = o.optString("posterUrl")
            )
        }
    }
}