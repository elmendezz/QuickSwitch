package com.elmendezz.qsre

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.elmendezz.qsre.ui.theme.QuickSwitchRevivedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val status: LauncherStatus
)

enum class LauncherStatus {
    IN_USE, AVAILABLE, INCOMPATIBLE
}

data class ModuleInfo(
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val description: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickSwitchRevivedTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // SharedPreferences persistentes
    val prefs = remember { context.getSharedPreferences("qsre_prefs", Context.MODE_PRIVATE) }
    
    var quickswitchPath by remember { 
        mutableStateOf(prefs.getString("quickswitch_path", "/data/adb/modules/quickswitch/quickswitch") ?: "/data/adb/modules/quickswitch/quickswitch") 
    }
    var autoReboot by remember { 
        mutableStateOf(prefs.getBoolean("auto_reboot", false)) 
    }
    
    var moduleInfo by remember { 
        mutableStateOf(ModuleInfo(name = "QuickSwitch", version = "...", author = "...")) 
    }
    var launchers by remember { mutableStateOf<List<LauncherInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var lastCommandOutput by remember { mutableStateOf("") }
    
    var showLogsDialog by remember { mutableStateOf(false) }
    var showOutputDialog by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }

    val excludedPackages = setOf("com.android.settings", "com.google.android.settings")

    fun reboot() {
        ShellHelper.runAsRoot("reboot")
    }

    fun refreshLaunchers() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val moduleDir = quickswitchPath.substringBeforeLast("/")
            val modulePropPath = "$moduleDir/module.prop"
            
            // 1. CARGA RÁPIDA: Info del módulo primero
            val propRes = ShellHelper.runAsRoot("cat $modulePropPath")
            val props = if (propRes.exitCode == 0) {
                propRes.stdout.lines().associate { 
                    it.substringBefore("=") to it.substringAfter("=", "")
                }
            } else emptyMap()

            withContext(Dispatchers.Main) {
                moduleInfo = ModuleInfo(
                    name = props["name"] ?: "QuickSwitch",
                    version = props["version"] ?: "v1.0",
                    author = props["author"] ?: "Lawnchair Team",
                    description = props["description"] ?: ""
                )
            }
            
            // 2. Obtener el proveedor activo
            val activeResult = ShellHelper.runAsRoot("grep '^description=' $modulePropPath")
            val description = activeResult.stdout
            val activePkg = if (description.contains("[ Quickstep :")) {
                val content = description.substringAfter("[ Quickstep :").substringBefore("]").trim()
                if (content.contains("Default", ignoreCase = true)) {
                    "com.android.launcher3"
                } else {
                    val packageRegex = Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")
                    packageRegex.find(content)?.value ?: ""
                }
            } else ""

            // 3. CARGA DE APPS: Listado de launchers
            val listCommand = "pm list packages -e --user 0 | cut -d: -f2"
            val listResult = ShellHelper.runAsRoot(listCommand)
            val pkgNames = listResult.stdout.lines().map { it.trim() }.filter { it.isNotEmpty() }
            
            val pm = context.packageManager
            val foundLaunchers = mutableListOf<LauncherInfo>()
            
            pkgNames.forEach { pkg ->
                if (pkg !in excludedPackages) {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            setPackage(pkg)
                        }
                        val isLauncher = pm.queryIntentActivities(intent, 0).isNotEmpty()
                        
                        if (isLauncher || pkg == activePkg) {
                            val info = pm.getPackageInfo(pkg, 0)
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            foundLaunchers.add(LauncherInfo(
                                name = pm.getApplicationLabel(appInfo).toString(),
                                packageName = pkg,
                                version = info.versionName ?: "N/A",
                                status = if (pkg == activePkg) LauncherStatus.IN_USE else LauncherStatus.AVAILABLE
                            ))
                        }
                    } catch (e: Exception) {}
                }
            }

            val parsed = foundLaunchers.sortedByDescending { it.status == LauncherStatus.IN_USE }

            withContext(Dispatchers.Main) {
                launchers = parsed
                lastCommandOutput = "Activo: $activePkg\nApps encontradas: ${parsed.size}"
                isLoading = false
            }
        }
    }

    fun executeSwitch(pkg: String) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val result = ShellHelper.runAsRoot("sh $quickswitchPath --ch=$pkg")
            withContext(Dispatchers.Main) {
                lastCommandOutput = result.stdout + "\n" + result.stderr
                isLoading = false
                showOutputDialog = true
            }
        }
    }

    fun executeReset() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val result = ShellHelper.runAsRoot("sh $quickswitchPath --reset")
            withContext(Dispatchers.Main) {
                lastCommandOutput = result.stdout + "\n" + result.stderr
                isLoading = false
                showOutputDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLaunchers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showRebootDialog = true }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = stringResource(R.string.menu_reboot_now))
                    }
                    IconButton(onClick = { refreshLaunchers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.menu_refresh))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stringResource(R.string.menu_auto_reboot))
                                        Checkbox(checked = autoReboot, onCheckedChange = null)
                                    }
                                },
                                onClick = { 
                                    autoReboot = !autoReboot
                                    prefs.edit().putBoolean("auto_reboot", autoReboot).apply()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_view_logs)) },
                                onClick = { showLogsDialog = true; showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_reset)) },
                                onClick = { executeReset(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_change_path)) },
                                onClick = { showPathDialog = true; showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_set_default)) },
                                onClick = { 
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_about)) },
                                onClick = { 
                                    context.startActivity(Intent(context, AboutMeActivity::class.java))
                                    showMenu = false 
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val inUse = launchers.filter { it.status == LauncherStatus.IN_USE }
            val available = launchers.filter { it.status == LauncherStatus.AVAILABLE }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ModuleInfoCard(moduleInfo)
                }

                if (inUse.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_in_use)) }
                    items(inUse) { LauncherItem(it) { pkg -> executeSwitch(pkg) } }
                }
                if (available.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_available)) }
                    items(available) { LauncherItem(it) { pkg -> executeSwitch(pkg) } }
                }
            }

            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (showLogsDialog) {
            AlertDialog(
                onDismissRequest = { showLogsDialog = false },
                title = { Text(stringResource(R.string.dialog_logs_title)) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        Text(lastCommandOutput, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { showLogsDialog = false }) { Text(stringResource(R.string.dialog_close)) } }
            )
        }

        if (showOutputDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showOutputDialog = false
                    if (autoReboot) reboot() else showRebootDialog = true
                },
                title = { Text(stringResource(R.string.dialog_result_title)) },
                text = { Text(lastCommandOutput) },
                confirmButton = {
                    TextButton(onClick = { 
                        showOutputDialog = false
                        if (autoReboot) reboot() else showRebootDialog = true
                    }) { Text(stringResource(R.string.dialog_continue)) }
                }
            )
        }

        if (showRebootDialog) {
            AlertDialog(
                onDismissRequest = { showRebootDialog = false },
                title = { Text(stringResource(R.string.dialog_reboot_title)) },
                text = { Text(stringResource(R.string.dialog_reboot_msg)) },
                confirmButton = { Button(onClick = { reboot() }) { Text(stringResource(R.string.dialog_reboot_confirm)) } },
                dismissButton = { TextButton(onClick = { showRebootDialog = false }) { Text(stringResource(R.string.dialog_reboot_later)) } }
            )
        }

        if (showPathDialog) {
            var tempPath by remember { mutableStateOf(quickswitchPath) }
            AlertDialog(
                onDismissRequest = { showPathDialog = false },
                title = { Text(stringResource(R.string.dialog_path_title)) },
                text = { TextField(value = tempPath, onValueChange = { tempPath = it }) },
                confirmButton = {
                    Button(onClick = { 
                        quickswitchPath = tempPath
                        prefs.edit().putString("quickswitch_path", quickswitchPath).apply()
                        showPathDialog = false
                        refreshLaunchers() 
                    }) { Text(stringResource(R.string.dialog_save)) }
                }
            )
        }
    }
}

@Composable
fun ModuleInfoCard(info: ModuleInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(text = info.name.ifEmpty { stringResource(R.string.unknown) }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(text = stringResource(R.string.module_version, info.version.ifEmpty { stringResource(R.string.unknown_desc) }), style = MaterialTheme.typography.bodyMedium)
            Text(text = stringResource(R.string.module_author, info.author.ifEmpty { stringResource(R.string.unknown) }), style = MaterialTheme.typography.bodyMedium)
            if (info.description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
fun LauncherItem(launcher: LauncherInfo, onClick: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val icon = remember(launcher.packageName) {
        try {
            pm.getApplicationIcon(launcher.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick(launcher.packageName) },
        colors = CardDefaults.cardColors(
            containerColor = if (launcher.status == LauncherStatus.IN_USE) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            } else {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = launcher.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (launcher.status == LauncherStatus.IN_USE) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.launcher_active_tag), fontSize = 10.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Text(text = launcher.packageName, style = MaterialTheme.typography.bodySmall)
                Text(text = "v${launcher.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
