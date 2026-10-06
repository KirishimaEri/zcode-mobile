package com.zcodemobile.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zcodemobile.app.AppSettings
import com.zcodemobile.app.ConnectionStore
import com.zcodemobile.app.ParseResult
import com.zcodemobile.app.QRUrlParser
import com.zcodemobile.app.R
import com.zcodemobile.app.SavedConnection
import com.zcodemobile.app.ZcodeConnection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    connections: List<SavedConnection>,
    settings: AppSettings,
    store: ConnectionStore,
    onScan: () -> Unit,
    onOpen: (ZcodeConnection) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showPasteDialog by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<SavedConnection?>(null) }

    fun connect(result: ParseResult) {
        when (result) {
            is ParseResult.Ok -> scope.launch {
                store.upsert(result.connection)
                onOpen(result.connection)
            }
            is ParseResult.Invalid -> scope.launch { snackbar.showSnackbar(result.reason) }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val decoded = QrImageDecoder.decode(context, uri)) {
                    is QrDecodeResult.Text -> connect(QRUrlParser.parse(decoded.raw, strict = true))
                    is QrDecodeResult.Error -> snackbar.showSnackbar(decoded.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF151718)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                ZIcons.BrandZ,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Zcode Mobile", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = onScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(ZIcons.QrCode, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("扫码连接", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("扫描桌面端生成的配对二维码", fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EntryCard(
                    icon = ZIcons.Photo,
                    label = "相册导入",
                    modifier = Modifier.weight(1f),
                ) {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                EntryCard(
                    icon = ZIcons.Paste,
                    label = "粘贴连接",
                    modifier = Modifier.weight(1f),
                ) { showPasteDialog = true }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("连接历史", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                ZIcons.Desktop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("还没有连接记录", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "在桌面端打开「移动端远程控制」，\n扫一次二维码即可建立连接。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                }
            } else {
                connections
                    .sortedWith(compareByDescending<SavedConnection> { it.favorite }.thenByDescending { it.lastUsed })
                    .forEach { conn ->
                        ConnectionCard(
                            connection = conn,
                            onOpen = {
                                scope.launch { store.touch(conn.id) }
                                onOpen(ZcodeConnection(conn.url, conn.host, conn.name))
                            },
                            onDelete = { scope.launch { store.remove(conn.id) } },
                            onEdit = { editingConnection = conn },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("设置", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            SettingRow(
                label = "启动时自动重连上次连接",
                checked = settings.autoReconnect,
                onChange = { scope.launch { store.setAutoReconnect(it) } },
            )
            SettingRow(
                label = "连接时保持屏幕常亮",
                checked = settings.keepScreenOn,
                onChange = { scope.launch { store.setKeepScreenOn(it) } },
            )

            Spacer(Modifier.height(12.dp))
            Text("主题", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(THEME_SYSTEM to "跟随系统", THEME_LIGHT to "亮色", THEME_DARK to "暗色")
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { scope.launch { store.setThemeMode(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "配对链接等同桌面端钥匙，请勿分享给他人。" +
                    "官方同一时刻只允许一个手机页面连接，新连接会自动踢掉旧连接。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPasteDialog) {
        PasteDialog(
            onDismiss = { showPasteDialog = false },
            onConfirm = { text ->
                showPasteDialog = false
                connect(QRUrlParser.parse(text, strict = false))
            },
        )
    }

    editingConnection?.let { conn ->
        EditConnectionDialog(
            connection = conn,
            onDismiss = { editingConnection = null },
            onConfirm = { name, favorite ->
                editingConnection = null
                scope.launch {
                    store.rename(conn.id, name)
                    store.setFavorite(conn.id, favorite)
                }
            },
        )
    }
}

@Composable
private fun EntryCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectionCard(
    connection: SavedConnection,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onOpen, onLongClick = onEdit)
                .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ZIcons.Desktop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(connection.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${connection.host} · ${relativeTime(connection.lastUsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (connection.favorite) {
                Icon(
                    ZIcons.Star,
                    contentDescription = "已收藏",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PasteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("粘贴连接") },
        text = {
            Column {
                Text(
                    "粘贴桌面端远程控制弹窗里显示的完整连接地址。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4,
                    placeholder = { Text("https://zcode.z.ai/remote/v4?...") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("连接") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EditConnectionDialog(
    connection: SavedConnection,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(connection.name) }
    var favorite by remember { mutableStateOf(connection.favorite) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑连接") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("设备名称") },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { favorite = !favorite },
                ) {
                    Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                    Text("收藏置顶", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.ifBlank { connection.host }, favorite) },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun relativeTime(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val diff = System.currentTimeMillis() - epochMillis
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        minutes < 60 * 24 -> "${minutes / 60} 小时前"
        minutes < 60 * 24 * 30 -> "${minutes / 60 / 24} 天前"
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(epochMillis))
    }
}
