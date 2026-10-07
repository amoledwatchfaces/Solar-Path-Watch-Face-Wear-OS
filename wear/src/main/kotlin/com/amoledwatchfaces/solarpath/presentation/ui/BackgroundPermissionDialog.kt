package com.amoledwatchfaces.solarpath.presentation.ui

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.amoledwatchfaces.solarpath.presentation.MainViewModel
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import com.amoledwatchfaces.solarpath.utils.isPermissionGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BackgroundPermissionDialog(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(true) }
    var hasForegroundPermission by remember {
        mutableStateOf(context.areLocationPermissionsGranted())
    }

    val backgroundPermissionsState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        onPermissionResult = { granted ->
            showDialog = false
            viewModel.setBackgroundLocationDialogState(false)
            if (granted) {
                viewModel.setBackgroundLocation(true)
            } else {
                Toast.makeText(context, R.string.all_the_time_not_granted, Toast.LENGTH_LONG).show()
            }
        }
    )

    val foregroundPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        onPermissionResult = { granted ->
            if (granted) {
                hasForegroundPermission = true
                if (context.isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    viewModel.setBackgroundLocation(true)
                    showDialog = false
                    viewModel.setBackgroundLocationDialogState(false)
                }
            } else {
                Toast.makeText(context, R.string.enable_permission_toast, Toast.LENGTH_LONG).show()
                showDialog = false
                viewModel.setBackgroundLocationDialogState(false)
            }
        }
    )

    val transformationSpec = rememberTransformationSpec()

    AlertDialog(
        visible = showDialog,
        icon = {
            Icon(
                tint = MaterialTheme.colorScheme.secondary,
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Background Location",
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        },
        title = {
            Text(
                stringResource(R.string.background_location),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                color = MaterialTheme.colorScheme.secondary,
                text = if (!hasForegroundPermission) {
                    stringResource(R.string.background_location_rationale)
                } else {
                    stringResource(R.string.background_location_instruction)
                },
                textAlign = TextAlign.Center
            )
        },
        transformationSpec = transformationSpec,
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (!context.areLocationPermissionsGranted()) {
                        foregroundPermissionState.launchPermissionRequest()
                    } else {
                        backgroundPermissionsState.launchPermissionRequest()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Confirm",
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically),
                )
            }
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    showDialog = false
                    viewModel.setBackgroundLocationDialogState(false)
                }
            )
        },
        onDismissRequest = {
            showDialog = false
            viewModel.setBackgroundLocationDialogState(false)
        }
    )
}
