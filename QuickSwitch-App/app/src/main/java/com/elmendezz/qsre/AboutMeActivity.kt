package com.elmendezz.qsre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

class AboutMeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickSwitchRevivedTheme {
                AboutMeScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName

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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Animation
            AnimatedBackground()

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // GitHub Profile Picture
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
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.about_version, versionName ?: "1.0"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xOffset"
    )
    
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yOffset"
    )

    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        drawCircle(
            color = color,
            radius = width * 0.4f,
            center = androidx.compose.ui.geometry.Offset(
                x = width * (0.2f + 0.6f * xOffset),
                y = height * (0.2f + 0.6f * yOffset)
            )
        )
    }
}
