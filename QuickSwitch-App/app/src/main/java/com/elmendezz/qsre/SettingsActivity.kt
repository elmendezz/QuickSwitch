package com.elmendezz.qsre

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elmendezz.qsre.ui.components.SingleCircleBackground
import com.elmendezz.qsre.ui.theme.QuickSwitchRevivedTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("qsre_prefs", Context.MODE_PRIVATE) }
            
            var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
            var appColor by remember { mutableIntStateOf(prefs.getInt("app_color", -1)) }
            var infiniteIcon by remember { mutableStateOf(prefs.getBoolean("infinite_icon", false)) }
            
            QuickSwitchRevivedTheme(themeMode = themeMode, appColor = appColor) {
                SettingsScreen(
                    themeMode = themeMode,
                    appColor = appColor,
                    infiniteIcon = infiniteIcon,
                    onThemeChange = { 
                        themeMode = it
                        prefs.edit().putString("theme_mode", it).apply()
                    },
                    onColorChange = {
                        appColor = it
                        prefs.edit().putInt("app_color", it).apply()
                    },
                    onInfiniteIconChange = {
                        infiniteIcon = it
                        prefs.edit().putBoolean("infinite_icon", it).apply()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    appColor: Int,
    infiniteIcon: Boolean,
    onThemeChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onInfiniteIconChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
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
                Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = themeMode == "system", onClick = { onThemeChange("system") })
                    Text(stringResource(R.string.theme_system))
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = themeMode == "light", onClick = { onThemeChange("light") })
                    Text(stringResource(R.string.theme_light))
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = themeMode == "dark", onClick = { onThemeChange("dark") })
                    Text(stringResource(R.string.theme_dark))
                }
                
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.setting_color), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .clickable { onColorChange(color.toArgb()) }
                        )
                    }
                }
                TextButton(onClick = { onColorChange(-1) }) { Text("Reset color (Material You)") }
                
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onInfiniteIconChange(!infiniteIcon) }) {
                    Checkbox(checked = infiniteIcon, onCheckedChange = { onInfiniteIconChange(it) })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.setting_icon_infinite))
                }
            }
        }
    }
}
