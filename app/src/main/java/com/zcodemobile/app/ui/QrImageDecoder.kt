package com.zcodemobile.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrImageDecoder {

    // 从相册图片里解出二维码原始文本，识别不到返回 null
    suspend fun decode(context: Context, uri: Uri): QrDecodeResult = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(context, uri, maxDim = 2048)
            ?: return@withContext QrDecodeResult.Error("无法读取图片")
        val image = InputImage.fromBitmap(bitmap, readRotationDegrees(context, uri))
        val client = BarcodeScanning.getClient()
        try {
            val barcodes = com.google.android.gms.tasks.Tasks.await(client.process(image))
            val raw = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                ?: barcodes.firstOrNull { it.valueType == Barcode.TYPE_URL }?.url?.url
            if (raw != null) QrDecodeResult.Text(raw) else QrDecodeResult.Error("图片中没有识别到二维码")
        } catch (e: Exception) {
            QrDecodeResult.Error("二维码识别失败：${e.message ?: "未知错误"}")
        } finally {
            client.close()
        }
    }

    // EXIF 旋转角需要传给 ML Kit
    private fun readRotationDegrees(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val orientation = ExifInterface(stream)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_TRANSPOSE,
                -> 90

                ExifInterface.ORIENTATION_ROTATE_180,
                -> 180

                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface.ORIENTATION_TRANSVERSE,
                -> 270

                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }

    // 相册返回的 URI 只读一次：反复打开输入流在部分 provider 上会被拒
    private fun decodeScaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val bytes = readBytes(context, uri, maxBytes = 32L * 1024 * 1024) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun readBytes(context: Context, uri: Uri, maxBytes: Long): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}

sealed interface QrDecodeResult {
    data class Text(val raw: String) : QrDecodeResult
    data class Error(val message: String) : QrDecodeResult
}
