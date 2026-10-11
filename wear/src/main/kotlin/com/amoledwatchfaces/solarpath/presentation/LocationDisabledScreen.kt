package com.amoledwatchfaces.solarpath.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.amoledwatchfaces.solarpath.R

@Composable
fun LocationDisabledScreen(
    transformationSpec: TransformationSpec,
    onEnableLocation: () -> Unit,
    onUseWithoutLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }

    ScreenScaffold(
        scrollState = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { paddingValues ->
        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    RotaryScrollableDefaults.behavior(scrollableState = listState),
                    focusRequester = focusRequester
                ),
            state = listState,
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = paddingValues.calculateTopPadding() + 28.dp,
                bottom = paddingValues.calculateBottomPadding() + 20.dp
            )
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.location_disclosure_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.location_disclosure_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item {
                Button(
                    onClick = onEnableLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.enable_location_permissions),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            item {
                FilledTonalButton(
                    onClick = onUseWithoutLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.use_without_location),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
    }
}
