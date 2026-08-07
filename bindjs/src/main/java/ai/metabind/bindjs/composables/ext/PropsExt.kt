package ai.metabind.bindjs.composables.ext

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import ai.metabind.bindjs.R
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "SystemImage"

@DrawableRes
fun String?.systemImage(): Int? {
    return when (this) {
        "photo" -> R.drawable.photo
        "info.circle.fill" -> R.drawable.info_circle_fill
        else -> null
    }
}

/**
 * SF Symbol name → Material icon.
 *
 * `Image(systemName:)` resolves against the OS-provided SF Symbols catalog on iOS, which
 * Android has no equivalent of, so the JS bridge's symbol names have to be mapped by hand.
 * [systemImage] covers the two names with purpose-drawn drawables; everything else falls
 * back to Material's icon set, which ships with `material3` and so costs no new dependency.
 *
 * The mapping is approximate by nature — Material has no counterpart for most SF Symbols,
 * and where it does the two rarely agree on weight or optical size. That is still far
 * better than the previous behaviour, where an unmapped name rendered *nothing*: a null
 * model handed to Coil, plus an invisible `fillMaxSize` box distorting the layout around it.
 *
 * Names are matched exactly first, then by progressively shorter dot-separated prefixes, so
 * a single entry covers a whole SF Symbol family: `xmark.circle.fill` → `xmark.circle` →
 * `xmark`. The cost is that decorations are flattened — `heart` and `heart.fill` both come
 * out filled, since Material's base set has no consistent outlined/filled pairing.
 *
 * The table covers every symbol the A2UI catalog's `Icon` primitive can emit — its 59 named
 * icons all route through SF Symbol names, and anything it can't name falls back to
 * `questionmark.circle`, so a gap here is a blank space on screen rather than a wrong glyph.
 * Symbols with no `material-icons-core` counterpart come from [VendoredIcons].
 */
private val SYSTEM_ICONS: Map<String, ImageVector> = mapOf(
    // Navigation / chevrons
    "chevron.right" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
    "chevron.forward" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
    "chevron.left" to Icons.AutoMirrored.Filled.KeyboardArrowLeft,
    "chevron.backward" to Icons.AutoMirrored.Filled.KeyboardArrowLeft,
    "chevron.down" to Icons.Filled.KeyboardArrowDown,
    "chevron.up" to Icons.Filled.KeyboardArrowUp,
    "arrow.right" to Icons.AutoMirrored.Filled.ArrowForward,
    "arrow.forward" to Icons.AutoMirrored.Filled.ArrowForward,
    "arrow.left" to Icons.AutoMirrored.Filled.ArrowBack,
    "arrow.backward" to Icons.AutoMirrored.Filled.ArrowBack,
    "arrow.up" to Icons.Filled.KeyboardArrowUp,
    "arrow.down" to Icons.Filled.KeyboardArrowDown,
    "arrow.clockwise" to Icons.Filled.Refresh,
    "arrow.counterclockwise" to Icons.Filled.Refresh,
    "arrow.triangle.2.circlepath" to Icons.Filled.Refresh,
    "rectangle.portrait.and.arrow.right" to Icons.AutoMirrored.Filled.ExitToApp,
    "line.3.horizontal" to Icons.Filled.Menu,
    "ellipsis" to Icons.Filled.MoreVert,
    "list.bullet" to Icons.AutoMirrored.Filled.List,

    // Actions
    "paperplane" to Icons.AutoMirrored.Filled.Send,
    "magnifyingglass" to Icons.Filled.Search,
    "xmark" to Icons.Filled.Close,
    "checkmark" to Icons.Filled.Check,
    "checkmark.circle" to Icons.Filled.CheckCircle,
    "plus" to Icons.Filled.Add,
    "plus.circle" to Icons.Filled.AddCircle,
    "trash" to Icons.Filled.Delete,
    "pencil" to Icons.Filled.Edit,
    "square.and.pencil" to Icons.Filled.Create,
    "square.and.arrow.up" to Icons.Filled.Share,
    "gear" to Icons.Filled.Settings,
    "gearshape" to Icons.Filled.Settings,
    "wrench" to Icons.Filled.Build,
    "hammer" to Icons.Filled.Build,
    "paperclip" to VendoredIcons.AttachFile,
    "printer" to VendoredIcons.Print,
    "arrow.down.circle" to VendoredIcons.FileDownload,
    "arrow.up.circle" to VendoredIcons.FileUpload,

    // Media transport. `forward.end`/`backward.end` are matched ahead of the bare
    // `forward`/`backward` they'd otherwise shorten to, so skip and seek stay distinct.
    "play" to Icons.Filled.PlayArrow,
    "pause" to VendoredIcons.Pause,
    "stop" to VendoredIcons.Stop,
    "forward.end" to VendoredIcons.SkipNext,
    "backward.end" to VendoredIcons.SkipPrevious,
    "forward" to VendoredIcons.FastForward,
    "backward" to VendoredIcons.FastRewind,

    // Volume. SF grades loudness by wave count; Material has only up/down, so 2 and 3 waves
    // both land on `VolumeUp`.
    "speaker.wave.3" to VendoredIcons.VolumeUp,
    "speaker.wave.2" to VendoredIcons.VolumeUp,
    "speaker.wave.1" to VendoredIcons.VolumeDown,
    "speaker.wave" to VendoredIcons.VolumeUp,
    "speaker.slash" to VendoredIcons.VolumeOff,
    "speaker" to VendoredIcons.VolumeMute,

    // Objects / status
    "heart" to Icons.Filled.Favorite,
    "star" to Icons.Filled.Star,
    "hand.thumbsup" to Icons.Filled.ThumbUp,
    "bell" to Icons.Filled.Notifications,
    "cart" to Icons.Filled.ShoppingCart,
    "bag" to Icons.Filled.ShoppingCart,
    "house" to Icons.Filled.Home,
    "person" to Icons.Filled.Person,
    "person.circle" to Icons.Filled.AccountCircle,
    "person.crop.circle" to Icons.Filled.AccountCircle,
    "face.smiling" to Icons.Filled.Face,
    "envelope" to Icons.Filled.Email,
    "phone" to Icons.Filled.Phone,
    "lock" to Icons.Filled.Lock,
    "calendar" to Icons.Filled.DateRange,
    "location" to Icons.Filled.LocationOn,
    "mappin" to Icons.Filled.Place,
    "map" to Icons.Filled.Place,
    "info.circle" to Icons.Filled.Info,
    "exclamationmark.triangle" to Icons.Filled.Warning,
    "exclamationmark.circle" to VendoredIcons.Error,
    "questionmark" to VendoredIcons.Help,
    "camera" to VendoredIcons.PhotoCamera,
    "folder" to VendoredIcons.Folder,
    "creditcard" to VendoredIcons.CreditCard,
    "eye" to VendoredIcons.Visibility,
    "eye.slash" to VendoredIcons.VisibilityOff,
)

/** Names already reported by [systemIcon], so the warning fires once per name, not per frame. */
private val unmappedSymbols = ConcurrentHashMap.newKeySet<String>()

/**
 * The [SYSTEM_ICONS] lookup itself: exact match, then progressively shorter dot-separated
 * prefixes. Kept free of logging so it stays a pure function the unit tests can exercise
 * without the Android framework.
 */
internal fun String.matchSystemIcon(): ImageVector? {
    var name = this
    while (true) {
        SYSTEM_ICONS[name]?.let { return it }
        if (!name.contains('.')) return null
        name = name.substringBeforeLast('.')
    }
}

/**
 * Material icon for an SF Symbol name, or null when nothing in [SYSTEM_ICONS] matches even
 * after stripping decorations. Unmapped names are logged once each under the `SystemImage`
 * tag — that log is how the table grows to fit the content actually being rendered.
 */
fun String?.systemIcon(): ImageVector? {
    val name = this ?: return null
    matchSystemIcon()?.let { return it }
    if (unmappedSymbols.add(name)) {
        Log.w(TAG, "No Material icon mapped for SF Symbol \"$name\" — nothing will render")
    }
    return null
}
