package com.example.tvwatchlist

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.tvwatchlist.data.api.TvShow
import com.example.tvwatchlist.data.db.WatchlistShow
import com.example.tvwatchlist.databinding.ActivityShowDetailsBinding
import com.example.tvwatchlist.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class ShowDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowDetailsBinding
    private val viewModel: MainViewModel by viewModels()

    private var showId: Int = -1
    private var currentTvShow: TvShow? = null
    private var existingWatchlistShow: WatchlistShow? = null

    companion object {
        private const val EXTRA_SHOW_ID = "extra_show_id"

        fun start(context: Context, showId: Int) {
            val intent = Intent(context, ShowDetailsActivity::class.java).apply {
                putExtra(EXTRA_SHOW_ID, showId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showId = intent.getIntExtra(EXTRA_SHOW_ID, -1)
        if (showId == -1) {
            Toast.makeText(this, "Invalid show ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        loadShowData()
    }

    private fun setupButtons() {
        binding.btnSaveWatchlist.setOnClickListener {
            saveOrUpdateStatus()
        }

        binding.btnRemoveWatchlist.setOnClickListener {
            viewModel.deleteShowById(showId)
            Toast.makeText(this, "Removed from Watchlist", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadShowData() {
        lifecycleScope.launch {
            // First check if already in local DB
            existingWatchlistShow = viewModel.getWatchlistShowByIdDirect(showId)

            existingWatchlistShow?.let { dbShow ->
                displayDbShow(dbShow)
            }

            // Also fetch from API to get full fresh details
            val result = viewModel.getShowDetailsApi(showId)
            result.onSuccess { tvShow ->
                currentTvShow = tvShow
                displayApiShow(tvShow)
            }.onFailure { error ->
                if (existingWatchlistShow == null) {
                    Toast.makeText(
                        this@ShowDetailsActivity,
                        "Failed to load show details: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun displayApiShow(show: TvShow) {
        binding.tvDetailTitle.text = show.name ?: "Unknown Show"
        binding.tvDetailLanguage.text = "Language: ${show.language ?: "N/A"}"
        binding.tvDetailPremiered.text = "Premiered: ${show.premiered ?: "N/A"}"

        val ratingText = show.rating?.average?.let { "⭐ %.1f".format(it) } ?: "⭐ N/A"
        binding.tvDetailRating.text = ratingText

        val genresText = show.genres?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "N/A"
        binding.tvDetailGenres.text = "Genres: $genresText"

        // Requirement 8: HTML tags removed from summary
        binding.tvDetailSummary.text = stripHtml(show.summary ?: "No summary available.")

        val posterUrl = show.image?.original ?: show.image?.medium
        Glide.with(this)
            .load(posterUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.ivDetailPoster)

        // Set radio status if already in DB
        existingWatchlistShow?.let { setRadioStatus(it.status) }
            ?: binding.rbPlan.setChecked(true)
    }

    private fun displayDbShow(show: WatchlistShow) {
        binding.tvDetailTitle.text = show.name
        binding.tvDetailLanguage.text = "Language: ${show.language ?: "N/A"}"
        binding.tvDetailPremiered.text = "Premiered: ${show.premiered ?: "N/A"}"

        val ratingText = show.rating?.let { "⭐ %.1f".format(it) } ?: "⭐ N/A"
        binding.tvDetailRating.text = ratingText

        binding.tvDetailGenres.text = "Genres: ${show.genres ?: "N/A"}"
        binding.tvDetailSummary.text = stripHtml(show.summary ?: "No summary available.")

        Glide.with(this)
            .load(show.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(binding.ivDetailPoster)

        setRadioStatus(show.status)
        binding.btnRemoveWatchlist.visibility = View.VISIBLE
        binding.btnSaveWatchlist.text = "Update Watchlist Status"
    }

    private fun setRadioStatus(status: String) {
        when (status) {
            WatchlistShow.STATUS_PLAN -> binding.rbPlan.isChecked = true
            WatchlistShow.STATUS_WATCHING -> binding.rbWatching.isChecked = true
            WatchlistShow.STATUS_COMPLETED -> binding.rbCompleted.isChecked = true
            else -> binding.rbPlan.isChecked = true
        }
    }

    private fun getSelectedStatus(): String {
        return when (binding.rgStatus.checkedRadioButtonId) {
            R.id.rbWatching -> WatchlistShow.STATUS_WATCHING
            R.id.rbCompleted -> WatchlistShow.STATUS_COMPLETED
            else -> WatchlistShow.STATUS_PLAN
        }
    }

    private fun saveOrUpdateStatus() {
        val selectedStatus = getSelectedStatus()

        val showToSave = if (currentTvShow != null) {
            val tvShow = currentTvShow!!
            WatchlistShow(
                id = tvShow.id,
                name = tvShow.name ?: "Unknown Show",
                imageUrl = tvShow.image?.original ?: tvShow.image?.medium,
                rating = tvShow.rating?.average,
                summary = tvShow.summary,
                genres = tvShow.genres?.joinToString(", "),
                language = tvShow.language,
                premiered = tvShow.premiered,
                status = selectedStatus
            )
        } else if (existingWatchlistShow != null) {
            existingWatchlistShow!!.copy(
                status = selectedStatus,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Toast.makeText(this, "Show data missing", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveOrUpdateShow(showToSave)
        Toast.makeText(this, "Saved to Watchlist as '$selectedStatus'", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun stripHtml(htmlText: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlText).toString().trim()
        }
    }
}
