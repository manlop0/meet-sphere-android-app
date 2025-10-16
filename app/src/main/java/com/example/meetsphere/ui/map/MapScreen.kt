package com.example.meetsphere.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.meetsphere.R
import com.example.meetsphere.ui.navigation.Screen
import kotlinx.coroutines.flow.collectLatest
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
import org.osmdroid.views.overlay.infowindow.InfoWindow
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

    val locationOverlay =
        remember {
            MyLocationNewOverlay(GpsMyLocationProvider(context), MapView(context)).apply {
                enableMyLocation()
            }
        }

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
                factory = {
                    MapView(it).apply {
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
                            overlayManager.tilesOverlay.loadingBackgroundColor = Color.Black.value.toInt()
                        }

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                        overlays.add(locationOverlay)
                        locationOverlay.enableMyLocation()

                        // Безопасная конверсия Drawable → Bitmap
                        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_user_map_location)
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

                        overlays.add(locationOverlay)

                        addMapListener(
                            object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    viewModel.onMapScrolled(
                                        center = this@apply.mapCenter as GeoPoint,
                                        zoom = this@apply.zoomLevelDouble,
                                    )
                                    return true
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean {
                                    viewModel.onMapScrolled(
                                        center = this@apply.mapCenter as GeoPoint,
                                        zoom = this@apply.zoomLevelDouble,
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
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor = Color.Black.value.toInt()
                    } else {
                        mapView.overlayManager.tilesOverlay.setColorFilter(null)
                        mapView.overlayManager.tilesOverlay.loadingBackgroundColor = Color.White.value.toInt()
                    }

                    mapView.controller.setZoom(uiState.zoomLevel)
                    mapView.controller.setCenter(uiState.cameraPosition)

                    mapView.overlays.removeAll { it is Marker }

                    uiState.activities.forEach { activity ->
                        val marker =
                            Marker(mapView).apply {
                                position = activity.position
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon =
                                    if (activity.creatorId == uiState.currentUserId) {
                                        ContextCompat.getDrawable(context, R.drawable.ic_activity_pin_own)
                                    } else {
                                        ContextCompat.getDrawable(context, R.drawable.ic_activity_pin_other)
                                    }
                                relatedObject = activity

                                setOnMarkerClickListener { _, _ ->
                                    viewModel.onMarkerClick(activity)
                                    true
                                }
                            }
                        mapView.overlays.add(marker)
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_my_location), contentDescription = "My Location")
            }
        }
    }
}
