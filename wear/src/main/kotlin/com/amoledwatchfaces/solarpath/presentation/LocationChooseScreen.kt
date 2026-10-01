@file:Suppress("UnstableApiUsage")
package com.amoledwatchfaces.solarpath.presentation

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.presentation.ui.LocationChip
import com.amoledwatchfaces.solarpath.presentation.ui.TextInputDialog
import com.amoledwatchfaces.solarpath.presentation.ui.locationsListItems
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import com.amoledwatchfaces.solarpath.utils.isLocationEnabled
import com.amoledwatchfaces.solarpath.utils.isOnline

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationChooseScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    permissionsState: MultiplePermissionsState,
    transformationSpec: TransformationSpec,
    focusRequester: FocusRequester,
    listState: TransformingLazyColumnState
) {
    val context = LocalContext.current
    val preferences = viewModel.preferences.collectAsState()
    val isLoading by viewModel.loaderState.collectAsState()
    val predictions by viewModel.searchPredictions.collectAsState()

    var showLocationResults by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

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
            item {
                ListSubHeader(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            text = stringResource(if (showLocationResults) R.string.locations else R.string.set_location),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            if (!showLocationResults) {
                item {
                    OutlinedButton(
                        border = ButtonDefaults.outlinedButtonBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            borderWidth = 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        icon = {
                            Image(
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = {
                            Text(
                                color = MaterialTheme.colorScheme.primary,
                                text = stringResource(id = R.string.search)
                            )
                        },
                        onClick = {
                            if (context.isOnline()) {
                                showRenameDialog = true
                            } else {
                                Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                item {
                    OutlinedButton(
                        border = ButtonDefaults.outlinedButtonBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            borderWidth = 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        icon = {
                            Image(
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Current location"
                            )
                        },
                        label = {
                            Text(
                                color = MaterialTheme.colorScheme.primary,
                                text = stringResource(id = R.string.current_location)
                            )
                        },
                        onClick = {
                            if (!context.isOnline()) {
                                Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            if (context.isLocationEnabled()) {
                                if (context.areLocationPermissionsGranted()) {
                                    viewModel.requestLocation()
                                    navController.popBackStack()
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            } else {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    )
                }

                val recentLocations = preferences.value.recentLocations
                if (recentLocations.isNotEmpty()) {
                    item {
                        ListSubHeader(
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.secondary,
                                    text = stringResource(R.string.recent),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }

                    items(recentLocations.size, key = { index -> "${recentLocations[index].latitude}_${recentLocations[index].longitude}_${recentLocations[index].primaryText}" }) { index ->
                        val savedLoc = recentLocations[index]
                        SwipeToReveal(
                            primaryAction = {
                                PrimaryActionButton(
                                    contentColor = MaterialTheme.colorScheme.errorContainer,
                                    onClick = { viewModel.removeSavedLocation(savedLoc) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onError
                                        )
                                    },
                                    text = {}
                                )
                            },
                            onSwipePrimaryAction = { viewModel.removeSavedLocation(savedLoc) }
                        ) {
                            LocationChip(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                                primaryText = savedLoc.primaryText,
                                secondaryText = savedLoc.secondaryText,
                                onClick = {
                                    viewModel.selectSavedLocation(savedLoc)
                                    showLocationResults = false
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            } else {
                locationsListItems(
                    predictions = predictions,
                    transformationSpec = transformationSpec,
                    onLocationSelected = { prediction ->
                        viewModel.getLocationCoordinates(prediction)
                        showLocationResults = false
                        navController.popBackStack()
                    }
                )
            }

            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
            }
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
                        colors = ProgressIndicatorDefaults.colors(
                            trackColor = Color.DarkGray
                        ),
                        strokeWidth = 4.dp,
                        gapSize = 6.dp
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        TextInputDialog(
            showDialog = true,
            inputLabel = stringResource(id = R.string.location),
            initialValue = { "" },
            onSubmit = { name ->
                viewModel.searchLocation(name) { success ->
                    if (success) {
                        showLocationResults = true
                    }
                }
                showRenameDialog = false
            },
            dismissDialog = { showRenameDialog = false },
        )
    }
}
