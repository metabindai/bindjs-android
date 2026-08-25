package ai.metabind.bindjs.model

import ai.metabind.bindjs.model.props.LayoutProps

/**
 * `NavigationStack(content)`. Android has no navigation-stack primitive to hand
 * this to, so `NavigationStackView` renders the content directly and draws the
 * bar itself from the `navigationTitle` / `toolbar` modifiers on its child.
 * `NavigationLink` pushes are handled by the embedder (see `UiEvent.OnNavigationTap`).
 */
class NavigationStackComponent(
    props: LayoutProps,
) : BaseComponent<LayoutProps>(props)
