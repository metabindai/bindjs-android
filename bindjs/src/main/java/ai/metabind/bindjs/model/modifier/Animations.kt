package ai.metabind.bindjs.model.modifier

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

val moveAnimation = spring<Dp>(dampingRatio = 0.7f, stiffness = 300f)
val opacityAnimation = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
val rotationAnimation = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
val scaleAnimation = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
