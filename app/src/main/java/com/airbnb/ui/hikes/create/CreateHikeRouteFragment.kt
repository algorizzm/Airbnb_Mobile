package com.airbnb.ui.hikes.create

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateHikeRouteBinding
import com.airbnb.utils.HikeDifficulty
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.google.android.gms.tasks.CancellationTokenSource

class CreateHikeRouteFragment : Fragment(R.layout.fragment_create_hike_route) {

    private var _binding: FragmentCreateHikeRouteBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                fetchCurrentLocation()
            }
        }

    private var elevation = 0
    private var distance = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreateHikeRouteBinding.bind(view)

        setupInputs()
        setupDifficultyChips()
        setupLocationButton()
        setupIncrementButtons()
        observeUi()
    }

    private fun setupInputs() {

        binding.etMeetup.doAfterTextChanged {
            flowVm.updateMeetupPoint(it?.toString().orEmpty())
        }

        binding.etDestination.doAfterTextChanged {
            flowVm.updateDestination(it?.toString().orEmpty())
        }
    }

    private fun setupIncrementButtons() {

        // ELEVATION
        binding.btnElevationIncrement.setOnClickListener {

            elevation += 50

            binding.tvElevationValue.text = elevation.toString()

            flowVm.updateElevationMText(
                elevation.toString()
            )
        }

        binding.btnElevationDecrement.setOnClickListener {

            if (elevation > 0) {
                elevation -= 50
            }

            binding.tvElevationValue.text = elevation.toString()

            flowVm.updateElevationMText(
                elevation.toString()
            )
        }

        // DISTANCE
        binding.btnDistanceIncrement.setOnClickListener {

            distance += 1

            binding.tvDistanceValue.text = distance.toString()

            flowVm.updateEstimatedDistanceKmText(
                distance.toString()
            )
        }

        binding.btnDistanceDecrement.setOnClickListener {

            if (distance > 0) {
                distance -= 1
            }

            binding.tvDistanceValue.text = distance.toString()

            flowVm.updateEstimatedDistanceKmText(
                distance.toString()
            )
        }
    }

    private fun setupDifficultyChips() {

        binding.chipGroupDifficulty.setOnCheckedStateChangeListener { _, checkedIds ->

            val difficulty = when (checkedIds.firstOrNull()) {

                R.id.chipBeginner -> HikeDifficulty.BEGINNER

                R.id.chipIntermediate -> HikeDifficulty.INTERMEDIATE

                R.id.chipAdvanced -> HikeDifficulty.ADVANCED

                else -> HikeDifficulty.BEGINNER
            }

            flowVm.updateRouteDifficulty(difficulty)
        }
    }

    private fun setupLocationButton() {

        binding.btnUseCurrentLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {

        when {

            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {

                fetchCurrentLocation()
            }

            else -> {

                locationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    private fun fetchCurrentLocation() {

        // REQUIRED explicit permission check
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val cancellationTokenSource =
                    CancellationTokenSource()

                val location = fusedLocationClient
                    .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                    .await()

                location?.let {

                    val address = reverseGeocode(
                        it.latitude,
                        it.longitude
                    )

                    flowVm.updateMeetupPoint(address)
                }

            } catch (e: SecurityException) {

                e.printStackTrace()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    private fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): String {

        return try {

            val geocoder = Geocoder(
                requireContext(),
                Locale.getDefault()
            )

            val addresses = geocoder.getFromLocation(
                latitude,
                longitude,
                1
            )

            addresses
                ?.firstOrNull()
                ?.getAddressLine(0)
                ?: "$latitude, $longitude"

        } catch (e: Exception) {

            "$latitude, $longitude"
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                flowVm.ui.collect { state ->

                    if (
                        binding.etMeetup.text?.toString()
                        != state.meetupPoint
                    ) {
                        binding.etMeetup.setText(state.meetupPoint)
                    }

                    if (
                        binding.etDestination.text?.toString()
                        != state.destination
                    ) {
                        binding.etDestination.setText(state.destination)
                    }

                    // ELEVATION
                    elevation =
                        state.elevationMText.toIntOrNull() ?: 0

                    binding.tvElevationValue.text =
                        elevation.toString()

                    // DISTANCE
                    distance =
                        state.estimatedDistanceKmText.toIntOrNull() ?: 0

                    binding.tvDistanceValue.text =
                        distance.toString()

                    // FLOATING CARD TITLE
                    binding.layoutFloatingCard
                        .tvFloatingHikeTitle
                        .text =
                        state.title.ifBlank {
                            "Untitled Hike"
                        }

                    val chipId = when (state.routeDifficulty) {

                        HikeDifficulty.BEGINNER ->
                            R.id.chipBeginner

                        HikeDifficulty.INTERMEDIATE ->
                            R.id.chipIntermediate

                        HikeDifficulty.ADVANCED ->
                            R.id.chipAdvanced

                        else ->
                            R.id.chipBeginner
                    }

                    if (
                        binding.chipGroupDifficulty.checkedChipId
                        != chipId
                    ) {
                        binding.chipGroupDifficulty.check(chipId)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}