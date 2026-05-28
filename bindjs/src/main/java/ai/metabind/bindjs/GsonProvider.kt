package ai.metabind.bindjs

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import ai.metabind.bindjs.model.AngularGradientComponent
import ai.metabind.bindjs.model.BaseComponent
import ai.metabind.bindjs.model.BoxComponent
import ai.metabind.bindjs.model.ButtonComponent
import ai.metabind.bindjs.model.CapsuleComponent
import ai.metabind.bindjs.model.CircleComponent
import ai.metabind.bindjs.model.ColorComponent
import ai.metabind.bindjs.model.ColumnComponent
import ai.metabind.bindjs.model.Component
import ai.metabind.bindjs.model.DividerComponent
import ai.metabind.bindjs.model.EllipseComponent
import ai.metabind.bindjs.model.EllipticalGradientComponent
import ai.metabind.bindjs.model.EmptyComponent
import ai.metabind.bindjs.model.ForEachComponent
import ai.metabind.bindjs.model.GeometryReaderComponent
import ai.metabind.bindjs.model.GroupComponent
import ai.metabind.bindjs.model.ImageComponent
import ai.metabind.bindjs.model.LabelComponent
import ai.metabind.bindjs.model.LinearGradientComponent
import ai.metabind.bindjs.model.MaterialComponent
import ai.metabind.bindjs.model.MenuComponent
import ai.metabind.bindjs.model.Model3DComponent
import ai.metabind.bindjs.model.ModifiedComponent
import ai.metabind.bindjs.model.NavigationLinkComponent
import ai.metabind.bindjs.model.PickerComponent
import ai.metabind.bindjs.model.ProgressViewComponent
import ai.metabind.bindjs.model.RadialGradientComponent
import ai.metabind.bindjs.model.RectangleComponent
import ai.metabind.bindjs.model.RoundedRectangleComponent
import ai.metabind.bindjs.model.RowComponent
import ai.metabind.bindjs.model.ScrollComponent
import ai.metabind.bindjs.model.SectionComponent
import ai.metabind.bindjs.model.SpacerComponent
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
import ai.metabind.bindjs.model.modifier.AccessibilityHiddenModifier
import ai.metabind.bindjs.model.modifier.AccessibilityHintModifier
import ai.metabind.bindjs.model.modifier.AccessibilityLabelModifier
import ai.metabind.bindjs.model.modifier.AccessibilityRemoveTraitsModifier
import ai.metabind.bindjs.model.modifier.AccessibilityValueModifier
import ai.metabind.bindjs.model.modifier.AllowsHitTestingModifier
import ai.metabind.bindjs.model.modifier.AspectRatioModifier
import ai.metabind.bindjs.model.modifier.AutocorrectionDisabledModifier
import ai.metabind.bindjs.model.modifier.BackgroundModifier
import ai.metabind.bindjs.model.modifier.BlendModeModifier
import ai.metabind.bindjs.model.modifier.BlurModifier
import ai.metabind.bindjs.model.modifier.BoldModifier
import ai.metabind.bindjs.model.modifier.BorderModifier
import ai.metabind.bindjs.model.modifier.BrightnessModifier
import ai.metabind.bindjs.model.modifier.ButtonStyleModifier
import ai.metabind.bindjs.model.modifier.ClipShapeModifier
import ai.metabind.bindjs.model.modifier.ClippedModifier
import ai.metabind.bindjs.model.modifier.ColorInvertModifier
import ai.metabind.bindjs.model.modifier.ColorSchemeModifier
import ai.metabind.bindjs.model.modifier.ComponentModifier
import ai.metabind.bindjs.model.modifier.ContextMenuModifier
import ai.metabind.bindjs.model.modifier.ContrastModifier
import ai.metabind.bindjs.model.modifier.CoordinateSpaceModifier
import ai.metabind.bindjs.model.modifier.CornerRadiusModifier
import ai.metabind.bindjs.model.modifier.DisabledModifier
import ai.metabind.bindjs.model.modifier.EmptyModifier
import ai.metabind.bindjs.model.modifier.EnvironmentModifier
import ai.metabind.bindjs.model.modifier.FixedSizeModifier
import ai.metabind.bindjs.model.modifier.FontDesignModifier
import ai.metabind.bindjs.model.modifier.FontModifier
import ai.metabind.bindjs.model.modifier.FontWeightModifier
import ai.metabind.bindjs.model.modifier.ForegroundStyleModifier
import ai.metabind.bindjs.model.modifier.FrameModifier
import ai.metabind.bindjs.model.modifier.GrayscaleModifier
import ai.metabind.bindjs.model.modifier.HiddenModifier
import ai.metabind.bindjs.model.modifier.IDModifier
import ai.metabind.bindjs.model.modifier.IgnoresSafeAreaModifier
import ai.metabind.bindjs.model.modifier.ItalicModifier
import ai.metabind.bindjs.model.modifier.LayoutPriorityModifier
import ai.metabind.bindjs.model.modifier.LineLimitModifier
import ai.metabind.bindjs.model.modifier.LineSpacingModifier
import ai.metabind.bindjs.model.modifier.MaskModifier
import ai.metabind.bindjs.model.modifier.MonospacedModifier
import ai.metabind.bindjs.model.modifier.MultilineTextAlignmentModifier
import ai.metabind.bindjs.model.modifier.OffsetModifier
import ai.metabind.bindjs.model.modifier.OnAppearModifier
import ai.metabind.bindjs.model.modifier.OnDisappearModifier
import ai.metabind.bindjs.model.modifier.OnDragGestureModifier
import ai.metabind.bindjs.model.modifier.OnLongPressModifier
import ai.metabind.bindjs.model.modifier.OnTapModifier
import ai.metabind.bindjs.model.modifier.OpacityModifier
import ai.metabind.bindjs.model.modifier.OverlayModifier
import ai.metabind.bindjs.model.modifier.PaddingModifier
import ai.metabind.bindjs.model.modifier.PickerStyleModifier
import ai.metabind.bindjs.model.modifier.ResizableModifier
import ai.metabind.bindjs.model.modifier.RotationEffectModifier
import ai.metabind.bindjs.model.modifier.SaturationModifier
import ai.metabind.bindjs.model.modifier.ScaleEffectModifier
import ai.metabind.bindjs.model.modifier.ScaledToFillModifier
import ai.metabind.bindjs.model.modifier.ScaledToFitModifier
import ai.metabind.bindjs.model.modifier.ShadowModifier
import ai.metabind.bindjs.model.modifier.StrikethroughModifier
import ai.metabind.bindjs.model.modifier.TagModifier
import ai.metabind.bindjs.model.modifier.TextCaseModifier
import ai.metabind.bindjs.model.modifier.TextSelectionModifier
import ai.metabind.bindjs.model.modifier.TintModifier
import ai.metabind.bindjs.model.modifier.TrackingModifier
import ai.metabind.bindjs.model.modifier.TransformEffectModifier
import ai.metabind.bindjs.model.modifier.UnderlineModifier
import ai.metabind.bindjs.model.modifier.VisualEffectModifier
import ai.metabind.bindjs.model.modifier.ZIndexModifier
import ai.metabind.bindjs.model.modifier.chart.AnnotationModifier
import ai.metabind.bindjs.model.modifier.chart.ChartForegroundStyleScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartLegendModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartSymbolScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXAxisLabelModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXAxisModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartXSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYAxisLabelModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYAxisModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYScaleModifier
import ai.metabind.bindjs.model.modifier.chart.ChartYSelectionModifier
import ai.metabind.bindjs.model.modifier.chart.InterpolationMethodModifier
import ai.metabind.bindjs.model.modifier.chart.LineStyleModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolModifier
import ai.metabind.bindjs.model.modifier.chart.SymbolSizeModifier
import java.io.IOException

class GsonProvider {
    companion object {
        fun get(): Gson = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory
                    .of(ComponentModifier::class.java, EmptyModifier::class.java, "type")
                    .registerSubtype(OverlayModifier::class.java, "overlay")
                    .registerSubtype(EnvironmentModifier::class.java, "environment")
                    .registerSubtype(CornerRadiusModifier::class.java, "cornerRadius")
                    .registerSubtype(FontModifier::class.java, "font")
                    .registerSubtype(FontWeightModifier::class.java, "fontWeight")
                    .registerSubtype(BoldModifier::class.java, "bold")
                    .registerSubtype(ItalicModifier::class.java, "italic")
                    .registerSubtype(ForegroundStyleModifier::class.java, "foregroundStyle")
                    .registerSubtype(BackgroundModifier::class.java, "background")
                    .registerSubtype(FrameModifier::class.java, "frame")
                    .registerSubtype(ResizableModifier::class.java, "resizable")
                    .registerSubtype(OffsetModifier::class.java, "offset")
                    .registerSubtype(DisabledModifier::class.java, "disabled")
                    .registerSubtype(OnTapModifier::class.java, "onTapGesture")
                    .registerSubtype(OnLongPressModifier::class.java, "onLongPressGesture")
                    .registerSubtype(OnDragGestureModifier::class.java, "onDragGesture")
                    .registerSubtype(AllowsHitTestingModifier::class.java, "allowsHitTesting")
                    .registerSubtype(OpacityModifier::class.java, "opacity")
                    .registerSubtype(PaddingModifier::class.java, "padding")
                    .registerSubtype(ShadowModifier::class.java, "shadow")
                    .registerSubtype(TagModifier::class.java, "tag")
                    .registerSubtype(PickerStyleModifier::class.java, "pickerStyle")
                    .registerSubtype(BorderModifier::class.java, "border")
                    .registerSubtype(ClippedModifier::class.java, "clipped")
                    .registerSubtype(AspectRatioModifier::class.java, "aspectRatio")
                    .registerSubtype(FixedSizeModifier::class.java, "fixedSize")
                    .registerSubtype(LayoutPriorityModifier::class.java, "layoutPriority")
                    .registerSubtype(HiddenModifier::class.java, "hidden")
                    .registerSubtype(ScaledToFitModifier::class.java, "scaledToFit")
                    .registerSubtype(ScaledToFillModifier::class.java, "scaledToFill")
                    .registerSubtype(UnderlineModifier::class.java, "underline")
                    .registerSubtype(StrikethroughModifier::class.java, "strikethrough")
                    .registerSubtype(LineLimitModifier::class.java, "lineLimit")
                    .registerSubtype(LineSpacingModifier::class.java, "lineSpacing")
                    .registerSubtype(
                        MultilineTextAlignmentModifier::class.java,
                        "multilineTextAlignment"
                    )
                    .registerSubtype(TextCaseModifier::class.java, "textCase")
                    .registerSubtype(MonospacedModifier::class.java, "monospaced")
                    .registerSubtype(MaskModifier::class.java, "mask")
                    .registerSubtype(ContrastModifier::class.java, "contrast")
                    .registerSubtype(BlurModifier::class.java, "blur")
                    .registerSubtype(BrightnessModifier::class.java, "brightness")
                    .registerSubtype(SaturationModifier::class.java, "saturation")
                    .registerSubtype(GrayscaleModifier::class.java, "grayscale")
                    .registerSubtype(ColorInvertModifier::class.java, "colorInvert")
                    .registerSubtype(BlendModeModifier::class.java, "blendMode")
                    .registerSubtype(ButtonStyleModifier::class.java, "buttonStyle")
                    .registerSubtype(ColorSchemeModifier::class.java, "colorScheme")
                    .registerSubtype(ScaleEffectModifier::class.java, "scaleEffect")
                    .registerSubtype(RotationEffectModifier::class.java, "rotationEffect")
                    .registerSubtype(TransformEffectModifier::class.java, "transformEffect")
                    .registerSubtype(ZIndexModifier::class.java, "zIndex")
                    .registerSubtype(IDModifier::class.java, "id")
                    .registerSubtype(OnAppearModifier::class.java, "onAppear")
                    .registerSubtype(OnDisappearModifier::class.java, "onDisappear")
                    .registerSubtype(
                        AutocorrectionDisabledModifier::class.java,
                        "autocorrectionDisabled"
                    )
                    .registerSubtype(TextSelectionModifier::class.java, "textSelection")
                    .registerSubtype(TrackingModifier::class.java, "tracking")
                    .registerSubtype(AccessibilityLabelModifier::class.java, "accessibilityLabel")
                    .registerSubtype(AccessibilityHintModifier::class.java, "accessibilityHint")
                    .registerSubtype(AccessibilityValueModifier::class.java, "accessibilityValue")
                    .registerSubtype(AccessibilityHiddenModifier::class.java, "accessibilityHidden")
                    .registerSubtype(
                        AccessibilityRemoveTraitsModifier::class.java,
                        "accessibilityRemoveTraits"
                    )
                    .registerSubtype(ClipShapeModifier::class.java, "clipShape")
                    .registerSubtype(IgnoresSafeAreaModifier::class.java, "ignoresSafeArea")
                    .registerSubtype(FontDesignModifier::class.java, "fontDesign")
                    .registerSubtype(TintModifier::class.java, "tint")
                    .registerSubtype(CoordinateSpaceModifier::class.java, "coordinateSpace")
                    .registerSubtype(VisualEffectModifier::class.java, "visualEffect")
                    .registerSubtype(ContextMenuModifier::class.java, "contextMenu")
                    .registerSubtype(ChartXAxisModifier::class.java, "chartXAxis")
                    .registerSubtype(ChartYAxisModifier::class.java, "chartYAxis")
                    .registerSubtype(ChartXScaleModifier::class.java, "chartXScale")
                    .registerSubtype(ChartYScaleModifier::class.java, "chartYScale")
                    .registerSubtype(
                        ChartForegroundStyleScaleModifier::class.java,
                        "chartForegroundStyleScale"
                    )
                    .registerSubtype(ChartSymbolScaleModifier::class.java, "chartSymbolScale")
                    .registerSubtype(ChartSelectionModifier::class.java, "chartSelection")
                    .registerSubtype(ChartXSelectionModifier::class.java, "chartXSelection")
                    .registerSubtype(ChartYSelectionModifier::class.java, "chartYSelection")
                    .registerSubtype(ChartLegendModifier::class.java, "chartLegend")
                    .registerSubtype(ChartXAxisLabelModifier::class.java, "chartXAxisLabel")
                    .registerSubtype(ChartYAxisLabelModifier::class.java, "chartYAxisLabel")
                    .registerSubtype(LineStyleModifier::class.java, "lineStyle")
                    .registerSubtype(InterpolationMethodModifier::class.java, "interpolationMethod")
                    .registerSubtype(SymbolModifier::class.java, "symbol")
                    .registerSubtype(SymbolSizeModifier::class.java, "symbolSize")
                    .registerSubtype(AnnotationModifier::class.java, "annotation")
            )
            .registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory
                    .of(BaseComponent::class.java, EmptyComponent::class.java, "type")
                    .registerSubtype(Component::class.java, "ComponentCall")
                    .registerSubtype(ChartComponent::class.java, "Chart")
                    .registerSubtype(PieChartComponent::class.java, "PieChart")
                    .registerSubtype(BarMarkComponent::class.java, "BarMark")
                    .registerSubtype(LineMarkComponent::class.java, "LineMark")
                    .registerSubtype(AreaMarkComponent::class.java, "AreaMark")
                    .registerSubtype(PointMarkComponent::class.java, "PointMark")
                    .registerSubtype(RuleMarkComponent::class.java, "RuleMark")
                    .registerSubtype(RectangleMarkComponent::class.java, "RectangleMark")
                    .registerSubtype(PieSliceMarkComponent::class.java, "PieSliceMark")
                    .registerSubtype(ButtonComponent::class.java, "Button")
                    .registerSubtype(TextFieldComponent::class.java, "TextField")
                    .registerSubtype(LabelComponent::class.java, "Label")
                    .registerSubtype(CircleComponent::class.java, "Circle")
                    .registerSubtype(ColorComponent::class.java, "Color")
                    .registerSubtype(MaterialComponent::class.java, "Material")
                    .registerSubtype(ForEachComponent::class.java, "ForEach")
                    .registerSubtype(RowComponent::class.java, "HStack")
                    .registerSubtype(ImageComponent::class.java, "Image")
                    .registerSubtype(ModifiedComponent::class.java, "ModifiedComponent")
                    .registerSubtype(RectangleComponent::class.java, "Rectangle")
                    .registerSubtype(ScrollComponent::class.java, "ScrollView")
                    .registerSubtype(SectionComponent::class.java, "Section")
                    .registerSubtype(SpacerComponent::class.java, "Spacer")
                    .registerSubtype(TextComponent::class.java, "Text")
                    .registerSubtype(ToggleComponent::class.java, "Toggle")
                    .registerSubtype(ColumnComponent::class.java, "VStack")
                    .registerSubtype(BoxComponent::class.java, "ZStack")
                    .registerSubtype(GroupComponent::class.java, "Group")
                    .registerSubtype(DividerComponent::class.java, "Divider")
                    .registerSubtype(ProgressViewComponent::class.java, "ProgressView")
                    .registerSubtype(TextEditorComponent::class.java, "TextEditor")
                    .registerSubtype(RoundedRectangleComponent::class.java, "RoundedRectangle")
                    .registerSubtype(CapsuleComponent::class.java, "Capsule")
                    .registerSubtype(GeometryReaderComponent::class.java, "GeometryReader")
                    .registerSubtype(NavigationLinkComponent::class.java, "NavigationLink")
                    .registerSubtype(EllipseComponent::class.java, "Ellipse")
                    .registerSubtype(PickerComponent::class.java, "Picker")
                    .registerSubtype(Model3DComponent::class.java, "Model3D")
                    .registerSubtype(VideoComponent::class.java, "Video")
                    .registerSubtype(LinearGradientComponent::class.java, "LinearGradient")
                    .registerSubtype(AngularGradientComponent::class.java, "AngularGradient")
                    .registerSubtype(RadialGradientComponent::class.java, "RadialGradient")
                    .registerSubtype(EllipticalGradientComponent::class.java, "EllipticalGradient")
                    .registerSubtype(MenuComponent::class.java, "Menu")
            )
            .registerTypeAdapter(
                object : TypeToken<Float?>() {}.type,
                object : TypeAdapter<Float?>() {
                    @Throws(IOException::class)
                    override fun read(reader: JsonReader): Float? {
                        if (reader.peek() == JsonToken.STRING) {
                            val stringValue = reader.nextString()
                            if (stringValue.equals("Infinity", ignoreCase = true)) {
                                return Float.POSITIVE_INFINITY
                            }
                            if (stringValue.equals("-Infinity", ignoreCase = true)) {
                                return Float.NEGATIVE_INFINITY
                            }
                            if (stringValue.equals("NaN", ignoreCase = true)) {
                                return Float.NaN
                            }

                            return stringValue.toFloatOrNull()
                        }

                        return reader.nextDouble().toFloat()
                    }

                    @Throws(IOException::class)
                    override fun write(writer: JsonWriter, value: Float?) {
                        writer.value(value)
                    }
                })
            .create()
    }
}
