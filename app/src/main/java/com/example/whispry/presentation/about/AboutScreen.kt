// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.presentation.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.whispry.R
import com.example.whispry.presentation.common.GlassCard
import com.example.whispry.ui.theme.DeepPurple
import com.example.whispry.util.CrashLogger
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun AboutScreen(
    backdrop: LayerBackdrop,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            start = 24.dp, 
            end = 24.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(modifier = Modifier.animateItem()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // App Logo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.whisperlogo),
                            contentDescription = "Whispry Logo",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Whispry", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Your voice, anywhere.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Stats Section
        item {
            Box(modifier = Modifier.animateItem()) {
                GlassCard(
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Impact", style = MaterialTheme.typography.labelSmall, color = DeepPurple)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AboutStatItem("${state.totalWords}", "Words")
                            AboutStatItem("${state.totalRecordings}", "Recordings")
                            AboutStatItem("${state.totalTimeSavedSeconds}s", "Time Saved")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Actions Section
        item {
            val crashLog = remember { CrashLogger.latestCrashLog(context) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutActionRow(Icons.Rounded.Star, "Rate the app", backdrop) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    context.startActivity(intent)
                }
                AboutActionRow(Icons.Rounded.BugReport, "Report a bug", backdrop) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@whispry.app"))
                    context.startActivity(intent)
                }
                if (crashLog != null) {
                    // On-device only — see CrashLogger. Nothing is sent anywhere until the user
                    // explicitly shares this file themselves.
                    AboutActionRow(Icons.Rounded.WarningAmber, "Share Crash Log", backdrop) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", crashLog)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share crash log"))
                    }
                }
                AboutActionRow(Icons.Rounded.Share, "Share Whispry", backdrop) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Whispry for instant voice transcription!")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Box(modifier = Modifier.animateItem()) {
                Text(
                    text = "Version ${state.version} (${state.buildNumber})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Required by LICENSE's additional Section 7 term: attribution must travel with the
        // running app, not just the source repo — see NOTICE.
        item {
            Box(modifier = Modifier.animateItem()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Whispry is open source, licensed under AGPL-3.0.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "github.com/cosmictaserdev-creator/whispry",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/cosmictaserdev-creator/whispry")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun AboutStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
    }
}

@Composable
fun AboutActionRow(
    icon: ImageVector,
    title: String,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    GlassCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f }
            )
        }
    }
}
