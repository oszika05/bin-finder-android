package hu.bme.binfinder

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sucho.placepicker.AddressData
import com.sucho.placepicker.Constants
import com.sucho.placepicker.Constants.GOOGLE_API_KEY
import com.sucho.placepicker.MapType
import com.sucho.placepicker.PlacePicker
import hu.bme.binfinder.api.Bin
import hu.bme.binfinder.api.BinType
import hu.bme.binfinder.api.Bins
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job

    private lateinit var mMap: GoogleMap

    private val bins: Bins by lazy {
        Bins(intent?.extras?.getString(AUTH_TOKEN))
    }

    private val markers = mutableListOf<Marker>()

    private val activeFilters = mutableListOf<BinType>()
    private lateinit var types: List<BinType>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        job = Job()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val typeFilters = findViewById<ChipGroup>(R.id.type_filters)

        val tickIcon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_check_24);

        launch(Dispatchers.IO) {
            types = bins.getTypes()

            withContext(Dispatchers.Main) {
                for (type in types) {
                    val chip = Chip(this@MapsActivity)
                    chip.text = type.name
                    chip.chipIcon = tickIcon

                    chip.setOnClickListener {
                        if (activeFilters.contains(type)) {
                            chip.chipIcon = null
                            activeFilters.remove(type)
                        } else {
                            chip.chipIcon = tickIcon
                            activeFilters.add(type)
                        }

                        mMap.clear()
                        markers.clear()

                        getNewMarkers()
                    }

                    typeFilters.addView(chip)
                    activeFilters.add(type)
                }
            }
        }

        findViewById<FloatingActionButton>(R.id.addButton).setOnClickListener {
            addBin()
        }
    }

    private fun addBin() {
        mMap.cameraPosition.target
        val intent = PlacePicker.IntentBuilder()
            .setLatLong(
                mMap.cameraPosition.target.latitude,
                mMap.cameraPosition.target.longitude
            )
            .showLatLong(true)  // Show Coordinates in the Activity
            .setMapZoom(12.0f)  // Map Zoom Level. Default: 14.0
            .hideMarkerShadow(true) // Hides the shadow under the map marker. Default: False
            .setMarkerImageImageColor(R.color.colorPrimary)
//            .setFabColor(R.color.fabColor)
            .setMapType(MapType.NORMAL)
            .onlyCoordinates(true)  //Get only Coordinates from Place Picker
            .hideLocationButton(true)   //Hide Location Button (Default: false)
            .build(this)

        startActivityForResult(intent, PLACE_PICKER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val addressData =
                    data?.getParcelableExtra<AddressData>(Constants.ADDRESS_INTENT) ?: return


                val builder = AlertDialog.Builder(this)
                val dialog = builder
                    .setTitle(R.string.pick_type)
                    .setItems(
                        types.map { it.name }.toTypedArray()
                    ) { _, which ->

                        launch(Dispatchers.IO) {
                            bins.create(addressData.latitude, addressData.longitude, types[which])

                            withContext(Dispatchers.Main) {
                                getNewMarkers()
                            }
                        }
                    }.create()

                dialog.show()

                Log.d("PICKER", "success: $addressData")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        mMap.setOnInfoWindowClickListener { marker ->
            Log.d("CLICK", "Click: ${marker.tag}")
            val binId = marker.tag as Int

            launch(Dispatchers.IO) {
                bins.report(binId)

                withContext(Dispatchers.Main) {
                    marker.remove()
                    markers.remove(marker)
                    getNewMarkers()
                }
            }
        }

        mMap.setOnCameraIdleListener {
            Log.d("IDLE", "IDLE")
            getNewMarkers()
        }
    }

    private fun getNewMarkers() {
        val visibleRegion = mMap.projection.visibleRegion

        Log.d(
            "MAPS",
            "visible: ${visibleRegion.latLngBounds.southwest} ${visibleRegion.latLngBounds.northeast}"
        )

        launch(Dispatchers.IO) {

            val bins = bins.get(
                visibleRegion.latLngBounds.southwest,
                visibleRegion.latLngBounds.northeast,
                activeFilters
            )

            Log.d("MAPS", "bins: $bins")

            withContext(Dispatchers.Main) {

                val markersToRemove = markers.filter { marker ->
                    marker.position.latitude < visibleRegion.latLngBounds.southwest.latitude
                            || marker.position.longitude < visibleRegion.latLngBounds.southwest.longitude
                            || marker.position.latitude > visibleRegion.latLngBounds.northeast.latitude
                            || marker.position.longitude > visibleRegion.latLngBounds.northeast.longitude
                }

                for (marker in markersToRemove) {
                    marker.remove()
                }

                markers.removeAll(markersToRemove)

                val existingIds = markers.map { it.tag as Int }
                for (bin in bins) {
                    if (existingIds.contains(bin.id)) {
                        continue;
                    }

                    val position = LatLng(bin.lat, bin.long)
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title("Típus: ${bin.type.name}")
                        .snippet(getString(R.string.click_to_report))
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                if (bin.isReported) {
                                    BitmapDescriptorFactory.HUE_YELLOW
                                } else {
                                    BitmapDescriptorFactory.HUE_GREEN
                                }
                            )
                        )

                    val marker = mMap.addMarker(markerOptions)

                    markers.add(marker)

                    marker.tag = bin.id
                }
            }
        }
    }


    override fun onDestroy() {
        job.cancel()
        bins.close()

        super.onDestroy()
    }

    companion object {
        const val PLACE_PICKER_REQUEST = 202
        const val AUTH_TOKEN = "auth_token"
    }
}
