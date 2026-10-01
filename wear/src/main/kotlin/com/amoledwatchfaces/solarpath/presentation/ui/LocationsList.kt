@file:Suppress("UnstableApiUsage")
package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.amoledwatchfaces.solarpath.location.LocationPrediction

fun TransformingLazyColumnScope.locationsListItems(
    predictions: List<LocationPrediction>,
    transformationSpec: TransformationSpec,
    onLocationSelected: (LocationPrediction) -> Unit
) {
    items(predictions) { prediction ->
        LocationChip(
            modifier = Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
            primaryText = prediction.primaryText,
            secondaryText = prediction.secondaryText,
            onClick = { onLocationSelected(prediction) }
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

@Composable
fun LocationChip(
    modifier: Modifier,
    transformation: SurfaceTransformation,
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit
) {
    Button(
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier,
        transformation = transformation,
        onClick = onClick,
        icon = {
            Image(
                imageVector = Icons.Default.LocationCity,
                contentDescription = "Location Icon",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
            )
        },
        label = { Text(text = primaryText) },
        secondaryLabel = {
            Text(
                text = secondaryText,
                color = MaterialTheme.colorScheme.tertiary,
            )
        },
    )
}
