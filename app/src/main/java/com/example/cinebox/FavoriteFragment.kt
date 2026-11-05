package com.example.cinebox

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoriteFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var adapter: MovieAdapter
    private lateinit var favDb: FavoriteDbHelper
    private val movies = mutableListOf<Movie>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_favorite, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        txtEmpty = view.findViewById(R.id.txtEmpty)
        favDb = FavoriteDbHelper(requireContext())
        adapter = MovieAdapter(
            movies, favDb,
            { movie -> MovieDetailActivity.start(requireContext(), movie) },
            { reload() }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun reload() {
        movies.clear()
        movies.addAll(favDb.getAll())
        adapter.notifyDataSetChanged()
        txtEmpty.visibility = if (movies.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "FavoriteFragment - onResume")
        reload()
    }
}