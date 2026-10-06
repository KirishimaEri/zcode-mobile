package com.zcodemobile.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun PathBuilder.rect(x: Float, y: Float, w: Float, h: Float) {
    moveTo(x, y)
    lineTo(x + w, y)
    lineTo(x + w, y + h)
    lineTo(x, y + h)
    close()
}

private fun buildIcon(name: String, fillType: PathFillType = PathFillType.NonZero, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black), pathFillType = fillType, pathBuilder = block).build()

object ZIcons {

    val QrCode: ImageVector by lazy {
        buildIcon("QrCode") {
            rect(3f, 3f, 6f, 6f)
            rect(15f, 3f, 6f, 6f)
            rect(3f, 15f, 6f, 6f)
            rect(11f, 3f, 2f, 2f)
            rect(11f, 7f, 2f, 2f)
            rect(3f, 11f, 2f, 2f)
            rect(7f, 11f, 2f, 2f)
            rect(15f, 15f, 2f, 2f)
            rect(19f, 15f, 2f, 2f)
            rect(15f, 19f, 2f, 2f)
            rect(19f, 19f, 2f, 2f)
        }
    }

    val Photo: ImageVector by lazy {
        buildIcon("Photo", PathFillType.EvenOdd) {
            rect(3f, 3f, 18f, 18f)
            rect(5f, 5f, 14f, 14f)
            moveTo(5f, 19f)
            lineTo(10f, 13f)
            lineTo(13f, 16.5f)
            lineTo(16f, 13f)
            lineTo(19f, 19f)
            close()
            rect(8f, 7f, 2.5f, 2.5f)
        }
    }

    val Paste: ImageVector by lazy {
        buildIcon("Paste", PathFillType.EvenOdd) {
            rect(5f, 3f, 14f, 19f)
            rect(7f, 5f, 10f, 15f)
            moveTo(9f, 2f)
            lineTo(15f, 2f)
            lineTo(15f, 5f)
            lineTo(9f, 5f)
            close()
            rect(9f, 9f, 6f, 1.5f)
            rect(9f, 12f, 6f, 1.5f)
            rect(9f, 15f, 4f, 1.5f)
        }
    }

    val Flashlight: ImageVector by lazy {
        buildIcon("Flashlight") {
            moveTo(6f, 2f)
            lineTo(18f, 2f)
            lineTo(18f, 5f)
            lineTo(6f, 5f)
            close()
            moveTo(8f, 7f)
            lineTo(16f, 7f)
            lineTo(16f, 11f)
            lineTo(14f, 13.5f)
            lineTo(14f, 21f)
            lineTo(10f, 21f)
            lineTo(10f, 13.5f)
            lineTo(8f, 11f)
            close()
            rect(11.25f, 3f, 1.5f, 1f)
        }
    }

    val Desktop: ImageVector by lazy {
        buildIcon("Desktop") {
            rect(3f, 4f, 18f, 12f)
            rect(10f, 17f, 4f, 2f)
            rect(8f, 20f, 8f, 2f)
        }
    }

    val Star: ImageVector by lazy {
        buildIcon("Star") {
            moveTo(12f, 2f)
            lineTo(14.6f, 8.6f)
            lineTo(21.7f, 9.2f)
            lineTo(16.3f, 13.9f)
            lineTo(18f, 21f)
            lineTo(12f, 17.3f)
            lineTo(6f, 21f)
            lineTo(7.7f, 13.9f)
            lineTo(2.3f, 9.2f)
            lineTo(9.4f, 8.6f)
            close()
        }
    }

    val Pencil: ImageVector by lazy {
        buildIcon("Pencil") {
            moveTo(3f, 17.25f)
            lineTo(3f, 21f)
            lineTo(6.75f, 21f)
            lineTo(17.81f, 9.94f)
            lineTo(14.06f, 6.19f)
            lineTo(3f, 17.25f)
            close()
            moveTo(20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            lineTo(16f, 5.66f)
            lineTo(18.34f, 8f)
            lineTo(20.71f, 5.63f)
            close()
        }
    }

    val BrandZ: ImageVector by lazy {
        buildIcon("BrandZ") {
            moveTo(5f, 4f)
            lineTo(19f, 4f)
            lineTo(19f, 8f)
            lineTo(11f, 16f)
            lineTo(19f, 16f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            lineTo(5f, 16f)
            lineTo(13f, 8f)
            lineTo(5f, 8f)
            close()
        }
    }
}
