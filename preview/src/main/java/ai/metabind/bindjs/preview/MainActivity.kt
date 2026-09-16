package ai.metabind.bindjs.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.metabind.bindjs.DesignerComponent
import ai.metabind.bindjs.JsRuntime
import ai.metabind.bindjs.JsRuntimeImpl
import ai.metabind.bindjs.composables.BindJSView
import ai.metabind.bindjs.composables.routeUiEvent
import ai.metabind.bindjs.model.BaseComponent
import kotlinx.coroutines.launch

/**
 * The BindJS preview app. The drawer picks a [PreviewSection]; the section screen lists
 * its fixtures; tapping one renders it through the real runtime, with back returning to
 * the list.
 *
 * Launch straight into a fixture with `--es fixture <name>`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PreviewApp(initialFixtureName = intent.getStringExtra("fixture"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewApp(initialFixtureName: String?) {
    val context = LocalContext.current
    val runtime = remember { JsRuntimeImpl.getInstance(context) }
    val initialFixture = remember(initialFixtureName) {
        allFixtures.firstOrNull { it.name == initialFixtureName }
    }
    var section by remember { mutableStateOf(initialFixture?.section ?: PreviewSection.entries.first()) }
    var fixture by remember { mutableStateOf(initialFixture) }
    var componentsRegistered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runtime.awaitReady()
        for (item in allFixtures) {
            runtime.setComponents(
                DesignerComponent(
                    name = item.componentName,
                    content = item.componentSource()
                )
            )
        }
        componentsRegistered = true
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // System back walks the same way the arrow does: fixture → list. On the list it
    // falls through to the system and leaves the app.
    BackHandler(enabled = fixture != null) { fixture = null }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SectionDrawer(
                selected = section,
                onSelect = { chosen ->
                    section = chosen
                    fixture = null
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(fixture?.name ?: section.title) },
                    navigationIcon = {
                        val current = fixture
                        if (current == null) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open sections")
                            }
                        } else {
                            IconButton(onClick = { fixture = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to ${section.title}")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val current = fixture
                if (current == null) {
                    FixtureList(
                        fixtures = allFixtures.inSection(section),
                        onSelect = { fixture = it }
                    )
                } else {
                    FixtureScreen(
                        runtime = runtime,
                        fixture = current,
                        componentsRegistered = componentsRegistered,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionDrawer(
    selected: PreviewSection,
    onSelect: (PreviewSection) -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "BindJS Preview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
        )
        PreviewSection.entries.forEach { section ->
            NavigationDrawerItem(
                label = { Text(section.title) },
                selected = section == selected,
                onClick = { onSelect(section) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun FixtureList(
    fixtures: List<PreviewFixture>,
    onSelect: (PreviewFixture) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(fixtures, key = { it.name }) { fixture ->
            ListItem(
                headlineContent = { Text(fixture.name) },
                supportingContent = { Text(fixture.description) },
                modifier = Modifier.clickable { onSelect(fixture) }
            )
            HorizontalDivider()
        }
    }
}

/**
 * Renders one fixture and keeps it live: an event reaches its JS handler, the handler's
 * `setState` asks for a re-render, and the body runs again. Without that loop a slider
 * can never move and a chart selection never highlights.
 */
@Composable
private fun FixtureScreen(
    runtime: JsRuntime,
    fixture: PreviewFixture,
    componentsRegistered: Boolean,
) {
    var component by remember(fixture) { mutableStateOf<BaseComponent<*>?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var error by remember(fixture) { mutableStateOf<String?>(null) }

    suspend fun render() {
        try {
            runtime.awaitReady()
            runtime.willRender()
            component = runtime.callComponent(fixture.componentName)
            version += 1
            error = null
        } catch (throwable: Throwable) {
            component = null
            error = throwable.message ?: throwable::class.java.simpleName
        }
    }

    LaunchedEffect(fixture, componentsRegistered) {
        if (!componentsRegistered) return@LaunchedEffect
        render()
    }

    val scope = rememberCoroutineScope()
    DisposableEffect(runtime, fixture) {
        runtime.setOnRerenderRequested { scope.launch { render() } }
        onDispose { runtime.setOnRerenderRequested(null) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = fixture.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
            // A chart is a fixed-height block that reads best centred; a component
            // fixture is a column of controls that reads top-down.
            contentAlignment = if (fixture.section == PreviewSection.Charts) {
                Alignment.Center
            } else {
                Alignment.TopStart
            }
        ) {
            when {
                error != null -> Text(
                    "Render error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )

                component == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                else -> WithPreviewComponents {
                    BindJSView(
                        jsRuntime = runtime,
                        component = component!!,
                        version = version,
                        onUiEvent = { event ->
                            scope.launch { runtime.routeUiEvent(event) }
                        },
                    )
                }
            }
        }
        Text(
            text = "Rendered through JsRuntimeImpl and Jetpack Compose.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
