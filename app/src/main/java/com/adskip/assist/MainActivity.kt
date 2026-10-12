package com.adskip.assist

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adskip.assist.automation.AccessibilityAutomationService
import com.adskip.assist.automation.LearnedTargetStore
import com.adskip.assist.automation.TeachCaptureStore
import com.adskip.assist.data.ActivityLogRepository
import com.adskip.assist.data.SettingsRepository
import com.adskip.assist.data.StatisticsRepository
import com.adskip.assist.ui.MainViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.min

/** Design direction: quiet arcade instrument panel, not a generic card dashboard. */
private val midnight = Color(0xFF0C171B)
private val field = Color(0xFF14252A)
private val raised = Color(0xFF1D3236)
private val lime = Color(0xFFD9F96B)
private val pale = Color(0xFFF0F4EC)
private val fog = Color(0xFFA8BCB9)
private val aqua = Color(0xFF7DD9D3)
private val red = Color(0xFFFF9B8B)
private val scheme = darkColorScheme(
    primary = lime, onPrimary = midnight, background = midnight, onBackground = pale,
    surface = field, onSurface = pale, surfaceVariant = raised, onSurfaceVariant = fog,
    secondary = aqua, onSecondary = midnight, outline = Color(0xFF345058), error = red
)
private val edge = RoundedCornerShape(22.dp)

class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = model.settingsRepository
        val logs = model.logRepository
        val apps = installedLaunchableApps()
        setContent {
            MaterialTheme(colorScheme = scheme) {
                val config by model.settings.collectAsState()
                val records by model.activity.collectAsState()
                val stats by model.statistics.collectAsState()
                var tab by remember { mutableIntStateOf(0) }
                var resumeRevision by remember { mutableIntStateOf(0) }
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val obs = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
                        resumeRevision++; settings.refresh()
                    } }
                    owner.lifecycle.addObserver(obs)
                    onDispose { owner.lifecycle.removeObserver(obs) }
                }
                val serviceActive = remember(resumeRevision) { accessibilityEnabled() }
                val captured = remember(resumeRevision) { TeachCaptureStore.captured }
                val capturedGame = remember(resumeRevision) { TeachCaptureStore.capturedPackage }
                if (captured != null && capturedGame != null) {
                    LearnDialog(captured, capturedGame,
                        onSave = { x, y ->
                            LearnedTargetStore(this).save(capturedGame, captured, x, y)
                            TeachCaptureStore.dismiss()
                            resumeRevision++
                        },
                        onDismiss = { TeachCaptureStore.dismiss(); resumeRevision++ })
                }
                Scaffold(
                    containerColor = midnight,
                    topBar = {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("A D S K I P   /   A S S I S T", color = lime,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(listOf("Overview", "Your games", "Activity", "Preferences")[tab],
                                    fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp)
                            }
                            Box(Modifier.size(43.dp).clip(CircleShape).background(field), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Shield, null, tint = lime)
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(containerColor = field, tonalElevation = 0.dp) {
                            listOf(Triple("Home", Icons.Rounded.Home, 0), Triple("Games", Icons.Rounded.SportsEsports, 1),
                                Triple("Activity", Icons.Rounded.History, 2), Triple("Settings", Icons.Rounded.Tune, 3)).forEach { (name, icon, idx) ->
                                NavigationBarItem(selected = tab == idx, onClick = { tab = idx },
                                    icon = { Icon(icon, null) }, label = { Text(name) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = midnight,
                                        selectedTextColor = lime, indicatorColor = lime, unselectedIconColor = fog, unselectedTextColor = fog))
                            }
                        }
                    }
                ) { pad ->
                    val modifier = Modifier.fillMaxSize().padding(pad)
                    when (tab) {
                        0 -> Overview(modifier, config, serviceActive, stats, settings,
                            { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, { tab = 1 }, { tab = 3 })
                        1 -> Games(modifier, config, apps, settings, ::openGame, serviceActive, resumeRevision)
                        2 -> History(modifier, records, logs)
                        else -> Preferences(modifier, config, settings,
                            { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                    }
                }
            }
        }
    }
    private fun accessibilityEnabled(): Boolean {
        val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val component = ComponentName(this, AccessibilityAutomationService::class.java).flattenToString()
        return services.split(':').any { it.equals(component, ignoreCase = true) }
    }
    private fun openGame(game: String) {
        val launch = packageManager.getLaunchIntentForPackage(game) ?: return
        startActivity(launch)
    }
    private fun installedLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = packageManager
        val result = if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        else { @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0) }
        return result.mapNotNull {
            val app = it.activityInfo?.applicationInfo ?: return@mapNotNull null
            if (app.packageName == packageName) return@mapNotNull null
            InstalledApp(it.loadLabel(pm).toString(), app.packageName, app.category == ApplicationInfo.CATEGORY_GAME)
        }.distinctBy { it.pkg }.sortedWith(compareByDescending<InstalledApp> { it.isGame }.thenBy { it.name.lowercase() })
    }
}

private data class InstalledApp(val name: String, val pkg: String, val isGame: Boolean)

@Composable private fun Eyebrow(text: String, color: Color = fog) {
    Text(text.uppercase(), color = color, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
}
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(edge).background(field).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable private fun Header(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-.35).sp)
        if (description != null) Text(description, color = fog, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
@Composable private fun ToggleRow(title: String, note: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(note, color = fog, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = midnight, checkedTrackColor = lime))
    }
}
@Composable private fun ActiveDial(active: Boolean) {
    Box(Modifier.size(117.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF325054), style = Stroke(width = 7.dp.toPx()))
            drawArc(color = if (active) lime else fog, startAngle = -95f, sweepAngle = if (active) 310f else 80f,
                useCenter = false, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color = if (active) lime.copy(alpha = .09f) else fog.copy(alpha = .07f), radius = size.minDimension * .37f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (active) Icons.Rounded.Check else Icons.Rounded.Pause, null,
                tint = if (active) lime else fog, modifier = Modifier.size(31.dp))
            Spacer(Modifier.height(4.dp))
            Text(if (active) "ON AIR" else "STANDBY", color = if (active) lime else fog,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}
@Composable private fun Overview(modifier: Modifier, cfg: SettingsRepository.Settings, service: Boolean,
    stats: StatisticsRepository.Statistics, settings: SettingsRepository, openAccess: () -> Unit, goGames: () -> Unit, goSettings: () -> Unit) {
    val active = cfg.enabled && cfg.consent && service && cfg.allowlist.isNotEmpty()
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Quiet gaming starts here", lime)
                Text("Ads end.\nYou play.", fontSize = 39.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 41.sp, letterSpacing = (-1.8).sp)
                Text("Hands-free ad dismissal, on your terms.", color = fog,
                    fontSize = 12.sp, lineHeight = 18.sp)
            }
            ActiveDial(active)
        }
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (active) lime else red))
                Spacer(Modifier.width(9.dp))
                Eyebrow(if (active) "Monitoring ${cfg.allowlist.size} game(s)" else "Automation paused", if (active) lime else red)
            }
            Text(when {
                !cfg.consent -> "Start by reviewing the permission disclosure."
                !service -> "Android accessibility access is not enabled."
                cfg.allowlist.isEmpty() -> "Choose which games the assistant may monitor."
                !cfg.enabled -> "Ready when you are. Nothing is being tapped."
                else -> "Ad-aware automation is running only in approved games."
            }, color = pale, fontSize = 14.sp, lineHeight = 21.sp)
            Button(onClick = { settings.setEnabled(!cfg.enabled) },
                enabled = cfg.consent && service && cfg.allowlist.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (active) raised else lime,
                    contentColor = if (active) pale else midnight)) {
                Icon(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(9.dp))
                Text(if (active) "Pause automation" else "Start monitoring", fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Eyebrow("Your activity / local device")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(stats.adsDetected.toString(), "Ad sessions", Modifier.weight(1f))
                Metric(stats.actionsSucceeded.toString(), "Taps attempted*", Modifier.weight(1f))
            }
            Text("*Automation actions accepted by Android, not confirmed ad closures or game rewards.",
                color = fog, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Eyebrow("Choose your pace")
            ModeSwitch(cfg.mode, settings::setMode)
            Text("Reward Priority uses timing and available completion hints. It cannot verify that a game will grant a reward.",
                fontSize = 11.sp, lineHeight = 17.sp, color = fog)
        }
        Panel(Modifier.fillMaxWidth()) {
            Eyebrow("Ready in three steps", lime)
            StepItem("01", "Grant access", if (cfg.consent && service) "Done" else "Needs setup", if (cfg.consent) openAccess else goSettings)
            HorizontalDivider(color = raised)
            StepItem("02", "Choose games", if (cfg.allowlist.isNotEmpty()) "${cfg.allowlist.size} selected" else "No games yet", goGames)
            HorizontalDivider(color = raised)
            StepItem("03", "Tune detection", if (cfg.visualDetection) "Visual mode ready" else "Optional", goSettings)
        }
    }
}
@Composable private fun StepItem(number: String, label: String, detail: String, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(number, fontFamily = FontFamily.Monospace, color = lime, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium); Text(detail, color = fog, fontSize = 11.sp) }
        Icon(Icons.Rounded.ChevronRight, null, tint = fog)
    }
}
@Composable private fun Metric(value: String, title: String, modifier: Modifier) {
    Column(modifier.border(1.dp, Color(0xFF2A4349), RoundedCornerShape(15.dp)).padding(17.dp)) {
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.Medium, color = pale)
        Text(title, fontSize = 11.sp, color = fog)
    }
}
@Composable private fun ModeSwitch(mode: SettingsRepository.Mode, select: (SettingsRepository.Mode) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(field).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(SettingsRepository.Mode.REWARD_PRIORITY to "Reward priority", SettingsRepository.Mode.FAST_SKIP to "Fast skip").forEach { (value, label) ->
            val active = mode == value
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (active) lime else Color.Transparent)
                .clickable { select(value) }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (active) midnight else fog, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
@Composable private fun Games(modifier: Modifier, cfg: SettingsRepository.Settings, apps: List<InstalledApp>,
    settings: SettingsRepository, openGame: (String) -> Unit, serviceActive: Boolean, resumeRevision: Int) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val matches = remember(apps, query, showAll, cfg.allowlist) {
        apps.filter { (showAll || it.isGame || it.pkg in cfg.allowlist) && (it.name.contains(query, true) || it.pkg.contains(query, true)) }
    }
    LazyColumn(modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Only you decide which apps we touch.", color = fog, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text("Find a game") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(15.dp))
            ToggleRow("Show all installed apps", "Include apps not classified as games", showAll, { showAll = it })
            Eyebrow("${cfg.allowlist.size} approved • ${matches.size} shown", lime)
            Spacer(Modifier.height(5.dp))
        }
        if (matches.isEmpty()) item { Panel(Modifier.fillMaxWidth()) { Text("No matching apps found.", color = fog) } }
        items(matches, key = { it.pkg }) { app ->
            val selected = app.pkg in cfg.allowlist
            val trained = remember(app.pkg, resumeRevision) { LearnedTargetStore(context).exists(app.pkg) }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(45.dp).clip(RoundedCornerShape(13.dp)).background(raised), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = if (selected) lime else fog)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.pkg, fontSize = 10.sp, color = fog, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(selected, { settings.setGameEnabled(app.pkg, it) })
                }
                if (selected) {
                    HorizontalDivider(color = raised)
                    val ownMode = settings.modeForGame(app.pkg)
                    TextButton(onClick = {
                        settings.setGameMode(app.pkg, when(ownMode) {
                            null -> SettingsRepository.Mode.REWARD_PRIORITY
                            SettingsRepository.Mode.REWARD_PRIORITY -> SettingsRepository.Mode.FAST_SKIP
                            SettingsRepository.Mode.FAST_SKIP -> null
                        })
                    }) { Text("Mode: ${ownMode?.name?.replace('_',' ')?.lowercase() ?: "global default"}") }
                    val wait = settings.waitForGame(app.pkg) ?: cfg.minimumWaitSeconds
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Minimum wait", fontSize = 12.sp, color = fog, modifier = Modifier.weight(1f))
                        Text("${wait}s", fontWeight = FontWeight.SemiBold, color = lime)
                    }
                    Slider(wait.toFloat(), { settings.setGameWait(app.pkg, it.toInt()) }, valueRange = 5f..120f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (trained) "Learned X saved" else "Teach a close button", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Open the game, tap floating CAPTURE AD, then return here.", color = fog, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 30 && cfg.consent && serviceActive) {
                                TeachCaptureStore.requestedPackage = app.pkg
                                openGame(app.pkg)
                            }
                        }, enabled = Build.VERSION.SDK_INT >= 30 && cfg.consent && serviceActive) {
                            Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Teach")
                        }
                    }
                    if (trained) TextButton(onClick = { LearnedTargetStore(context).delete(app.pkg) }) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Remove learned button")
                    }
                }
            }
        }
    }
}
@Composable private fun History(modifier: Modifier, entries: List<ActivityLogRepository.Entry>, repo: ActivityLogRepository) {
    var query by remember { mutableStateOf("") }
    val visible = remember(entries, query) { entries.filter { it.packageName.contains(query, true) || it.message.contains(query, true) } }
    LazyColumn(modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Eyebrow("A private log", lime); Text("Only action summaries stay on this device.", fontSize = 12.sp, color = fog) }
                TextButton(onClick = repo::clear) { Text("Clear all") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(15.dp), leadingIcon = { Icon(Icons.Rounded.Search, null) }, label = { Text("Filter events") })
            Spacer(Modifier.height(5.dp))
        }
        if (visible.isEmpty()) item { Panel(Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.History, null, tint = lime, modifier = Modifier.size(32.dp))
            Header("Nothing here yet", "Once the assistant spots an ad, relevant actions will appear here. Rewards cannot be independently verified.")
        } }
        items(visible) { event ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(11.dp).padding(top = 3.dp).clip(CircleShape).background(aqua))
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(event.message, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(event.packageName, color = fog, fontSize = 10.sp)
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.timestamp)),
                        color = fog, fontSize = 10.sp)
                    HorizontalDivider(color = raised, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
@Composable private fun Preferences(modifier: Modifier, cfg: SettingsRepository.Settings, settings: SettingsRepository,
    openAccessibility: () -> Unit) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Eyebrow("01 / Privacy & access", lime)
            Header("You are in control", "Accessibility access is powerful. AdSkip only acts inside games you explicitly approve, plus optional short Play Store recovery.")
            Text("With consent, the service can inspect selected games' controls, read on-device screenshot text, and tap detected dismissal targets. Full captures are memory-only during Teach mode. Small learned button crops and local action summaries stay on your phone. No accessibility data is transmitted.",
                color = fog, fontSize = 12.sp, lineHeight = 19.sp)
            ToggleRow("I understand and consent", "Required for the service to act", cfg.consent, settings::setConsent)
            OutlinedButton(onClick = openAccessibility, enabled = cfg.consent, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Settings, null); Spacer(Modifier.width(8.dp)); Text("Android accessibility settings")
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Eyebrow("02 / Recognition", lime)
            Header("Smarter detection", "Stay conservative by default. Turn on visual options only if needed.")
            ToggleRow("Read ad screens with OCR", "On-device ML Kit; screenshot availability varies by Android and ad", cfg.visualDetection, settings::setVisualDetection)
            HorizontalDivider(color = raised)
            ToggleRow("Use taught close buttons", "Opt-in template matching from your saved button crops", cfg.learnedTargetsEnabled, settings::setLearnedTargetsEnabled)
            HorizontalDivider(color = raised)
            ToggleRow("Extended ad signals", "Recognize weaker ad badges; more false-positive risk", cfg.extendedDetection, settings::setExtendedDetection)
        }
        Panel(Modifier.fillMaxWidth()) {
            Eyebrow("03 / Behavior", lime)
            ModeSwitch(cfg.mode, settings::setMode)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Default minimum watch", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text("${cfg.minimumWaitSeconds} seconds", color = lime, fontSize = 12.sp)
            }
            Slider(cfg.minimumWaitSeconds.toFloat(), { settings.setWaitSeconds(it.toInt()) }, valueRange = 5f..120f)
            Text("A timer is not proof that a reward was earned. Early closure can lose rewards.", color = fog, fontSize = 11.sp, lineHeight = 17.sp)
            HorizontalDivider(color = raised)
            ToggleRow("Play Store redirect recovery", "Opt-in BACK only for short-lived verified ad sessions", cfg.storeRecovery, settings::setStoreRecovery)
            HorizontalDivider(color = raised)
            ToggleRow("Store local event history", "Package names and short event descriptions only", cfg.loggingEnabled, settings::setLoggingEnabled)
        }
        Button(onClick = { settings.setEnabled(false); TeachCaptureStore.dismiss() },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F2728), contentColor = red)) {
            Icon(Icons.Rounded.PauseCircle, null); Spacer(Modifier.width(8.dp)); Text("Emergency stop", fontWeight = FontWeight.Bold)
        }
        Text("AdSkip Assist v0.2 • Private by design • No reward spoofing", textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = fog, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
    }
}
@Composable private fun LearnDialog(image: Bitmap, game: String, onSave: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var selection by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val valid = selection?.let { LearnedTargetStore.inCorner(it.first, it.second, image.width, image.height) &&
        it.first >= 48 && it.second >= 48 && it.first+48 < image.width && it.second+48 < image.height } ?: false
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(field).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Eyebrow("Teach mode / local capture", lime)
            Header("Tap the X button", "Select an actual close or skip control near a corner. Only a 96×96 pixel thumbnail is saved.")
            Image(image.asImageBitmap(), null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(12.dp)).background(midnight)
                    .onSizeChanged { size = it }
                    .pointerInput(image, size) {
                        detectTapGestures { point ->
                            val scale = min(size.width.toFloat()/image.width, size.height.toFloat()/image.height)
                            val usedW = image.width*scale; val usedH = image.height*scale
                            val x = ((point.x-(size.width-usedW)/2)/scale).toInt()
                            val y = ((point.y-(size.height-usedH)/2)/scale).toInt()
                            if (x in 0 until image.width && y in 0 until image.height) selection = x to y
                        }
                    })
            Text(if (selection == null) "Tap a close button on the image."
                else if (!valid) "Choose a corner control, away from screen edges."
                else "Target selected — ready to save.", color = if (valid) lime else fog, fontSize = 12.sp)
            Button(onClick = { selection?.let { onSave(it.first, it.second) } }, enabled = valid,
                modifier = Modifier.fillMaxWidth()) { Text("Save learned button") }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Discard screenshot") }
        }
    }
}
