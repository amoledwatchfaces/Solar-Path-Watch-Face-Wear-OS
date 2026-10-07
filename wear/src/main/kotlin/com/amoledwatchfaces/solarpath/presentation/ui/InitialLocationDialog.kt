package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.amoledwatchfaces.solarpath.R

@Composable
fun InitialLocationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val transformationSpec = rememberTransformationSpec()

    AlertDialog(
        visible = true,
        icon = {
            Icon(
                tint = MaterialTheme.colorScheme.secondary,
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.location_disclosure_title),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        },
        title = {
            Text(
                stringResource(R.string.location_disclosure_title),
                textAlign = TextAlign.Center
            )
        },
        transformationSpec = transformationSpec,
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = onConfirm
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Continue",
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically),
                )
            }
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = onDismiss
            )
        },
        onDismissRequest = onDismiss
    ) {
        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                text = stringResource(R.string.location_disclosure_message),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
