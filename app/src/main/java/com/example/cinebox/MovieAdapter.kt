package com.example.cinebox
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MovieAdapter(
    private val movies: List<Movie>,
    private val favDb: FavoriteDbHelper,
    private val onItemClick: (Movie) -> Unit,
    private val onFavoriteChanged: () -> Unit = {}
) : RecyclerView.Adapter<MovieAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgPoster: ImageView = v.findViewById(R.id.imgPoster)
        val txtTitle: TextView = v.findViewById(R.id.txtTitle)
        val txtMeta: TextView = v.findViewById(R.id.txtMeta)
        val txtRating: TextView = v.findViewById(R.id.txtRating)
        val btnFavorite: TextView = v.findViewById(R.id.btnFavorite)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return VH(v)
    }
    override fun getItemCount(): Int = movies.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = movies[position]
        holder.txtTitle.text = m.title
        holder.txtMeta.text = "${m.year} • ${m.genresText}"
        holder.txtRating.text = "★ ${m.rating}"
        ImageLoader.load(m.posterUrl, holder.imgPoster)
        holder.btnFavorite.text = if (favDb.isFavorite(m.id)) "♥" else "♡"
        holder.itemView.setOnClickListener { onItemClick(m) }
        holder.btnFavorite.setOnClickListener {
            if (favDb.isFavorite(m.id)) favDb.remove(m.id) else favDb.add(m)
            holder.btnFavorite.text = if (favDb.isFavorite(m.id)) "♥" else "♡"
            onFavoriteChanged()
        }
    }
}