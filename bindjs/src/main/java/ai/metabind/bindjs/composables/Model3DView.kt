package ai.metabind.bindjs.composables

import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.google.android.filament.Skybox
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.model.Model3DComponent
import ai.metabind.bindjs.model.modifier.ComponentModifier
import io.github.sceneview.Scene
import io.github.sceneview.animation.Transition.animateRotation
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberRenderer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private const val TAG = "Model3DView"

@Composable
fun Model3DView(
    jsRuntime: JsRuntime,
    component: Model3DComponent,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit
) {
    val engine = rememberEngine()
    val renderer = rememberRenderer(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val centerNode = rememberNode(engine)

    val cameraNode = rememberCameraNode(engine) {
        position = Position(y = -0.5f, z = 2.0f)
        lookAt(centerNode)
        centerNode.addChildNode(this)
    }

    val cameraTransition = rememberInfiniteTransition(label = "CameraTransition")
    val cameraRotation by cameraTransition.animateRotation(
        initialValue = Rotation(y = 0.0f),
        targetValue = Rotation(y = 360.0f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7.seconds.toInt(DurationUnit.MILLISECONDS))
        )
    )

    val modelUrl = component.props.url
    val nodes = remember { mutableStateListOf<ModelNode>() }

    modelUrl?.let {
        LaunchedEffect(Unit) {
            try {
                modelLoader.loadModel(modelUrl)?.let { model ->
                    val modelNode = ModelNode(modelInstance = model.instance).apply {
                        scaleToUnitCube(1.0f)
                        position = Position(x = 0.0f, y = 0.0f, z = 0.0f)
                    }
                    nodes.add(modelNode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load error $modelUrl", e)
            }
        }
    }

    val cameraControlsEnabled = component.props.cameraControls != false

    Scene(
        renderer = renderer,
        modifier = modifiers.buildModifier(onUiEvent),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        cameraManipulator = if (cameraControlsEnabled) {
            rememberCameraManipulator(
                orbitHomePosition = cameraNode.worldPosition,
                targetPosition = centerNode.worldPosition
            )
        } else {
            null
        },
        childNodes = nodes,
        environment = rememberEnvironment(environmentLoader, isOpaque = false),
        onFrame = {
            if (component.props.autoRotate == true) {
                centerNode.rotation = cameraRotation
                cameraNode.lookAt(centerNode)
            }
        },
        onViewCreated = {
            // TODO, perhaps just make the color match the parent component...? Transparent isn't working
            skybox = Skybox.Builder().color(255f, 255f, 255f, 1.0f).build(this.engine)

            // Prevent parent scroll containers from intercepting touch events
            // so the user can manipulate the 3D model.
            if (cameraControlsEnabled) {
                setOnTouchListener { _, event ->
                    when (event.action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_DOWN ->
                            parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            }
        },
    )
}
