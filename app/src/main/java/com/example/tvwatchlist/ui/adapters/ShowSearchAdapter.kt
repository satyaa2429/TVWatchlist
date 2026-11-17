package com.example.tvwatchlist.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvwatchlist.R
import com.example.tvwatchlist.data.api.TvShow
import com.example.tvwatchlist.databinding.ItemShowGridBinding

class ShowSearchAdapter(
    private val onItemClick: (TvShow) -> Unit
) : RecyclerView.Adapter<ShowSearchAdapter.ShowViewHolder>() {

    private val shows = mutableListOf<TvShow>()

    fun submitList(newShows: List<TvShow>) {
        shows.clear()
        shows.addAll(newShows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowViewHolder {
        val binding = ItemShowGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShowViewHolder, position: Int) {
        holder.bind(shows[position])
    }

    override fun getItemCount(): Int = shows.size

    inner class ShowViewHolder(private val binding: ItemShowGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(show: TvShow) {
            binding.tvShowTitle.text = show.name ?: "Unknown Title"

            val ratingText = show.rating?.average?.let { "⭐ %.1f".format(it) } ?: "⭐ N/A"
            binding.tvRating.text = ratingText

            val genresText = show.genres?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "N/A"
            binding.tvGenres.text = genresText

            val posterUrl = show.image?.medium ?: show.image?.original
            Glide.with(binding.root.context)
                .load(posterUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.ivPoster)

            binding.root.setOnClickListener {
                onItemClick(show)
            }
        }
    }
}
