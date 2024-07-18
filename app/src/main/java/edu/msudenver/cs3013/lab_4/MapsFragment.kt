package edu.msudenver.cs3013.lab_4

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import edu.msudenver.cs3013.lab_4.databinding.FragmentMapsBinding

class MapsFragment : Fragment(), OnMapReadyCallback {

    // Declare variables for Google Map, view binding, and location services
    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var carMarker: Marker? = null // Variable to keep track of the car marker
    private lateinit var viewModel: SharedViewModel

    // Define the initial location (Denver, Colorado)
    private val denverLocation = LatLng(39.7392, -104.9903)

    // Initialize the FusedLocationProviderClient
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    // Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Set up the map and button click listener
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up click listener for the "I'm Parked Here" button
        binding.btnParkedHere.setOnClickListener {
            mMap.cameraPosition.target.let { location ->
                addOrMoveCarMarker(location)
                viewModel.updateParkingLocation("${location.latitude}, ${location.longitude}")
            }
        }

        // Set up the permission request launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocation()
            } else {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    // Callback for when the map is ready to be used
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            // Set up map click listener to add or move car marker
            setOnMapClickListener { latLng ->
                addOrMoveCarMarker(latLng)
            }
        }
        // Start with Denver location
        updateMapLocation(denverLocation)
        addMarkerAtLocation(denverLocation, "Denver, Colorado")

        // Check location permissions and request if necessary
        when {
            hasLocationPermission() -> getLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    // Get the user's last known location and update the map
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (hasLocationPermission()) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    updateMapLocation(currentLocation)
                    addMarkerAtLocation(currentLocation, "Current Location")
                }
            }
        }
    }

    // Update the map's camera position
    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }

    // Add a generic marker to the map
    private fun addMarkerAtLocation(location: LatLng, title: String, markerIcon: BitmapDescriptor? = null) =
        mMap.addMarker(
            MarkerOptions().title(title).position(location)
                .apply { markerIcon?.let { icon(markerIcon) } }
        )

    // Add a new car marker or move the existing one
    private fun addOrMoveCarMarker(latLng: LatLng) {
        if (carMarker == null) {
            // If there's no car marker, create a new one
            carMarker = addMarkerAtLocation(
                latLng,
                "Parked Car",
                getBitmapDescriptorFromVector(R.drawable.car_icon)
            )
        } else {
            // If a car marker exists, update its position
            carMarker?.position = latLng
        }
        // Update the map view to center on the new marker position
        updateMapLocation(latLng)
    }

    // Convert a vector drawable to a BitmapDescriptor for use as a marker icon
    private fun getBitmapDescriptorFromVector(@DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor? {
        val bitmap = ContextCompat.getDrawable(requireContext(), vectorDrawableResourceId)?.let { vectorDrawable ->
            vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
            val drawableWithTint = DrawableCompat.wrap(vectorDrawable)
            DrawableCompat.setTint(drawableWithTint, Color.RED)
            val bitmap = Bitmap.createBitmap(
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawableWithTint.draw(canvas)
            bitmap
        } ?: return null
        return BitmapDescriptorFactory.fromBitmap(bitmap).also { bitmap.recycle() }
    }

    // Check if the app has location permission
    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Show a dialog explaining why the app needs location permission
    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Location permission")
            .setMessage("We need your permission to find your current position")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }
}