package com.amoledwatchfaces.solarpath.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.tooling.preview.RectangularLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.RoundLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.SquircleLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.WearWidgetPreview

/**
 * Android Studio Design Panel previews for Dusk Times Widget.
 */
@Preview
@Composable
fun DuskTimesWidgetSquirclePreview(
    @PreviewParameter(SquircleLargeWidgetPreviewParams ::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DuskTimesWidget(),
        params = params,
    )
}

@Preview
@Composable
fun DuskTimesWidgetRoundPreview(
    @PreviewParameter(RoundLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DuskTimesWidget(),
        params = params,
    )
}

@Preview(
    name = "Dusk Times Preview",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun DuskTimesWidgetRectangularPreview(
    @PreviewParameter(RectangularLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DuskTimesWidget(),
        params = params,
    )
}

/**
 * Android Studio Design Panel previews for Dawn Times Widget.
 */
@Preview
@Composable
fun DawnTimesWidgetSquirclePreview(
    @PreviewParameter(SquircleLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DawnTimesWidget(),
        params = params,
    )
}

@Preview
@Composable
fun DawnTimesWidgetRoundPreview(
    @PreviewParameter(RoundLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DawnTimesWidget(),
        params = params,
    )
}

@Preview(
    name = "Dawn Times Preview",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun DawnTimesWidgetRectangularPreview(
    @PreviewParameter(RectangularLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = DawnTimesWidget(),
        params = params,
    )
}

/**
 * Android Studio Design Panel previews for Solar Graph Widget.
 */
@Preview
@Composable
fun SolarGraphWidgetSquirclePreview(
    @PreviewParameter(SquircleLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = SolarGraphWidget(),
        params = params,
    )
}

@Preview
@Composable
fun SolarGraphWidgetRoundPreview(
    @PreviewParameter(RoundLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = SolarGraphWidget(),
        params = params,
    )
}

@Preview(
    name = "Solar Graph Preview",
    device = "spec:width=1000dp,height=1000dp,dpi=320",
)
@Composable
fun SolarGraphWidgetRectangularPreview(
    @PreviewParameter(RectangularLargeWidgetPreviewParams::class) params: WearWidgetParams,
) {
    WearWidgetPreview(
        widget = SolarGraphWidget(),
        params = params,
    )
}

