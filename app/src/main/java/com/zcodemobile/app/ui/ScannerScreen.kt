package com.zcodemobile.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.zcodemobile.app.ParseResult
import com.zcodemobile.app.QRUrlParser
import com.zcodemobile.app.ZcodeConnection
import kotlinx.coroutines.launch

private class QrAnalyzer(
    private val onResult: (ParseResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                if (raw != null) onResult(QRUrlParser.parse(raw, strict = true))
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        scanner.close()
    }
}

@Composable
fun ScannerScreen(onBack: () -> Unit, onConnection: (ZcodeConnection) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    androidx.activity.compose.BackHandler(onBack = onBack)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permanentlyDenied = !granted &&
            (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) != true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var statusText by remember { mutableStateOf("将桌面端的连接二维码对准取景框") }
    var handled by remember { mutableStateOf<ZcodeConnection?>(null) }

    fun handleResult(result: ParseResult) {
        when (result) {
            is ParseResult.Ok -> {
                if (handled == null) {
                    handled = result.connection
                    onConnection(result.connection)
                }
            }
            is ParseResult.Invalid -> statusText = result.reason
        }
    }

    val analyzer = remember {
        QrAnalyzer { result -> handleResult(result) }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val decoded = QrImageDecoder.decode(context, uri)) {
                    is QrDecodeResult.Text -> handleResult(QRUrlParser.parse(decoded.raw, strict = true))
                    is QrDecodeResult.Error -> statusText = decoded.message
                }
            }
        }
    }

    // 离开页面时解除相机绑定
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            analyzer.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(ContextCompat.getMainExecutor(ctx), analyzer) }
                        runCatching {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }.onFailure { e ->
                            statusText = "相机启动失败：${e.message ?: "未知错误"}，可返回用相册导入或粘贴连接"
                        }
                        cameraProvider = provider
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            ViewFinderOverlay()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(120.dp))
                Text(
                    "扫码连接需要使用相机",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "也可以返回首页，用相册导入或粘贴连接。",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                if (permanentlyDenied) {
                    Text(
                        "相机权限已被拒绝，请在系统设置中手动开启。",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    }) {
                        Text("去系统设置授权")
                    }
                } else {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("授予相机权限")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text("扫码连接", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (camera != null) {
                IconButton(onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                }) {
                    Icon(
                        ZIcons.Flashlight,
                        contentDescription = if (torchOn) "关闭手电筒" else "打开手电筒",
                        tint = if (torchOn) Color(0xFFFFD54F) else Color.White,
                    )
                }
            }
            IconButton(onClick = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(ZIcons.Photo, contentDescription = "从相册导入", tint = Color.White)
            }
        }

        Text(
            statusText,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
        )
    }
}

@Composable
private fun ViewFinderOverlay() {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineFraction",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = size.width * 0.72f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - size.height * 0.03f
        val rect = Rect(left, top, left + side, top + side)

        drawIntoCanvas { canvas -> canvas.saveLayer(Rect(Offset.Zero, size), Paint()) }
        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRect(
            color = Color.Transparent,
            topLeft = rect.topLeft,
            size = rect.size,
            blendMode = BlendMode.Clear,
        )
        drawIntoCanvas { it.restore() }

        val len = side * 0.14f
        val stroke = 4.dp.toPx()
        val bracket: (Offset, Offset) -> Unit = { a, b ->
            drawLine(Color.Black.copy(alpha = 0.35f), a, b, stroke + 3.dp.toPx())
            drawLine(Color.White, a, b, stroke)
        }
        bracket(Offset(rect.left, rect.top + len), Offset(rect.left, rect.top))
        bracket(Offset(rect.left, rect.top), Offset(rect.left + len, rect.top))
        bracket(Offset(rect.right - len, rect.top), Offset(rect.right, rect.top))
        bracket(Offset(rect.right, rect.top), Offset(rect.right, rect.top + len))
        bracket(Offset(rect.right, rect.bottom - len), Offset(rect.right, rect.bottom))
        bracket(Offset(rect.right, rect.bottom), Offset(rect.right - len, rect.bottom))
        bracket(Offset(rect.left + len, rect.bottom), Offset(rect.left, rect.bottom))
        bracket(Offset(rect.left, rect.bottom), Offset(rect.left, rect.bottom - len))

        val lineY = rect.top + side * (0.06f + 0.88f * fraction)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFF8B7CF0).copy(alpha = 0.55f), Color.Transparent),
                startX = rect.left,
                endX = rect.right,
            ),
            start = Offset(rect.left, lineY),
            end = Offset(rect.right, lineY),
            strokeWidth = 12.dp.toPx(),
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFF6C5CE7), Color.Transparent),
                startX = rect.left,
                endX = rect.right,
            ),
            start = Offset(rect.left, lineY),
            end = Offset(rect.right, lineY),
            strokeWidth = 3.dp.toPx(),
        )
    }
}
