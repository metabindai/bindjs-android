package ai.metabind.bindjs.composables.ext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Material glyphs that [SYSTEM_ICONS] needs but `material-icons-core` does not ship.
 *
 * The core artifact is a deliberately small set — roughly 50 icons, all of them
 * navigation/chrome staples — so whole categories of SF Symbol that A2UI content leans on
 * (media transport, volume, visibility) have no counterpart at all. `material-icons-extended`
 * covers them, but it is a several-thousand-class dependency that only shrinks back down
 * under R8 in the consuming app, which is a poor trade for the couple of dozen glyphs
 * actually referenced here. Vendoring is also what androidx now recommends, and what
 * `res/drawable/photo.xml` already does for the two purpose-drawn icons.
 *
 * Path data is copied verbatim from google/material-design-icons (`materialicons` 24px,
 * Apache 2.0), so these render identically to the core set beside them.
 */
internal object VendoredIcons {

    // ─── Media transport ──────────────────────────────────────
    val Pause: ImageVector by lazy {
        icon("Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z")
    }
    val SkipNext: ImageVector by lazy {
        icon("SkipNext", "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z")
    }
    val SkipPrevious: ImageVector by lazy {
        icon("SkipPrevious", "M6 6h2v12H6zm3.5 6l8.5 6V6z")
    }
    val FastForward: ImageVector by lazy {
        icon("FastForward", "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z")
    }
    val FastRewind: ImageVector by lazy {
        icon("FastRewind", "M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z")
    }
    val Stop: ImageVector by lazy {
        icon("Stop", "M6 6h12v12H6z")
    }

    // ─── Volume ───────────────────────────────────────────────
    val VolumeUp: ImageVector by lazy {
        icon(
            "VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 " +
                "2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 " +
                "7-4.49 7-8.77s-2.99-7.86-7-8.77z",
        )
    }
    val VolumeDown: ImageVector by lazy {
        icon(
            "VolumeDown",
            "M18.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM5 9v6h4l5 5V4L9 9H5z",
        )
    }
    val VolumeMute: ImageVector by lazy {
        icon("VolumeMute", "M7 9v6h4l5 5V4l-5 5H7z")
    }
    val VolumeOff: ImageVector by lazy {
        icon(
            "VolumeOff",
            "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 " +
                "0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 " +
                "5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 " +
                "1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z",
        )
    }

    // ─── Visibility ───────────────────────────────────────────
    val Visibility: ImageVector by lazy {
        icon(
            "Visibility",
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 " +
                "17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 " +
                "3 3-1.34 3-3-1.34-3-3-3z",
        )
    }
    val VisibilityOff: ImageVector by lazy {
        icon(
            "VisibilityOff",
            "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 " +
                "3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 " +
                "4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 " +
                "4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 " +
                "0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 " +
                "0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z",
        )
    }

    // ─── Objects ──────────────────────────────────────────────
    /**
     * Two subpaths: the body (whose counter-wound lens ring punches a hole under non-zero
     * winding, as in the source SVG) and the `<circle r="3.2">` aperture, expressed as arcs
     * because [PathParser] takes path data only.
     */
    val PhotoCamera: ImageVector by lazy {
        icon(
            "PhotoCamera",
            "M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 " +
                "2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z " +
                "M12 8.8a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 1 0 0-6.4z",
        )
    }
    val Folder: ImageVector by lazy {
        icon("Folder", "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z")
    }
    val Print: ImageVector by lazy {
        icon(
            "Print",
            "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 " +
                "0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z",
        )
    }
    val CreditCard: ImageVector by lazy {
        icon(
            "CreditCard",
            "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 " +
                "2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z",
        )
    }
    val AttachFile: ImageVector by lazy {
        icon(
            "AttachFile",
            "M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 " +
                "2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 " +
                "2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z",
        )
    }

    // ─── Status ───────────────────────────────────────────────
    val Help: ImageVector by lazy {
        icon(
            "Help",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 " +
                "12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 " +
                "0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z",
        )
    }
    val Error: ImageVector by lazy {
        icon(
            "Error",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 " +
                "2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z",
        )
    }

    // ─── Transfer ─────────────────────────────────────────────
    val FileDownload: ImageVector by lazy {
        icon("FileDownload", "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z")
    }
    val FileUpload: ImageVector by lazy {
        icon("FileUpload", "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z")
    }

    /**
     * Material's 24×24 grid, filled solid black so `Icon`'s tint applies — the same shape
     * `materialIcon { materialPath { … } }` builds for the core set.
     */
    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
}
