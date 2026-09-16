package ai.metabind.bindjs.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.composables.ComponentRepresentable
import ai.metabind.bindjs.composables.ComponentRepresentableContext
import ai.metabind.bindjs.composables.WithComponent

/**
 * The native components the preview app registers, so a fixture can show a component
 * call or a `Placeholder` resolving to a composable the host supplies. Wraps the
 * renderer in the app and in the screenshot tests alike.
 */
@Composable
fun WithPreviewComponents(content: @Composable () -> Unit) {
    WithComponent(PreviewBadge.NAME, PreviewBadge, content)
}

/** A chip drawn in Compose; `label` and `count` come from the props of the component call. */
object PreviewBadge : ComponentRepresentable {
    const val NAME = "PreviewBadge"

    @Composable
    override fun Content(context: ComponentRepresentableContext) {
        val label = context.props["label"] as? String ?: NAME
        val count = (context.props["count"] as? Number)?.toInt()

        Row(
            modifier = Modifier
                .background(Color(0xFF1E88E5), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
            if (count != null) {
                Text(
                    text = count.toString(),
                    color = Color(0xFF1E88E5),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}
