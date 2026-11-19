package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.PlacePoint
import com.example.data.RouteResult
import com.example.ui.MapPickTarget
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
  mapCenter: GeoPoint,
  currentGpsPoint: PlacePoint?,
  fromPoint: PlacePoint?,
  toPoint: PlacePoint?,
  routeResult: RouteResult?,
  mapPickTarget: MapPickTarget,
  onMapTapped: (GeoPoint) -> Unit,
  onRecenterClick: () -> Unit,
  onTogglePickDestination: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val mapView =
    remember(context) {
      val osmConfig = Configuration.getInstance()
      osmConfig.userAgentValue = context.packageName
      val basePath = File(context.cacheDir, "osmdroid")
      osmConfig.osmdroidBasePath = basePath
      osmConfig.osmdroidTileCache = File(basePath, "tiles")

      MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(14.5)
        controller.setCenter(mapCenter)
      }
    }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onDetach()
    }
  }

  val routeColor = MaterialTheme.colorScheme.secondary.toArgb()
  val gpsDotDrawable = remember(context) { createPinDrawable(context, Color(0xFF2563EB), 18) }
  val fromPinDrawable = remember(context) { createPinDrawable(context, Color(0xFF0B875B), 24) }
  val toPinDrawable = remember(context) { createPinDrawable(context, Color(0xFFFFB703), 24) }

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(20.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    AndroidView(
      factory = { mapView },
      update = { view ->
        view.overlays.clear()

        val eventsOverlay =
          MapEventsOverlay(
            object : MapEventsReceiver {
              override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) onMapTapped(p)
                return mapPickTarget != MapPickTarget.NONE
              }

              override fun longPressHelper(p: GeoPoint?): Boolean = false
            }
          )
        view.overlays.add(eventsOverlay)

        // Route Polyline
        if (routeResult != null && routeResult.polylinePoints.size >= 2) {
          val polyline =
            Polyline(view).apply {
              setPoints(routeResult.polylinePoints)
              outlinePaint.color = routeColor
              outlinePaint.strokeWidth = 11f
              outlinePaint.isAntiAlias = true
            }
          view.overlays.add(polyline)
        }

        // Current Standing GPS marker
        if (currentGpsPoint != null) {
          val gpsMarker =
            Marker(view).apply {
              position = currentGpsPoint.toGeoPoint()
              title = "Standing Here"
              snippet = currentGpsPoint.shortName
              icon = gpsDotDrawable
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
          view.overlays.add(gpsMarker)
        }

        // From Marker
        if (fromPoint != null) {
          val fromMarker =
            Marker(view).apply {
              position = fromPoint.toGeoPoint()
              title = "From: ${fromPoint.shortName}"
              icon = fromPinDrawable
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
          view.overlays.add(fromMarker)
        }

        // To Marker
        if (toPoint != null) {
          val toMarker =
            Marker(view).apply {
              position = toPoint.toGeoPoint()
              title = "To: ${toPoint.shortName}"
              icon = toPinDrawable
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
          view.overlays.add(toMarker)
        }

        // Fit camera cleanly
        val pointsToFit =
          when {
            routeResult != null && routeResult.polylinePoints.size >= 2 -> routeResult.polylinePoints
            fromPoint != null && toPoint != null -> listOf(fromPoint.toGeoPoint(), toPoint.toGeoPoint())
            else -> emptyList()
          }

        if (pointsToFit.size >= 2) {
          val box = BoundingBox.fromGeoPointsSafe(pointsToFit)
          view.post {
            runCatching { view.zoomToBoundingBox(box, true, 90) }
          }
        } else {
          val targetCenter = toPoint?.toGeoPoint() ?: fromPoint?.toGeoPoint() ?: mapCenter
          view.controller.animateTo(targetCenter)
        }

        view.invalidate()
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Top-left status chip (Distance/Duration or Pick Mode Banner)
    if (mapPickTarget != MapPickTarget.NONE) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
      ) {
        Text(
          text =
            if (mapPickTarget == MapPickTarget.PICK_TO) {
              "Tap anywhere on map to set Destination"
            } else {
              "Tap anywhere on map to set Pickup"
            },
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    } else if (routeResult != null) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "%.2f km".format(routeResult.distanceKm),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
          )
          Text(
            text = "•  ~${routeResult.durationMinutes} min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Bottom-right Map Action Buttons
    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilledTonalIconButton(
        onClick = onTogglePickDestination,
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor =
              if (mapPickTarget == MapPickTarget.PICK_TO) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
              },
            contentColor =
              if (mapPickTarget == MapPickTarget.PICK_TO) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
          ),
        modifier = Modifier.size(48.dp).testTag("pick_destination_on_map_button"),
      ) {
        Icon(
          imageVector = Icons.Default.AddLocationAlt,
          contentDescription = "Pick destination on map",
        )
      }

      FilledTonalIconButton(
        onClick = onRecenterClick,
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.secondary,
          ),
        modifier = Modifier.size(48.dp).clip(CircleShape).testTag("recenter_gps_button"),
      ) {
        Icon(
          imageVector = Icons.Default.MyLocation,
          contentDescription = "Center map on current standing location",
        )
      }
    }
  }
}

private fun createPinDrawable(context: Context, color: Color, radiusDp: Int): Drawable {
  val density = context.resources.displayMetrics.density
  val sizePx = (radiusDp * 2 * density).toInt().coerceAtLeast(24)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val center = sizePx / 2f

  val borderPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = android.graphics.Color.WHITE
      style = Paint.Style.FILL
    }
  canvas.drawCircle(center, center, center * 0.86f, borderPaint)

  val fillPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      this.color = color.toArgb()
      style = Paint.Style.FILL
    }
  canvas.drawCircle(center, center, center * 0.64f, fillPaint)

  return BitmapDrawable(context.resources, bitmap)
}
