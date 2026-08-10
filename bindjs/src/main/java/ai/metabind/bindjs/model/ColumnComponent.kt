package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.props.LayoutProps

open class ColumnComponent(
    props: LayoutProps,
) : BaseComponent<LayoutProps>(props)

/**
 * SwiftUI's `LazyVStack`. It lays out identically to a [ColumnComponent] here — the
 * laziness comes from the enclosing BindJS `ScrollView`, which renders as a LazyColumn
 * — so it subclasses it and every `is ColumnComponent` dispatch picks it up unchanged.
 * It needs its own class purely because `RuntimeTypeAdapterFactory` maps one label to
 * one class, and an unregistered label silently decodes to `EmptyComponent`.
 */
class LazyColumnComponent(
    props: LayoutProps,
) : ColumnComponent(props)
