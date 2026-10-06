package com.zcodemobile.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zcodemobile.app.ui.HomeScreen
import com.zcodemobile.app.ui.ScannerScreen
import com.zcodemobile.app.ui.THEME_DARK
import com.zcodemobile.app.ui.THEME_LIGHT
import com.zcodemobile.app.ui.WebScreen
import com.zcodemobile.app.ui.ZcodeTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Scanner : Screen
    data class Web(val connection: ZcodeConnection) : Screen
}

const val ACTION_RECONNECT = "com.zcodemobile.app.ACTION_RECONNECT"

// Activity 重建时恢复当前页面
private val ScreenSaver = listSaver<Screen, String>(
    save = {
        when (it) {
            Screen.Home -> listOf("home")
            Screen.Scanner -> listOf("scanner")
            is Screen.Web -> listOf("web", it.connection.url, it.connection.host, it.connection.deviceName ?: "")
        }
    },
    restore = {
        when (it[0]) {
            "web" -> Screen.Web(ZcodeConnection(it[1], it[2], it[3].takeIf { name -> name.isNotEmpty() }))
            "scanner" -> Screen.Scanner
            else -> Screen.Home
        }
    },
)

private const val NOTIFICATION_CHANNEL_ID = "connection"
private const val NOTIFICATION_ID = 1

class MainActivity : ComponentActivity() {

    private val store by lazy { ConnectionStore(this) }
    private val reconnectRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == ACTION_RECONNECT) reconnectRequests.intValue = 1
        setContent {
            val settings by store.settings.collectAsState(initial = AppSettings())
            ZcodeTheme(settings.themeMode) {
                ZcodeMobileApp(store, settings, reconnectRequests.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_RECONNECT) reconnectRequests.intValue++
    }
}

@Composable
private fun ZcodeMobileApp(store: ConnectionStore, settings: AppSettings, reconnectRequests: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connections by store.connections.collectAsState(initial = emptyList())
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    var autoReconnectDone by remember { mutableStateOf(false) }
    var handledReconnects by remember { mutableIntStateOf(0) }

    LaunchedEffect(connections, settings.autoReconnect) {
        if (!autoReconnectDone && settings.autoReconnect) {
            val last = connections.maxByOrNull { it.lastUsed }
            if (last != null) {
                autoReconnectDone = true
                screen = Screen.Web(ZcodeConnection(last.url, last.host, last.name))
            }
        }
    }

    LaunchedEffect(connections, reconnectRequests) {
        if (reconnectRequests > handledReconnects) {
            val last = connections.maxByOrNull { it.lastUsed }
            if (last != null) {
                handledReconnects = reconnectRequests
                screen = Screen.Web(ZcodeConnection(last.url, last.host, last.name))
            }
        }
    }

    val currentConnection = (screen as? Screen.Web)?.connection

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(currentConnection != null) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (currentConnection != null && !granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(currentConnection?.url) {
        if (currentConnection != null) postConnectionNotification(context, currentConnection)
        else NotificationManagerCompat.cancel(context)
        onDispose { }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (slideInHorizontally { it / 6 } + fadeIn()) togetherWith
                (slideOutHorizontally { -it / 6 } + fadeOut())
        },
        label = "screen",
    ) { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                connections = connections,
                settings = settings,
                store = store,
                onScan = { screen = Screen.Scanner },
                onOpen = { screen = Screen.Web(it) },
            )
            Screen.Scanner -> ScannerScreen(
                onBack = { screen = Screen.Home },
                onConnection = { conn ->
                    scope.launch {
                        store.upsert(conn)
                        screen = Screen.Web(conn)
                    }
                },
            )
            is Screen.Web -> WebScreen(
                connection = s.connection,
                keepScreenOn = settings.keepScreenOn,
                onClose = { screen = Screen.Home },
            )
        }
    }
}

private object NotificationManagerCompat {
    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}

private fun postConnectionNotification(context: Context, connection: ZcodeConnection) {
    val nm = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        nm.createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL_ID, "连接", NotificationManager.IMPORTANCE_LOW),
        )
    }
    val intent = Intent(context, MainActivity::class.java)
    val pending = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("已连接 ${QRUrlParser.displayName(connection)}")
        .setContentText("点按返回远程控制页面")
        .setOngoing(true)
        .setContentIntent(pending)
        .build()
    nm.notify(NOTIFICATION_ID, notification)
}
