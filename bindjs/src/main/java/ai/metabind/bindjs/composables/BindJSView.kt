package ai.metabind.bindjs.composables

import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.composables.chart.ChartView
import ai.metabind.bindjs.composables.chart.PieChartView
import ai.metabind.bindjs.composables.ext.buildModifier
import ai.metabind.bindjs.composables.ext.buildModifierFromSubset
import ai.metabind.bindjs.composables.ext.getAlignment
import ai.metabind.bindjs.composables.ext.getBackgroundComponents
import ai.metabind.bindjs.composables.ext.getBackgroundFrameHeight
import ai.metabind.bindjs.composables.ext.has
import ai.metabind.bindjs.composables.ext.hasFrame
import ai.metabind.bindjs.composables.ext.modifiersToShareWithChildren
import ai.metabind.bindjs.composables.ext.process
import ai.metabind.bindjs.model.AngularGradientComponent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.BrushComponent
import ai.metabind.bindjs.model.ButtonComponent
import ai.metabind.bindjs.model.CapsuleComponent
import ai.metabind.bindjs.model.CircleComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColumnComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.DividerComponent
import ai.metabind.bindjs.model.EllipseComponent
import ai.metabind.bindjs.model.EllipticalGradientComponent
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.GeometryReaderComponent
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.ImageComponent
import ai.metabind.bindjs.model.LabelComponent
import ai.metabind.bindjs.model.LinearGradientComponent
import ai.metabind.bindjs.model.MenuComponent
import ai.metabind.bindjs.model.Model3DComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.ModifierProps
import ai.metabind.bindjs.model.NavigationLinkComponent
import ai.metabind.bindjs.model.PickerComponent
import ai.metabind.bindjs.model.ProgressViewComponent
import ai.metabind.bindjs.model.RadialGradientComponent
import ai.metabind.bindjs.model.RectangleComponent
import ai.metabind.bindjs.model.RoundedRectangleComponent
import ai.metabind.bindjs.model.RowComponent
import ai.metabind.bindjs.model.ScrollComponent
import ai.metabind.bindjs.model.SectionComponent
import ai.metabind.bindjs.model.TextComponent
import ai.metabind.bindjs.model.TextEditorComponent
import ai.metabind.bindjs.model.TextFieldComponent
import ai.metabind.bindjs.model.ToggleComponent
import ai.metabind.bindjs.model.VideoComponent
import ai.metabind.bindjs.model.chart.AreaMarkComponent
import ai.metabind.bindjs.model.chart.BarMarkComponent
import ai.metabind.bindjs.model.chart.ChartComponent
import ai.metabind.bindjs.model.chart.LineMarkComponent
import ai.metabind.bindjs.model.chart.PieChartComponent
import ai.metabind.bindjs.model.chart.PieSliceMarkComponent
import ai.metabind.bindjs.model.chart.PointMarkComponent
import ai.metabind.bindjs.model.chart.RectangleMarkComponent
import ai.metabind.bindjs.model.chart.RuleMarkComponent
import ai.metabind.bindjs.model.ext.toAlignment
import ai.metabind.bindjs.model.modifier.ClipShapeModifier
import ai.metabind.bindjs.model.modifier.ClippedModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.ContextMenuModifier
import ai.metabind.bindjs.model.modifier.CornerRadiusModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.LocalModifier
import ai.metabind.bindjs.model.modifier.MaskModifier
import ai.metabind.bindjs.model.modifier.OnAppearModifier
import ai.metabind.bindjs.model.modifier.OnDisappearModifier
import ai.metabind.bindjs.model.modifier.OverlayModifier
import ai.metabind.bindjs.model.modifier.ZIndexModifier
import ai.metabind.bindjs.model.modifier.asColorComponent

private const val TAG = "BindJSView"

private val LocalIsInsideBindJS = compositionLocalOf { false }

@Composable
fun BindJSView(
    jsRuntime: JsRuntime,
    component: BaseComponent<*>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
    hasFrame: Boolean = false,
) {
    val isRoot = !LocalIsInsideBindJS.current

    if (isRoot) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalIsInsideBindJS provides true) {
                BindJSViewImpl(jsRuntime, component, version, onUiEvent, modifiers, isBackground, hasFrame)
            }
        }
    } else {
        BindJSViewImpl(jsRuntime, component, version, onUiEvent, modifiers, isBackground, hasFrame)
    }
}

@Composable
private fun BindJSViewImpl(
    jsRuntime: JsRuntime,
    component: BaseComponent<*>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
    hasFrame: Boolean = false,
) {
    if (component is ModifiedComponent) {
        ModifiedComponent(
            component = component,
            version = version,
            jsRuntime = jsRuntime,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
            isBackground = isBackground,
            hasFrame = hasFrame
        )
    } else if (isBackground) {
        val modifiersFinal = if (modifiers.has<FrameModifier>() && modifiers.has<LocalModifier>()) {
            modifiers.filter {
                it !is LocalModifier
            }
        } else {
            modifiers
        }
        ComponentInnerView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiersFinal
        )
    } else {
        NonModifiedComponent(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            onUiEvent = onUiEvent,
            entryModifiers = modifiers,
            hasFrame = hasFrame
        )
    }
}

@Composable
private fun InnerComponents(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    components: List<BaseComponent<*>?>?,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
    hasFrame: Boolean = false,
) {
    components?.forEach { innerComponent ->
        innerComponent?.let {
            BindJSView(
                jsRuntime = jsRuntime,
                component = innerComponent,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers,
                isBackground = isBackground,
                hasFrame = hasFrame
            )
        }
    }
}

@Composable
private fun NonModifiedComponent(
    jsRuntime: JsRuntime,
    component: BaseComponent<*>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    entryModifiers: List<ComponentModifier<*>> = listOf(),
    hasFrame: Boolean = false,
) {
    val modifiers = entryModifiers.process()
    val hasFill =
        modifiers.any { it is LocalModifier.FillMaxSize || it is LocalModifier.FillMaxWidth }
    val bgFrameHeight = modifiers.getBackgroundFrameHeight()
    Box(
        modifier =
            modifiers
                .buildModifierFromSubset(
                    onUiEvent, include = listOf(
                        LocalModifier.Weight::class,
                        LocalModifier.MatchParentSize::class,
                        LocalModifier.FillMaxWidth::class,
                        LocalModifier.FillMaxSize::class,
                        ZIndexModifier::class,
                        CornerRadiusModifier::class,
                        ClipShapeModifier::class,
                        ClippedModifier::class
                    )
                )
                .then(if (!hasFill) Modifier.wrapContentSize(modifiers.getAlignment()) else Modifier)
                .then(if (bgFrameHeight != null) Modifier.defaultMinSize(minHeight = bgFrameHeight.dp) else Modifier),
        contentAlignment = modifiers.getAlignment()
    ) {
        BackgroundViews(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
            matchParentSizeModifier = LocalModifier.MatchParentSize(Modifier.matchParentSize())
        )

        ComponentInnerView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
            hasFrame = hasFrame
        )
    }
}

@Composable
private fun ModifiedComponent(
    component: ModifiedComponent,
    version: Int,
    jsRuntime: JsRuntime,
    onUiEvent: (UiEvent) -> Unit,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
    hasFrame: Boolean = false,
) {
    val modifierProps = component.props
    val modifier = modifierProps.modifier
    val updateModifiers = if (modifier != null) {
        val newModifierList = modifiers.toMutableList()
        newModifierList.add(modifier)
        newModifierList
    } else {
        modifiers
    }

    when (modifier) {
        is OverlayModifier -> OverlayModifier(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifier = modifier,
            modifierProps = modifierProps,
            modifiers = updateModifiers,
            isBackground = isBackground
        )

        is FrameModifier -> FrameModifier(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifierProps = modifierProps,
            modifiers = updateModifiers,
            isBackground = isBackground,
            parentHasFrame = hasFrame
        )

        is MaskModifier -> MaskModifier(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifier = modifier,
            modifierProps = modifierProps,
            modifiers = updateModifiers,
            isBackground = isBackground
        )

        is ContextMenuModifier -> ContextMenuModifier(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifier = modifier,
            modifierProps = modifierProps,
            modifiers = updateModifiers,
            isBackground = isBackground
        )

        // Fire effect modifiers at the layer they're attached to, before
        // descending. Special modifier composables below us (FrameModifier,
        // MaskModifier, ContextMenuModifier, ...) call InnerComponents with
        // `modifiers.modifiersToShareWithChildren()`, which strips these
        // out — handling them here means a `.frame(...).onAppear(...)` still
        // fires correctly. Don't add the modifier to `updateModifiers`,
        // otherwise NonModifiedComponent would fire it a second time at the
        // leaf.
        is OnAppearModifier -> {
            LaunchedEffect(Unit) {
                onUiEvent(UiEvent.OnAppear(modifier.props.handlerId))
            }
            InnerComponents(
                jsRuntime = jsRuntime,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers,
                components = modifierProps.content,
                isBackground = isBackground,
                hasFrame = hasFrame
            )
        }

        is OnDisappearModifier -> {
            DisposableEffect(Unit) {
                onDispose {
                    onUiEvent(UiEvent.OnDisappear(modifier.props.handlerId))
                }
            }
            InnerComponents(
                jsRuntime = jsRuntime,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers,
                components = modifierProps.content,
                isBackground = isBackground,
                hasFrame = hasFrame
            )
        }

        else -> InnerComponents(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = updateModifiers,
            components = modifierProps.content,
            isBackground = isBackground,
            hasFrame = hasFrame
        )
    }
}

@Composable
private fun OverlayModifier(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifier: OverlayModifier,
    modifierProps: ModifierProps,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
) {
    // The outer Box already carries the full accumulated modifier chain
    // (offset/shadow/cornerRadius/...). The base content and the overlay layer
    // live *inside* that Box, so they must only inherit text-formatting
    // modifiers — re-applying geometry modifiers here double-applies things
    // like offset, pushing the overlay (e.g. a color-swatch palette) off its
    // intended position. Matches MaskModifier / ContextMenuModifier.
    val childModifiers = modifiers.modifiersToShareWithChildren()
    Box(
        modifier =
            modifiers
                .buildModifier(onUiEvent),
        contentAlignment = modifiers.getAlignment()
    ) {
        InnerComponents(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = childModifiers,
            components = modifierProps.content,
            isBackground = isBackground
        )
        modifier.props.content?.let {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = modifier.props.alignment.toAlignment()
            ) {
                // matchParentSize gives the overlay layer the base's bounded
                // size, so flag it as a bounded-height context — lets greedy
                // content (e.g. a VStack of color swatches) fill and distribute
                // instead of collapsing.
                BindJSView(
                    jsRuntime = jsRuntime,
                    component = it,
                    version = version,
                    onUiEvent = onUiEvent,
                    modifiers = childModifiers,
                    isBackground = isBackground,
                    hasFrame = true
                )
            }
        }
    }
}

@Composable
private fun MaskModifier(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifier: MaskModifier,
    modifierProps: ModifierProps,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
) {
    val maskComponent = modifier.props.rawValue ?: return

    Box(
        modifier = modifiers
            .buildModifier(onUiEvent, exclude = listOf(MaskModifier::class))
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        // Draw the content (destination)
        InnerComponents(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers.modifiersToShareWithChildren(),
            components = modifierProps.content,
            isBackground = isBackground
        )

        // Draw the mask (source) with DstIn blend mode
        // DstIn keeps content only where the mask is opaque
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(
                            Rect(Offset.Zero, size),
                            Paint().apply { blendMode = BlendMode.DstIn }
                        )
                    }
                    drawContent()
                    drawIntoCanvas { canvas ->
                        canvas.restore()
                    }
                }
        ) {
            BindJSView(
                jsRuntime = jsRuntime,
                component = maskComponent,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = listOf(
                    LocalModifier.FillMaxSize(Modifier.fillMaxSize())
                )
            )
        }
    }
}

@Composable
private fun ContextMenuModifier(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifier: ContextMenuModifier,
    modifierProps: ModifierProps,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        InnerComponents(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers.modifiersToShareWithChildren(),
            components = modifierProps.content,
            isBackground = isBackground
        )

        modifier.props.content?.let { menuContent ->
            CenteredPopupMenu(
                jsRuntime = jsRuntime,
                version = version,
                expanded = expanded,
                onDismiss = { expanded = false },
                menuContent = menuContent,
                onUiEvent = onUiEvent
            )
        }
    }
}

/**
 * A popup menu centered on its parent. Measures its own size and applies
 * a negative offset of half width/height so the menu center aligns with
 * the anchor center. Reused by ContextMenuModifier and MenuView.
 */
@Composable
private fun CenteredPopupMenu(
    jsRuntime: JsRuntime,
    version: Int,
    expanded: Boolean,
    onDismiss: () -> Unit,
    menuContent: BaseComponent<*>,
    onUiEvent: (UiEvent) -> Unit,
) {
    val density = LocalDensity.current
    val menuSizePx = remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = DpOffset(
                x = with(density) { -(menuSizePx.value.width / 2f).toDp() },
                y = with(density) { -(menuSizePx.value.height / 2f).toDp() }
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.onSizeChanged { size ->
                menuSizePx.value = size
            }) {
                androidx.compose.foundation.layout.Column {
                    ContextMenuItems(
                        jsRuntime = jsRuntime,
                        version = version,
                        component = menuContent,
                        onUiEvent = onUiEvent,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItems(
    jsRuntime: JsRuntime,
    version: Int,
    component: BaseComponent<*>,
    onUiEvent: (UiEvent) -> Unit,
    onDismiss: () -> Unit,
    textColor: Color? = null,
) {
    when (component) {
        is GroupComponent -> {
            component.props.children?.forEach { child ->
                child?.let {
                    ContextMenuItems(
                        jsRuntime = jsRuntime,
                        version = version,
                        component = it,
                        onUiEvent = onUiEvent,
                        onDismiss = onDismiss,
                        textColor = textColor
                    )
                }
            }
        }

        is ButtonComponent -> {
            DropdownMenuItem(
                text = {
                    BindJSView(
                        jsRuntime = jsRuntime,
                        component = component.props.label,
                        version = version,
                        onUiEvent = onUiEvent
                    )
                },
                onClick = {
                    onDismiss()
                    onUiEvent(UiEvent.OnTap(component.props.handlerId))
                }
            )
        }

        is DividerComponent -> {
            HorizontalDivider()
        }

        is MenuComponent -> {
            var subMenuExpanded by remember { mutableStateOf(false) }
            var itemWidthPx by remember { mutableStateOf(0) }
            var itemHeightPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current

            // Wrap item + submenu in a Box so the submenu anchors relative to this item
            Box {
                DropdownMenuItem(
                    modifier = Modifier.onSizeChanged {
                        itemWidthPx = it.width
                        itemHeightPx = it.height
                    },
                    text = {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            component.props.label?.let { label ->
                                BindJSView(
                                    jsRuntime = jsRuntime,
                                    component = label,
                                    version = version,
                                    onUiEvent = onUiEvent
                                )
                            }
                            Text(text = " \u25B6")
                        }
                    },
                    onClick = { subMenuExpanded = true }
                )
                // Offset the submenu to the right of this item, top-aligned.
                // DropdownMenu places the popup below the anchor, so use a
                // negative Y offset equal to the item height to pull it up.
                DropdownMenu(
                    expanded = subMenuExpanded,
                    onDismissRequest = {
                        subMenuExpanded = false
                        onDismiss()
                    },
                    offset = DpOffset(
                        x = with(density) { itemWidthPx.toDp() },
                        y = with(density) { -itemHeightPx.toDp() }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    component.props.children?.forEach { child ->
                        child?.let {
                            ContextMenuItems(
                                jsRuntime = jsRuntime,
                                version = version,
                                component = it,
                                onUiEvent = onUiEvent,
                                onDismiss = {
                                    subMenuExpanded = false
                                    onDismiss()
                                },
                                textColor = textColor
                            )
                        }
                    }
                }
            }
        }

        is ModifiedComponent -> {
            val modifier = component.props.modifier
            val childColor = if (modifier is ForegroundStyleModifier) {
                val colorComponent = modifier.props.rawValue.asColorComponent()
                colorComponent?.let { Color(it.color) }
            } else {
                textColor
            }
            component.props.content?.forEach { child ->
                child?.let {
                    ContextMenuItems(
                        jsRuntime = jsRuntime,
                        version = version,
                        component = it,
                        onUiEvent = onUiEvent,
                        onDismiss = onDismiss,
                        textColor = childColor ?: textColor
                    )
                }
            }
        }

        else -> {}
    }
}

@Composable
private fun MenuView(
    jsRuntime: JsRuntime,
    component: MenuComponent,
    version: Int,
    modifiers: List<ComponentModifier<*>> = listOf(),
    onUiEvent: (UiEvent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifiers
            .buildModifier(onUiEvent)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = { expanded = true },
                onLongClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        // Render the label as the visible content
        component.props.label?.let { label ->
            BindJSView(
                jsRuntime = jsRuntime,
                component = label,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers.modifiersToShareWithChildren()
            )
        }

        // Render children as a centered popup menu on click
        component.props.children?.let { children ->
            val menuGroup = GroupComponent(
                props = ai.metabind.bindjs.model.props.LayoutProps(
                    spacing = null,
                    alignment = null,
                    children = children.filterNotNull()
                )
            )
            CenteredPopupMenu(
                jsRuntime = jsRuntime,
                version = version,
                expanded = expanded,
                onDismiss = { expanded = false },
                menuContent = menuGroup,
                onUiEvent = onUiEvent
            )
        }
    }
}


@Composable
private fun FrameModifier(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifierProps: ModifierProps,
    modifiers: List<ComponentModifier<*>> = listOf(),
    isBackground: Boolean = false,
    parentHasFrame: Boolean = false,
) {
    // When the frame has explicit width/height, exclude inherited
    // FillMaxSize/FillMaxWidth so the frame's own size takes effect.
    val frameModifier = modifiers.firstOrNull { it is FrameModifier } as? FrameModifier
    val hasExplicitSize =
        frameModifier?.props?.width != null || frameModifier?.props?.height != null
    val effectiveModifiers = if (hasExplicitSize) {
        modifiers.filter { it !is LocalModifier.FillMaxSize && it !is LocalModifier.FillMaxWidth }
    } else {
        modifiers
    }.process()

    // When a GeometryReader is the direct content of a frame with explicit
    // height, use heightIn(max=) instead of exact height() so the Box wraps
    // to the GeometryReader's content height. This prevents the shadow from
    // drawing at the full frame height (e.g. 500dp) when the actual rendered
    // content is smaller (e.g. 226dp).
    val contentHasGeometryReader =
        modifierProps.content?.any { it is GeometryReaderComponent } == true

    val bgFrameHeight = effectiveModifiers.getBackgroundFrameHeight()
    val boxModifier = if (contentHasGeometryReader && frameModifier?.props?.height != null) {
        effectiveModifiers
            .buildModifier(onUiEvent, exclude = listOf(FrameModifier::class))
            .then(if (frameModifier.props.width != null) Modifier.width(frameModifier.props.width.dp) else Modifier)
            .then(Modifier.heightIn(max = frameModifier.props.height.dp))
    } else {
        effectiveModifiers.buildModifier(onUiEvent)
    }
    // A height-only frame (no explicit width) is typically an inner content
    // box — e.g. a fixed-height text slot. Compose Text doesn't auto-trim by
    // height, so a 3-line description in a 2-line slot would draw past its
    // bounds and overlap siblings. Clip to enforce the explicit height.
    // Width-and-height frames are skipped because they're typically card-like
    // containers whose shadow modifiers need to render outside the bounds.
    val shouldClipToHeight = frameModifier?.props?.height != null &&
            frameModifier.props.width == null &&
            !contentHasGeometryReader
    Box(
        modifier = boxModifier
            .then(if (bgFrameHeight != null) Modifier.defaultMinSize(minHeight = bgFrameHeight.dp) else Modifier)
            .then(if (shouldClipToHeight) Modifier.clipToBounds() else Modifier),
        contentAlignment = modifiers.getAlignment()
    ) {
        BackgroundViews(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
            matchParentSizeModifier = LocalModifier.MatchParentSize(Modifier.matchParentSize())
        )
        // Preserve bounded height from either this frame or an ancestor frame
        val hasBoundedHeight = frameModifier?.props?.height != null || parentHasFrame
        InnerComponents(
            jsRuntime = jsRuntime,
            version = version,
            onUiEvent = onUiEvent,
            modifiers = modifiers.modifiersToShareWithChildren(),
            components = modifierProps.content,
            isBackground = isBackground,
            hasFrame = hasBoundedHeight
        )
    }
}

@Composable
private fun BoxScope.BackgroundViews(
    jsRuntime: JsRuntime,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifiers: List<ComponentModifier<*>> = listOf(),
    matchParentSizeModifier: LocalModifier.MatchParentSize,
) {
    val backgroundComponents = modifiers.getBackgroundComponents()
    backgroundComponents.forEach {
        if (it !is ColorComponent &&
            it !is BrushComponent
        ) {
            // Wrap in a Box with matchParentSize so the background fills the
            // parent regardless of how deep the component tree is.  Previously
            // matchParentSize was passed as a modifier through the tree, but it
            // would get lost in intermediate NonModifiedComponent wrapping boxes.
            Box(modifier = Modifier.matchParentSize()) {
                val bgModifiers = listOf(
                    LocalModifier.FillMaxSize(Modifier.fillMaxSize())
                )
                if (it is ModifiedComponent) {
                    BindJSView(
                        jsRuntime = jsRuntime,
                        component = it,
                        version = version,
                        onUiEvent = onUiEvent,
                        modifiers = bgModifiers,
                        isBackground = true
                    )
                } else {
                    ComponentInnerView(
                        jsRuntime = jsRuntime,
                        component = it,
                        version = version,
                        onUiEvent = onUiEvent,
                        modifiers = bgModifiers
                    )
                }
            }
        }
    }
}

private fun addFillIfNoFrame(modifiers: List<ComponentModifier<*>>): List<ComponentModifier<*>> {
    return if (modifiers.hasFrame()) {
        modifiers
    } else {
        modifiers + LocalModifier.FillMaxSize(Modifier.fillMaxSize())
    }
}

private fun addFill(modifiers: List<ComponentModifier<*>>): List<ComponentModifier<*>> {
    return modifiers + LocalModifier.FillMaxSize(Modifier.fillMaxSize())
}

private fun addWrapIfNoFrame(modifiers: List<ComponentModifier<*>>): List<ComponentModifier<*>> {
    return if (modifiers.hasFrame() || modifiers.any { it is LocalModifier.FillMaxSize || it is LocalModifier.FillMaxWidth }) {
        modifiers
    } else {
        modifiers + LocalModifier.WrapContentSize(Modifier.wrapContentSize(modifiers.getAlignment()))
    }
}

private fun wrapContentSize(modifiers: List<ComponentModifier<*>>): List<ComponentModifier<*>> {
    return modifiers + LocalModifier.WrapContentSize(Modifier.wrapContentSize(modifiers.getAlignment()))
}

@Composable
private fun ComponentInnerView(
    jsRuntime: JsRuntime,
    component: BaseComponent<*>,
    version: Int,
    onUiEvent: (UiEvent) -> Unit,
    modifiers: List<ComponentModifier<*>> = listOf(),
    hasFrame: Boolean = false,
) {
    when (component) {
        is ChartComponent -> ChartView(
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is PieChartComponent -> PieChartView(
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is BarMarkComponent -> UnsupportedChartContent("BarMark", modifiers, onUiEvent)

        is LineMarkComponent -> UnsupportedChartContent("LineMark", modifiers, onUiEvent)

        is AreaMarkComponent -> UnsupportedChartContent("AreaMark", modifiers, onUiEvent)

        is PointMarkComponent -> UnsupportedChartContent("PointMark", modifiers, onUiEvent)

        is RuleMarkComponent -> UnsupportedChartContent("RuleMark", modifiers, onUiEvent)

        is RectangleMarkComponent -> UnsupportedChartContent("RectangleMark", modifiers, onUiEvent)

        is PieSliceMarkComponent -> UnsupportedChartContent("PieSliceMark", modifiers, onUiEvent)

        is BoxComponent -> BoxView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent,
            hasFrame = hasFrame
        )

        is ButtonComponent -> ButtonView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = wrapContentSize(modifiers),
            onUiEvent = onUiEvent
        )

        is TextFieldComponent -> TextFieldView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = wrapContentSize(modifiers),
            onUiEvent = onUiEvent
        )

        is LabelComponent -> LabelView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = wrapContentSize(modifiers),
            onUiEvent = onUiEvent
        )

        is Component -> {
            InnerComponents(
                jsRuntime = jsRuntime,
                version = version,
                onUiEvent = onUiEvent,
                modifiers = modifiers,
                components = component.props.children,
                hasFrame = hasFrame
            )
        }

        is CircleComponent -> CircleView(
            jsRuntime,
            component,
            if (!hasFrame) addFill(modifiers) else modifiers,
            onUiEvent,
            hasFrame
        )

        is ColumnComponent -> ColumnView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent,
            hasFrame = hasFrame
        )

        is ForEachComponent -> ForEachView(
            jsRuntime = jsRuntime,
            onUiEvent = onUiEvent,
            modifiers = modifiers,
            component = component,
            version = version
        )

        is ImageComponent -> ImageView(jsRuntime, component, addFillIfNoFrame(modifiers), onUiEvent)

        is RectangleComponent -> RectangleView(
            jsRuntime,
            component,
            addFillIfNoFrame(modifiers),
            onUiEvent
        )

        is RowComponent -> RowView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is ScrollComponent -> ScrollView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is TextComponent -> {
            TextView(jsRuntime, component, addWrapIfNoFrame(modifiers), onUiEvent)
        }

        is SectionComponent -> SectionView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is GroupComponent -> GroupView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent,
            hasFrame = hasFrame
        )

        is DividerComponent -> DividerView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is GeometryReaderComponent -> GeometryReaderView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is ProgressViewComponent -> ProgressView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is TextEditorComponent -> TextEditorView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is RoundedRectangleComponent -> RoundedRectangleView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is CapsuleComponent -> CapsuleView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is EllipseComponent -> EllipseView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is ColorComponent -> ColorView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is PickerComponent -> PickerView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is Model3DComponent -> Model3DView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is VideoComponent -> VideoView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is LinearGradientComponent -> LinearGradientView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is AngularGradientComponent -> AngularGradientView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is RadialGradientComponent -> RadialGradientView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is EllipticalGradientComponent -> EllipticalGradientView(
            jsRuntime = jsRuntime,
            component = component,
            modifiers = addFillIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        is NavigationLinkComponent -> NavigationLinkView(
            jsRuntime = jsRuntime,
            version = version,
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is ToggleComponent -> ToggleView(
            component = component,
            modifiers = modifiers,
            onUiEvent = onUiEvent
        )

        is MenuComponent -> MenuView(
            jsRuntime = jsRuntime,
            component = component,
            version = version,
            modifiers = addWrapIfNoFrame(modifiers),
            onUiEvent = onUiEvent
        )

        else -> Log.w(TAG, "No mapping found for component $component")
    }
}

@Composable
private fun UnsupportedChartContent(
    componentName: String,
    modifiers: List<ComponentModifier<*>>,
    onUiEvent: (UiEvent) -> Unit,
) {
    Text(
        text = "Unsupported chart content: $componentName",
        modifier = modifiers.buildModifier(onUiEvent)
    )
}
