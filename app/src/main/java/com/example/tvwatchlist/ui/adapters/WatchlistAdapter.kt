package com.example.tvwatchlist.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvwatchlist.R
import com.example.tvwatchlist.data.db.WatchlistShow
import com.example.tvwatchlist.databinding.ItemWatchlistShowBinding

class WatchlistAdapter(
    private val onItemClick: (WatchlistShow) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.WatchlistViewHolder>() {

    private val shows = mutableListOf<WatchlistShow>()

    fun submitList(newShows: List<WatchlistShow>) {
        shows.clear()
        shows.addAll(newShows)
        notifyDataSetChanged()
    }

    fun getShowAt(position: Int): WatchlistShow? {
        return shows.getOrNull(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val binding = ItemWatchlistShowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WatchlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
        holder.bind(shows[position])
    }

    override fun getItemCount(): Int = shows.size

    inner class WatchlistViewHolder(private val binding: ItemWatchlistShowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(show: WatchlistShow) {
            val context = binding.root.context
            binding.tvWatchlistTitle.text = show.name

            val ratingText = show.rating?.let { "⭐ %.1f".format(it) } ?: "⭐ N/A"
            binding.tvWatchlistRating.text = ratingText

            binding.tvWatchlistGenres.text = show.genres ?: "N/A"

            binding.tvWatchlistStatus.text = show.status

            val statusColorRes = when (show.status) {
                WatchlistShow.STATUS_PLAN -> R.color.status_plan
                WatchlistShow.STATUS_WATCHING -> R.color.status_watching
                WatchlistShow.STATUS_COMPLETED -> R.color.status_completed
                else -> R.color.primary
            }
            binding.tvWatchlistStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, statusColorRes)
            )

            Glide.with(context)
                .load(show.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.ivWatchlistPoster)

            binding.root.setOnClickListener {
                onItemClick(show)
            }
        }
    }
}
