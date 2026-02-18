package com.elmendezz.qsre

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elmendezz.qsre.ui.theme.QuickSwitchRevivedTheme
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class AboutMeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val installedVersion = intent.getStringExtra("installed_version") ?: ""
        enableEdgeToEdge()
        setContent {
            QuickSwitchRevivedTheme {
                AboutMeScreen(onBack = { finish() }, installedVersion = installedVersion)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMeScreen(onBack: () -> Unit, installedVersion: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val appVersionName = packageInfo.versionName

    var latestVersion by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingUpdate by remember { mutableStateOf(true) }
    var updateAvailable by remember { mutableStateOf(false) }

    // Download States
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadSpeed by remember { mutableStateOf("") }
    var downloadSize by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf("") }
    var downloadFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response = URL("https://raw.githubusercontent.com/elmendezz/QuickSwitch/refs/heads/anyfix/master/update.json").readText()
                val json = JSONObject(response)
                latestVersion = json.getString("version")
                downloadUrl = json.getString("zipUrl")
                
                if (latestVersion != null && installedVersion.isNotEmpty()) {
                    val cleanLatest = latestVersion!!.replace("v", "").replace(".", "").toIntOrNull() ?: 0
                    val cleanInstalled = installedVersion.replace("v", "").replace(".", "").toIntOrNull() ?: 0
                    updateAvailable = cleanLatest > cleanInstalled
                }
            } catch (e: Exception) {
                latestVersion = null
            } finally {
                isLoadingUpdate = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Updated circle background to match home screen
            LargeCircleBackground()

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    AsyncImage(
                        model = "https://github.com/elmendezz.png",
                        contentDescription = "GitHub Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.about_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Update Card with Progress
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = (if (updateAvailable) MaterialTheme.colorScheme.primaryContainer 
                                          else MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (downloadFinished) Icons.Default.CheckCircle else Icons.Default.Update,
                                contentDescription = null,
                                tint = if (updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.about_latest_module),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = if (isLoadingUpdate) stringResource(R.string.about_loading) 
                                           else latestVersion ?: stringResource(R.string.about_error_update),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!isLoadingUpdate && latestVersion != null) {
                                    Text(
                                        text = if (downloadFinished) "¡Módulo listo e instalado!"
                                               else if (updateAvailable) stringResource(R.string.about_update_available)
                                               else stringResource(R.string.about_up_to_date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (updateAvailable) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        
                        if (isDownloading) {
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text(downloadSpeed, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(downloadSize, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                Text(timeRemaining, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        
                        if (updateAvailable && downloadUrl != null && !isDownloading && !downloadFinished) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    isDownloading = true
                                    scope.launch(Dispatchers.IO) {
                                        startCustomDownload(
                                            context = context,
                                            urlStr = downloadUrl!!,
                                            version = latestVersion ?: "update",
                                            onProgress = { p, speed, size, remain ->
                                                downloadProgress = p
                                                downloadSpeed = speed
                                                downloadSize = size
                                                timeRemaining = remain
                                            },
                                            onComplete = { file ->
                                                isDownloading = false
                                                downloadFinished = true
                                                flashInMagisk(context, file)
                                            },
                                            onError = { error ->
                                                isDownloading = false
                                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Descargar e instalar")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.about_version, appVersionName ?: "1.0"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private suspend fun startCustomDownload(
    context: Context,
    urlStr: String,
    version: String,
    onProgress: (Float, String, String, String) -> Unit,
    onComplete: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            withContext(Dispatchers.Main) {
                onError("Server returned HTTP ${connection.responseCode}")
            }
            return
        }

        val fileLength = connection.contentLength
        val inputStream = connection.inputStream
        val file = File(context.cacheDir, "QuickSwitch-$version.zip")
        val outputStream = FileOutputStream(file)

        val data = ByteArray(4096)
        var total: Long = 0
        var count: Int
        val startTime = System.currentTimeMillis()
        var lastUpdate = 0L

        while (inputStream.read(data).also { count = it } != -1) {
            total += count
            outputStream.write(data, 0, count)
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdate > 500) { // Update UI every 500ms
                val progress = total.toFloat() / fileLength
                val timeElapsed = (currentTime - startTime) / 1000f
                val speedBytes = if (timeElapsed > 0) total / timeElapsed else 0f
                val speedText = String.format("%.2f MB/s", speedBytes / (1024 * 1024))
                val totalSizeText = String.format("%.2f MB / %.2f MB", total.toFloat() / (1024 * 1024), fileLength.toFloat() / (1024 * 1024))
                
                val remainingBytes = fileLength - total
                val remainingSeconds = if (speedBytes > 0) (remainingBytes / speedBytes).toInt() else 0
                val remainingText = String.format("Restan: %02d:%02d", remainingSeconds / 60, remainingSeconds % 60)

                withContext(Dispatchers.Main) {
                    onProgress(progress, speedText, totalSizeText, remainingText)
                }
                lastUpdate = currentTime
            }
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()

        withContext(Dispatchers.Main) {
            onComplete(file)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError(e.message ?: "Unknown error")
        }
    }
}

private fun flashInMagisk(context: Context, file: File) {
    CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Flasheando en Magisk...", Toast.LENGTH_SHORT).show()
        }
        
        // Command to install module in Magisk
        val cmd = "magisk --install-module ${file.absolutePath}"
        val result = ShellHelper.runAsRoot(cmd)
        
        withContext(Dispatchers.Main) {
            if (result.exitCode == 0) {
                Toast.makeText(context, "¡Módulo flasheado correctamente!", Toast.LENGTH_LONG).show()
                // Auto reboot? User usually prefers to reboot manually or from main screen.
            } else {
                Toast.makeText(context, "Fallo al flashear: ${result.stderr}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun LargeCircleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Increased duration and using SineInOut for smoother, more fluid movement
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xOffset"
    )
    
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yOffset"
    )

    // Using same alpha as home screen (0.15f)
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Matching home screen radius (width * 0.4f)
        drawCircle(
            color = color,
            radius = width * 0.4f,
            center = Offset(
                x = width * (0.2f + 0.6f * xOffset),
                y = height * (0.2f + 0.6f * yOffset)
            )
        )
    }
}
