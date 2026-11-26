package com.thesasan1.app.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn from Material Symbols path data (Apache 2.0) so the app needs no icon
 * library. Tint them with `Icon(tint = ...)`; directional ones mirror in RTL.
 */
object AppIcons {
    val Home: ImageVector by lazy { icon("Home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z") }

    val Tasks: ImageVector by lazy {
        icon(
            "Tasks",
            "M19,3H5C3.89,3 3,3.9 3,5v14c0,1.1 0.89,2 2,2h14c1.11,0 2,-0.9 2,-2V5C21,3.9 20.11,3 19,3z" +
                "M10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z",
        )
    }

    val Habits: ImageVector by lazy {
        icon(
            "Habits",
            "M12,6v3l4,-4 -4,-4v3c-4.42,0 -8,3.58 -8,8 0,1.57 0.46,3.03 1.24,4.26L6.7,14.8" +
                "c-0.45,-0.83 -0.7,-1.79 -0.7,-2.8 0,-3.31 2.69,-6 6,-6zM18.76,7.74L17.3,9.2" +
                "c0.44,0.84 0.7,1.79 0.7,2.8 0,3.31 -2.69,6 -6,6v-3l-4,4 4,4v-3c4.42,0 8,-3.58 8,-8 " +
                "0,-1.57 -0.46,-3.03 -1.24,-4.26z",
        )
    }

    val Notes: ImageVector by lazy {
        icon(
            "Notes",
            "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6z" +
                "M16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z",
        )
    }

    val ChevronForward: ImageVector by lazy {
        icon("ChevronForward", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z", mirror = true)
    }

    val Check: ImageVector by lazy { icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }

    val More: ImageVector by lazy {
        icon(
            "More",
            "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 " +
                "2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
        )
    }

    val Add: ImageVector by lazy { icon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }

    @Suppress("MagicNumber") // Material icons use a 24-unit canvas.
    private fun icon(name: String, path: String, mirror: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = mirror,
        ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
