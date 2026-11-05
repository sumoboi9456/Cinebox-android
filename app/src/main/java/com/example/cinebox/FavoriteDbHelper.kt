package com.example.cinebox

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Lưu danh sách phim yêu thích trong máy bằng SQLite (chương 6)
class FavoriteDbHelper(context: Context) :
    SQLiteOpenHelper(context, "CineFavorites", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE Favorite(
                movieId INTEGER PRIMARY KEY,
                title TEXT,
                year INTEGER,
                genres TEXT,
                rating REAL,
                overview TEXT,
                posterUrl TEXT
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Favorite")
        onCreate(db)
    }

    // ===== INSERT (nếu phim đã có thì ghi đè) =====
    fun add(m: Movie) {
        val values = ContentValues().apply {
            put("movieId", m.id)
            put("title", m.title)
            put("year", m.year)
            put("genres", m.genres.joinToString("|"))
            put("rating", m.rating)
            put("overview", m.overview)
            put("posterUrl", m.posterUrl)
        }
        writableDatabase.insertWithOnConflict("Favorite", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ===== DELETE =====
    fun remove(id: Int) {
        writableDatabase.delete("Favorite", "movieId=?", arrayOf(id.toString()))
    }

    // ===== SELECT một phim: đã yêu thích chưa? =====
    fun isFavorite(id: Int): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT 1 FROM Favorite WHERE movieId=?", arrayOf(id.toString())
        )
        val found = cursor.moveToFirst()
        cursor.close()
        return found
    }

    // ===== SELECT tất cả =====
    fun getAll(): List<Movie> {
        val list = mutableListOf<Movie>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM Favorite ORDER BY rowid DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Movie(
                        id = cursor.getInt(0),
                        title = cursor.getString(1),
                        year = cursor.getInt(2),
                        genres = cursor.getString(3).split("|").filter { it.isNotEmpty() },
                        rating = cursor.getDouble(4),
                        overview = cursor.getString(5),
                        posterUrl = cursor.getString(6)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}