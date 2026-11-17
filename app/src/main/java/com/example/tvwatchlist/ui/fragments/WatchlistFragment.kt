package com.example.tvwatchlist.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvwatchlist.R
import com.example.tvwatchlist.ShowDetailsActivity
import com.example.tvwatchlist.data.db.WatchlistShow
import com.example.tvwatchlist.databinding.FragmentWatchlistBinding
import com.example.tvwatchlist.ui.adapters.WatchlistAdapter
import com.example.tvwatchlist.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar

class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterChips()
        setupSwipeToDelete()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = WatchlistAdapter { show ->
            context?.let { ctx ->
                ShowDetailsActivity.start(ctx, show.id)
            }
        }
        binding.rvWatchlist.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWatchlist.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipPlan -> WatchlistShow.STATUS_PLAN
                R.id.chipWatching -> WatchlistShow.STATUS_WATCHING
                R.id.chipCompleted -> WatchlistShow.STATUS_COMPLETED
                else -> "All"
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val deletedShow = adapter.getShowAt(position) ?: return

                viewModel.deleteShow(deletedShow)

                Snackbar.make(
                    binding.root,
                    "Removed '${deletedShow.name}' from watchlist",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    viewModel.saveOrUpdateShow(deletedShow)
                }.show()
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvWatchlist)
    }

    private fun observeViewModel() {
        viewModel.watchlistShows.observe(viewLifecycleOwner) { shows ->
            if (shows.isNullOrEmpty()) {
                binding.tvWatchlistEmpty.visibility = View.VISIBLE
                adapter.submitList(emptyList())
            } else {
                binding.tvWatchlistEmpty.visibility = View.GONE
                adapter.submitList(shows)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
