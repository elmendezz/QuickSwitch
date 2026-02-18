package com.elmendezz.qsre

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elmendezz.qsre.ui.components.SingleCircleBackground
import com.elmendezz.qsre.ui.theme.QuickSwitchRevivedTheme
import com.elmendezz.qsre.ui.theme.ThemeConfig

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickSwitchRevivedTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("qsre_prefs", Context.MODE_PRIVATE) }
    val themeMode = ThemeConfig.themeMode
    
    var showPathDialog by remember { mutableStateOf(false) }
    var quickswitchPath by remember { 
        mutableStateOf(prefs.getString("quickswitch_path", "/data/adb/modules/quickswitch/quickswitch") ?: "/data/adb/modules/quickswitch/quickswitch") 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SingleCircleBackground()
            
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                // Theme Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = themeMode == "system", 
                                onClick = { 
                                    ThemeConfig.themeMode = "system"
                                    prefs.edit().putString("theme_mode", "system").apply()
                                }
                            )
                            Text(stringResource(R.string.theme_system))
                            Spacer(Modifier.width(8.dp))
                            RadioButton(
                                selected = themeMode == "light", 
                                onClick = { 
                                    ThemeConfig.themeMode = "light"
                                    prefs.edit().putString("theme_mode", "light").apply()
                                }
                            )
                            Text(stringResource(R.string.theme_light))
                            Spacer(Modifier.width(8.dp))
                            RadioButton(
                                selected = themeMode == "dark", 
                                onClick = { 
                                    ThemeConfig.themeMode = "dark"
                                    prefs.edit().putString("theme_mode", "dark").apply()
                                }
                            )
                            Text(stringResource(R.string.theme_dark))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                // Advanced Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "Avanzado", 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_change_path)) },
                            supportingContent = { Text(quickswitchPath) },
                            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showPathDialog = true }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // About Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "Información", 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_about)) },
                            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { 
                                context.startActivity(Intent(context, AboutMeActivity::class.java))
                            }
                        )
                    }
                }
            }
        }

        if (showPathDialog) {
            var tempPath by remember { mutableStateOf(quickswitchPath) }
            AlertDialog(
                onDismissRequest = { showPathDialog = false },
                title = { Text(stringResource(R.string.dialog_path_title)) },
                text = { 
                    TextField(
                        value = tempPath, 
                        onValueChange = { tempPath = it },
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                confirmButton = {
                    Button(onClick = { 
                        quickswitchPath = tempPath
                        prefs.edit().putString("quickswitch_path", quickswitchPath).apply()
                        showPathDialog = false
                    }) { Text(stringResource(R.string.dialog_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showPathDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}
