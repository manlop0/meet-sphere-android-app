package com.example.meetsphere.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.R
import com.example.meetsphere.ui.navigation.Screen
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(
    navController: NavController,
    bottomBarNavController: NavController,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedActivity by viewModel.selectedActivity.collectAsState()
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.centerOnUserLocation()
                val location = uiState.cameraPosition
                navController.navigate(Screen.CreateActivity.createRoute(location))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Create activity")
            }
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets(0.dp)),
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        isTilesScaledToDpi = false
                        tilesScaleFactor = 1.0f
                        setZoomRounding(false)

                        minZoomLevel = 5.0
                        maxZoomLevel = 30.0

                        isVerticalMapRepetitionEnabled = false
                        isHorizontalMapRepetitionEnabled = false

                        if (isDarkTheme) {
                            overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                            overlayManager.tilesOverlay.loadingBackgroundColor = Color.BLACK
                        }

                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        val eventsReceiver =
                            object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    viewModel.onMapClick()
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?) = false
                            }
                        overlays.add(0, MapEventsOverlay(eventsReceiver))

                        val markerClusterer =
                            CustomMarkerClusterer(ctx).apply {
                                setRadius(100)

                                val clusterDrawable = ContextCompat.getDrawable(ctx, R.drawable.ic_cluster)
                                clusterDrawable?.let { drawable ->
                                    val clusterBitmap =
                                        when (drawable) {
                                            is BitmapDrawable -> drawable.bitmap
                                            else -> {
                                                val bitmap =
                                                    createBitmap(
                                                        drawable.intrinsicWidth.coerceAtLeast(1),
                                                        drawable.intrinsicHeight.coerceAtLeast(1),
                                                    )
                                                val canvas = Canvas(bitmap)
                                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                                drawable.draw(canvas)
                                                bitmap
                                            }
                                        }
                                    setIcon(clusterBitmap)
                                }
                            }
                        overlays.add(markerClusterer)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        overlays.add(locationOverlay)
                        locationOverlay.enableMyLocation()

                        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_user_map_location)
                        val bitmap: Bitmap? =
                            drawable?.let { d ->
                                when (d) {
                                    is BitmapDrawable -> d.bitmap
                                    else -> {
                                        val b =
                                            createBitmap(
                                                d.intrinsicWidth.coerceAtLeast(1),
                                                d.intrinsicHeight.coerceAtLeast(1),
                                            )
                                        val canvas = Canvas(b)
                                        d.setBounds(0, 0, canvas.width, canvas.height)
                                        d.draw(canvas)
                                        b
                                    }
                                }
                            }

                        bitmap?.let {
                            locationOverlay.setPersonIcon(it)
                            locationOverlay.setDirectionArrow(it, it)
                        }

                        locationOverlay.isDrawAccuracyEnabled = true

                        addMapListener(
                            object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    viewModel.onMapScrolled(
                                        center = mapCenter as GeoPoint,
                                        zoom = zoomLevelDouble,
                                    )
                                    return true
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean {
                                    viewModel.onMapScrolled(
                                        center = mapCenter as GeoPoint,
                                        zoom = zoomLevelDouble,
                                    )
                                    return true
                                }
                            },
                        )
                    }
                },
                update = { mapView ->
                    if (isDarkTheme) {
                        mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor = Color.BLACK
                    } else {
                        mapView.overlayManager.tilesOverlay.setColorFilter(null)
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor = Color.WHITE
                    }

                    mapView.controller.setZoom(uiState.zoomLevel)
                    mapView.controller.setCenter(uiState.cameraPosition)

                    mapView.overlays.removeAll { it is Marker }

                    val clusterOverlay =
                        mapView.overlays.firstOrNull {
                            it is RadiusMarkerClusterer
                        } as? RadiusMarkerClusterer

                    clusterOverlay?.items?.clear()

                    val currentUserId = uiState.currentUserId
                    val myActivities = uiState.activities.filter { it.creatorId == currentUserId }
                    val otherActivities = uiState.activities.filter { it.creatorId != currentUserId }

                    myActivities.forEach { activity ->
                        val marker =
                            Marker(mapView).apply {
                                position = activity.position
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = ContextCompat.getDrawable(context, R.drawable.ic_activity_pin_own)
                                relatedObject = activity

                                setOnMarkerClickListener { _, _ ->
                                    viewModel.onMarkerClick(activity)
                                    true
                                }
                            }
                        mapView.overlays.add(marker)
                    }

                    clusterOverlay?.let { clusterer ->
                        otherActivities.forEach { activity ->
                            val marker =
                                Marker(mapView).apply {
                                    position = activity.position
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = ContextCompat.getDrawable(context, R.drawable.ic_activity_pin_other)
                                    relatedObject = activity

                                    setOnMarkerClickListener { _, _ ->
                                        viewModel.onMarkerClick(activity)
                                        true
                                    }
                                }
                            clusterer.add(marker)
                        }

                        clusterer.items.forEach { marker ->
                            if (marker.relatedObject is ArrayList<*>) {
                                marker.setOnMarkerClickListener { clickedMarker, mv ->
                                    val currentZoom = mv.zoomLevelDouble
                                    val targetZoom = (currentZoom + 5.0).coerceAtMost(15.0)
                                    mv.controller.animateTo(clickedMarker.position, targetZoom, 500L)
                                    true
                                }
                            }
                        }

                        clusterer.invalidate()
                    }

                    mapView.invalidate()
                },
            )

            if (selectedActivity != null) {
                ActivityDetailsDialog(
                    marker = selectedActivity!!,
                    onDismissRequest = { viewModel.onMapClick() },
                    onMoreDetailsClick = { activityId ->
                        viewModel.onMapClick()
                        navController.navigate(Screen.ActivityDetails.createRoute(activityId))
                    },
                )
            }

            FloatingActionButton(
                onClick = { viewModel.centerOnUserLocation() },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = "My Location",
                )
            }
        }
    }
}

class CustomMarkerClusterer(
    ctx: Context,
) : RadiusMarkerClusterer(ctx) {
    override fun onSingleTapConfirmed(
        e: MotionEvent?,
        mapView: MapView?,
    ): Boolean {
        val clicked =
            mItems.firstOrNull { marker ->
                marker.hitTest(e, mapView)
            }

        if (clicked != null && mapView != null) {
            val clusterItems = clicked.relatedObject as? ArrayList<*>

            if (clusterItems != null && clusterItems.size > 1) {
                val currentZoom = mapView.zoomLevelDouble
                val targetZoom = (currentZoom + 5.0).coerceAtMost(15.0)

                mapView.controller.animateTo(clicked.position, targetZoom, 500L)
                return true
            }
        }

        return super.onSingleTapConfirmed(e, mapView)
    }
}
