package com.example.connectany.presentation.device.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorSheet(
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val appList = resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val packageName = resolveInfo.activityInfo.packageName
                    val icon = resolveInfo.loadIcon(pm)
                    AppInfo(packageName, appName, icon)
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
            
            withContext(Dispatchers.Main) {
                apps = appList
                isLoading = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select App to Auto-Launch",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    item {
                        ListItem(
                            headlineContent = { Text("None (Disable)") },
                            modifier = Modifier.clickable { onAppSelected(null) }
                        )
                    }
                    items(apps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.appName) },
                            supportingContent = { Text(app.packageName) },
                            leadingContent = {
                                Image(
                                    bitmap = app.icon.toBitmap(100, 100).asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            modifier = Modifier.clickable { onAppSelected(app.packageName) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp)) // Padding for bottom nav
                    }
                }
            }
        }
    }
}
