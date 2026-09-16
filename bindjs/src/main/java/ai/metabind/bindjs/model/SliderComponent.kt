package ai.metabind.bindjs.model

class SliderComponent(
    props: SliderProps,
) : BaseComponent<SliderProps>(props)

/**
 * `Slider({ value, setValue, range, step, label, minimumValueLabel, maximumValueLabel })`.
 *
 * `setValue` has already been turned into [setValueId] by the runtime's `processProps`,
 * the same way `setIsOn` reaches [ToggleComponentProps]. [range] is the `[lower, upper]`
 * pair the thumb moves between and defaults to `[0, 1]` when absent. A [step] makes the
 * slider discrete; without one it is continuous. The two value labels arrive as nested
 * components, so `Text("0").font("caption")` keeps its styling.
 */
class SliderProps(
    val value: Double? = null,
    val setValueId: String? = null,
    val range: List<Double>? = null,
    val step: Double? = null,
    val label: String? = null,
    val minimumValueLabel: BaseComponent<*>? = null,
    val maximumValueLabel: BaseComponent<*>? = null,
    val environmentId: String? = null,
) : Props(children = emptyList())
