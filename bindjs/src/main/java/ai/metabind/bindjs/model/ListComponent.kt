package ai.metabind.bindjs.model

/**
 * A `List(...)`: a scrolling column of rows, optionally grouped into `Section`s, with an
 * optional single selection.
 *
 * ```js
 * List({ selection, setSelection }, [
 *   Section({ header: Text('Cities') }, [
 *     Text('Paris').tag('paris'),
 *     Text('Tokyo').tag('tokyo'),
 *   ]),
 * ]).listStyle('plain')
 * ```
 *
 * Rows come straight from the children (a `ForEach` is flattened into its rows); a
 * `Section` child contributes its header, rows and footer. A row is selectable when it
 * carries a `.tag(...)`, and tapping it hands that tag to the `setSelection` handler.
 * The look of the list is driven by the `listStyle` / `scrollContentBackground` modifiers
 * written on the list and by the `listRowBackground` / `listRowSeparator` modifiers
 * written on a row; see `ListView`.
 */
class ListComponent(
    props: ListProps,
) : BaseComponent<ListProps>(props)

class ListProps(
    /** The tag of the selected row, or null when nothing is selected. */
    val selection: String?,
    /** Handler id of the `setSelection` closure; null makes the rows plain content. */
    val setSelectionId: String?,
    /** Environment to restore before calling the handler; blank or null for none. */
    val environmentId: String?,
    children: List<BaseComponent<*>?>?,
) : Props(children = children) {
    override fun toString(): String {
        return "ListProps(selection=$selection, setSelectionId=$setSelectionId, environmentId=$environmentId)"
    }
}
