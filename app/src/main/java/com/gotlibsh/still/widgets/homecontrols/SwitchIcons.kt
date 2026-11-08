package com.gotlibsh.still.widgets.homecontrols

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Thin single-color line icons, drawn here so they follow the text color and never turn into
 * colorful emoji. Paths are SVG path data on a 24x24 grid.
 */
object SwitchIcons {

    private val PATHS: Map<SwitchIcon, List<String>> = mapOf(
        SwitchIcon.COOL to listOf(
            "M12 3 V21", "M4.2 7.5 L19.8 16.5", "M4.2 16.5 L19.8 7.5",
            "M9.5 4.5 L12 7 L14.5 4.5", "M9.5 19.5 L12 17 L14.5 19.5",
        ),
        SwitchIcon.HEAT to listOf(
            "M12 3 C12 3 6 9 6 14 C6 17.3 8.7 20 12 20 C15.3 20 18 17.3 18 14 C18 9 12 3 12 3 Z",
            "M12 20 C10.3 20 9.5 18.6 9.5 17.3 C9.5 15.5 12 13 12 13 C12 13 14.5 15.5 14.5 17.3 C14.5 18.6 13.7 20 12 20",
        ),
        SwitchIcon.LIGHT to listOf(
            "M12 3 C8.7 3 6 5.7 6 9 C6 11.4 7.3 13 8.5 14.2 C9.2 14.9 9 15.5 9 16 H15 C15 15.5 14.8 14.9 15.5 14.2 C16.7 13 18 11.4 18 9 C18 5.7 15.3 3 12 3 Z",
            "M9.5 18.5 H14.5", "M10.5 21 H13.5",
        ),
        SwitchIcon.AIR to listOf(
            "M3 8 H14 A3 3 0 1 0 11 5", "M3 12 H18 A3 3 0 1 1 15 15", "M3 16 H10",
        ),
        SwitchIcon.TV to listOf("M3.5 6 H20.5 V17 H3.5 Z", "M9 21 H15", "M12 17 V21"),
        SwitchIcon.PLUG to listOf(
            "M9 3 V8", "M15 3 V8",
            "M6 8 H18 V11 C18 14.3 15.3 17 12 17 C8.7 17 6 14.3 6 11 Z", "M12 17 V21",
        ),
        SwitchIcon.POWER to listOf("M12 3 V11", "M7.5 6.5 A7 7 0 1 0 16.5 6.5"),
    )

    private val cache = HashMap<SwitchIcon, ImageVector>()

    fun vector(icon: SwitchIcon): ImageVector = cache.getOrPut(icon) {
        val builder = ImageVector.Builder(
            name = "switch_${icon.id}",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        PATHS.getValue(icon).forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White), // tinted by Icon()
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        builder.build()
    }
}
