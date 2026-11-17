package com.example.tvwatchlist.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tvwatchlist.ShowDetailsActivity
import com.example.tvwatchlist.databinding.FragmentSearchBinding
import com.example.tvwatchlist.ui.adapters.ShowSearchAdapter
import com.example.tvwatchlist.ui.viewmodels.MainViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ShowSearchAdapter
    private var cameraImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processPosterImage(it) }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { processPosterImage(it) }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to scan posters using camera",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ShowSearchAdapter { show ->
            context?.let { ctx ->
                ShowDetailsActivity.start(ctx, show.id)
            }
        }
        binding.rvSearchResults.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.btnScanPoster.setOnClickListener {
            showScanSourceDialog()
        }
    }

    private fun showScanSourceDialog() {
        val options = arrayOf("Take Photo with Camera", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Scan Poster")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> selectImageLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        val context = requireContext()
        val permissionState = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val ctx = context ?: return
        try {
            val photoFile = File(ctx.cacheDir, "scan_poster_${System.currentTimeMillis()}.jpg")
            val authority = "${ctx.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(ctx, authority, photoFile)
            cameraImageUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Failed to launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch() {
        val query = binding.etSearchQuery.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a show title to search", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.searchShows(query)
    }

    private fun observeViewModel() {
        viewModel.isLoadingSearch.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarSearch.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.tvSearchEmpty.visibility = View.GONE
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            result?.onSuccess { shows ->
                if (shows.isEmpty()) {
                    binding.tvSearchEmpty.text = "No shows found for your query."
                    binding.tvSearchEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                } else {
                    binding.tvSearchEmpty.visibility = View.GONE
                    adapter.submitList(shows)
                }
            }?.onFailure { exception ->
                binding.tvSearchEmpty.text = "Network error: ${exception.localizedMessage ?: "Failed to connect"}"
                binding.tvSearchEmpty.visibility = View.VISIBLE
                adapter.submitList(emptyList())
                Toast.makeText(requireContext(), "Error fetching shows", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processPosterImage(uri: Uri) {
        val context = context ?: return
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            binding.progressBarSearch.visibility = View.VISIBLE
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    binding.progressBarSearch.visibility = View.GONE
                    val lines = mutableListOf<String>()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            if (lineText.isNotEmpty()) {
                                lines.add(lineText)
                            }
                        }
                    }

                    if (lines.isEmpty()) {
                        Toast.makeText(context, "No text recognized in poster image", Toast.LENGTH_SHORT).show()
                    } else {
                        showRecognizedTextLinesDialog(lines)
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBarSearch.visibility = View.GONE
                    Toast.makeText(context, "Text recognition failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecognizedTextLinesDialog(lines: List<String>) {
        val items = lines.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Title from Poster")
            .setItems(items) { _, which ->
                val selectedText = lines[which]
                binding.etSearchQuery.setText(selectedText)
                performSearch()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
