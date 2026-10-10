@file:Suppress("UnstableApiUsage")
package com.amoledwatchfaces.solarpath.presentation

import android.Manifest
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.amoledwatchfaces.solarpath.BuildConfig
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.presentation.ui.BackgroundPermissionDialog
import com.amoledwatchfaces.solarpath.presentation.ui.ConfirmationOverlay
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import com.amoledwatchfaces.solarpath.utils.isPermissionGranted
import com.amoledwatchfaces.solarpath.utils.openPlayStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    transformationSpec: TransformationSpec,
    focusRequester: FocusRequester,
    listState: TransformingLazyColumnState,
    onEnableLocation: () -> Unit = {}
) {
    val preferences by viewModel.preferences.collectAsState()
    val solarData by viewModel.solarData.collectAsState()
    val isLoading by viewModel.loaderState.collectAsState()
    val isWatchFaceActive by viewModel.isWatchFaceActive.collectAsState()

    val context = LocalContext.current
    var showConfirmation by remember { mutableStateOf(false) }
    var confirmationState by remember { mutableStateOf(true) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", LocalLocale.current.platformLocale)
        .withZone(ZoneId.systemDefault())

    fun formatEpoch(epoch: Long): String {
        return if (epoch > 0) timeFormatter.format(Instant.ofEpochMilli(epoch)) else "- -"
    }

    ScreenScaffold(
        scrollState = listState
    ) { paddingValues ->
        TransformingLazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    RotaryScrollableDefaults.behavior(scrollableState = listState),
                    focusRequester = focusRequester
                ),
            state = listState,
        ) {

            // Next Event Header
            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline,
                            text = stringResource(R.string.next_event),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // Next Event / Current Solar Status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = solarData.nextEventName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = formatEpoch(solarData.nextEventEpoch),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        textAlign = TextAlign.Center,
                        text = "${String.format(LocalLocale.current.platformLocale, "%.1f", solarData.sunElevation)}° elevation • ${solarData.daylightDurationMinutes / 60}h ${solarData.daylightDurationMinutes % 60}m day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Push Watch Face Button
            item {
                Button(
                    colors = if (isWatchFaceActive) {
                        ButtonDefaults.filledTonalButtonColors()
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = { viewModel.pushDefaultWatchFace() },
                    icon = {
                        Icon(
                            imageVector = if (isWatchFaceActive) Icons.Default.Check else Icons.Default.WatchLater,
                            contentDescription = "Watch Face",
                            tint = if (isWatchFaceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    label = {
                        Text(
                            text = if (isWatchFaceActive) stringResource(R.string.watch_face_active) else stringResource(R.string.push_watch_face),
                            color = if (isWatchFaceActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }

            // FAQ Button
            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = { navController.navigate("faq") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.faq)
                        )
                    },
                    label = {
                        Text(text = stringResource(R.string.faq))
                    },
                    secondaryLabel = {
                        Text(text = stringResource(R.string.faq_subtitle))
                    }
                )
            }

            // Recalculate Button
            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = {
                        viewModel.recalculate()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_calculation),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = {
                        Text(text = stringResource(R.string.refresh_calculation))
                    }
                )
            }

            // Solar Times Breakdown
            item {
                ListSubHeader(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            text = stringResource(R.string.solar_times),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            item {
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {},
                    onClick = {},
                    subtitle = {
                        val currentTimeMillis = System.currentTimeMillis()
                        val solarEvents = listOf(
                            SolarTimeEntry(stringResource(R.string.astro_dawn), solarData.astroDawnEpoch, MaterialTheme.colorScheme.outlineVariant),
                            SolarTimeEntry(stringResource(R.string.nautical_dawn), solarData.nauticalDawnEpoch, MaterialTheme.colorScheme.outline),
                            SolarTimeEntry(stringResource(R.string.civil_dawn), solarData.civilDawnEpoch, MaterialTheme.colorScheme.onSurfaceVariant),
                            SolarTimeEntry(stringResource(R.string.sunrise), solarData.sunriseEpoch, MaterialTheme.colorScheme.primaryDim),
                            SolarTimeEntry(stringResource(R.string.solar_noon), solarData.solarNoonEpoch, MaterialTheme.colorScheme.tertiary),
                            SolarTimeEntry(stringResource(R.string.sunset), solarData.sunsetEpoch, MaterialTheme.colorScheme.primaryDim),
                            SolarTimeEntry(stringResource(R.string.civil_dusk), solarData.civilDuskEpoch, MaterialTheme.colorScheme.onSurfaceVariant),
                            SolarTimeEntry(stringResource(R.string.nautical_dusk), solarData.nauticalDuskEpoch, MaterialTheme.colorScheme.outline),
                            SolarTimeEntry(stringResource(R.string.astro_dusk), solarData.astroDuskEpoch, MaterialTheme.colorScheme.outlineVariant),
                            SolarTimeEntry(stringResource(R.string.solar_midnight), solarData.solarNadirEpoch, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        ).sortedBy { entry ->
                            if (entry.epoch <= 0L) Long.MAX_VALUE
                            else if (entry.epoch <= currentTimeMillis) entry.epoch + 86_400_000L
                            else entry.epoch
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            solarEvents.forEach { entry ->
                                SolarTimeRow(
                                    name = entry.name,
                                    time = formatEpoch(entry.epoch),
                                    color = entry.color
                                )
                            }
                        }
                    }
                )
            }

            // Location Header & Chip
            item {
                ListSubHeader(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            text = stringResource(R.string.location),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = { navController.navigate("location_choose") },
                    icon = {
                        Image(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
                        )
                    },
                    label = {
                        Text(
                            text = preferences.locationName,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    secondaryLabel = {
                        if (preferences.locationSubName.isNotEmpty()) {
                            Text(
                                text = preferences.locationSubName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            // Background Location Toggle
            item {
                SwitchButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    checked = preferences.backgroundLocationState,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (context.isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                viewModel.setBackgroundLocation(true)
                            } else {
                                viewModel.setBackgroundLocationDialogState(true)
                            }
                        } else {
                            viewModel.setBackgroundLocation(false)
                        }
                    },
                    label = { Text(stringResource(R.string.background_location)) },
                    secondaryLabel = { Text("${preferences.backgroundLocationRepeatInterval} min") }
                )
            }

            if (preferences.backgroundLocationState) {
                item {
                    Text(
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = "${stringResource(R.string.update_interval)}: ${preferences.backgroundLocationRepeatInterval} min"
                    )
                }

                item {
                    Slider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        value = preferences.backgroundLocationRepeatInterval.toInt(),
                        onValueChange = {
                            viewModel.setBackgroundLocationRepeatInterval(it.toLong())
                        },
                        valueProgression = IntProgression.fromClosedRange(30, 240, 15),
                        decreaseIcon = {
                            SliderDefaults.DecreaseIcon()
                        },
                        increaseIcon = {
                            SliderDefaults.IncreaseIcon()
                        }
                    )
                }
            }

            // About Section Header
            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            text = stringResource(R.string.about),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // App Version Button
            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = { context.openPlayStore() },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.version),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.version),
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(text = BuildConfig.VERSION_NAME)
                    }
                )
            }

            // Privacy Policy Button
            item {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = {
                        viewModel.openLinkOnPhone("https://amoledwatchfaces.github.io/apps/privacy/solarpath.html") { success ->
                            confirmationState = success
                            showConfirmation = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = stringResource(R.string.privacy_policy),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.privacy_policy),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            // amoledwatchfaces.com
            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            text = "amoledwatchfaces.com",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }

            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
        }

        ConfirmationOverlay(
            showConfirmation = showConfirmation,
            confirmationState = confirmationState,
            onTimeout = { showConfirmation = false }
        )

        val backgroundLocationDialogState by viewModel.backgroundLocationDialogState.collectAsState()
        if (backgroundLocationDialogState) {
            BackgroundPermissionDialog(
                viewModel = viewModel
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.18f)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        colors = ProgressIndicatorDefaults.colors(trackColor = Color.DarkGray),
                        strokeWidth = 4.dp,
                        gapSize = 6.dp
                    )
                }
            }
        }
    }
}

private data class SolarTimeEntry(
    val name: String,
    val epoch: Long,
    val color: Color
)

@Composable
fun SolarTimeRow(
    name: String,
    time: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}
