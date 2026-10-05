package com.amoledwatchfaces.solarpath.presentation.ui

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Text
import com.amoledwatchfaces.solarpath.R

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun ConfirmationOverlay(
    showConfirmation: Boolean,
    confirmationState: Boolean,
    onTimeout: () -> Unit
) {
    ConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = onTimeout,
        durationMillis = ConfirmationDialogDefaults.DurationMillis,
        colors = if (confirmationState) ConfirmationDialogDefaults.colors() else ConfirmationDialogDefaults.failureColors(),
        text = {
            Text(
                text = stringResource(id = if (confirmationState) R.string.check_your_phone else R.string.check_your_phone_failure),
                textAlign = TextAlign.Center,
            )
        }
    ) {
        val animation = AnimatedImageVector.animatedVectorResource(
            if (confirmationState) R.drawable.open_on_phone_animation else R.drawable.open_on_phone_animation_failure
        )
        var atEnd by remember { mutableStateOf(false) }
        DisposableEffect(Unit) {
            atEnd = true
            onDispose {}
        }
        Image(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = "Open on phone",
            modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
        )
    }
}
