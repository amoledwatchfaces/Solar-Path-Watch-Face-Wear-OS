@file:Suppress("UnstableApiUsage")
package com.amoledwatchfaces.solarpath.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.amoledwatchfaces.solarpath.R

@Composable
fun FaqScreen(
    navController: NavHostController,
    transformationSpec: TransformationSpec,
    focusRequester: FocusRequester,
    listState: TransformingLazyColumnState
) {
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
            state = listState
        ) {
            // Header
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
                            text = stringResource(R.string.faq),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
            }

            // 1. 24-Hour Solar Dial
            item {
                FaqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = stringResource(R.string.faq_dial_title),
                    description = stringResource(R.string.faq_dial_desc)
                )
            }

            // 2. 5 Sky & Twilight Phases
            item {
                FaqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = stringResource(R.string.faq_phases_title),
                    description = stringResource(R.string.faq_phases_desc)
                )
            }

            // 3. Sun Marker & Beam
            item {
                FaqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = stringResource(R.string.faq_marker_title),
                    description = stringResource(R.string.faq_marker_desc)
                )
            }

            // 4. 10 Timeline Event Dots
            item {
                FaqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = stringResource(R.string.faq_dots_title),
                    description = stringResource(R.string.faq_dots_desc)
                )
            }

            // 5. Location & Calculations
            item {
                FaqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = stringResource(R.string.faq_location_title),
                    description = stringResource(R.string.faq_location_desc)
                )
            }

            // Footer: amoledwatchfaces.com
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
    }
}

@Composable
private fun FaqCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    transformation:  SurfaceTransformation? = null
) {
    TitleCard(
        modifier = modifier,
        transformation = transformation,
        onClick = {},
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        subtitle = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
