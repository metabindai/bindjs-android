package ai.metabind.bindjs.composables.ext

import androidx.annotation.DrawableRes
import ai.metabind.bindjs.R

@DrawableRes
fun String?.systemImage(): Int? {
    return when (this) {
        "photo" -> R.drawable.photo
        "info.circle.fill" -> R.drawable.info_circle_fill
        else -> null
    }
}


