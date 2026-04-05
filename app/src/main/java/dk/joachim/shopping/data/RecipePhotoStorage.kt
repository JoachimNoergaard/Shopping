package dk.joachim.shopping.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Stores one JPEG per recipe under app-internal storage; file name is `{recipeId}.jpg`. */
object RecipePhotoStorage {

    private const val DIR_NAME = "recipe_photos"
    private const val MAX_EDGE_PX = 1600
    private const val JPEG_QUALITY = 88

    fun localJpegFile(context: Context, recipeId: String): File =
        File(File(context.filesDir, DIR_NAME).apply { mkdirs() }, "$recipeId.jpg")

    fun hasPhoto(context: Context, recipeId: String): Boolean =
        localJpegFile(context, recipeId).exists()

    fun deletePhoto(context: Context, recipeId: String) {
        localJpegFile(context, recipeId).takeIf { it.exists() }?.delete()
    }

    /** JPEG bytes as Base64, or null if there is no local file. */
    fun readBase64(context: Context, recipeId: String): String? {
        val f = localJpegFile(context, recipeId)
        if (!f.isFile || f.length() == 0L) return null
        return Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
    }

    /** Writes decoded JPEG to disk, or deletes the file when [base64] is null/blank. */
    fun saveFromBase64(context: Context, recipeId: String, base64: String?) {
        if (base64.isNullOrBlank()) {
            deletePhoto(context, recipeId)
            return
        }
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (bytes.isEmpty()) {
                deletePhoto(context, recipeId)
                return
            }
            val out = localJpegFile(context, recipeId)
            out.parentFile?.mkdirs()
            out.writeBytes(bytes)
        } catch (_: Exception) {
            deletePhoto(context, recipeId)
        }
    }

    /** Removes cached files for recipe IDs that no longer exist (e.g. after a full sync). */
    fun deleteOrphanPhotos(context: Context, keepRecipeIds: Set<String>) {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (!file.name.endsWith(".jpg")) return@forEach
            val id = file.name.removeSuffix(".jpg")
            if (id !in keepRecipeIds) file.delete()
        }
    }

    /**
     * Decodes [uri], scales down to [MAX_EDGE_PX], compresses as JPEG, writes to [localJpegFile].
     * @return true if a file was written.
     */
    fun saveFromUri(context: Context, recipeId: String, uri: Uri): Boolean {
        return try {
            val bitmap =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    decodeBitmapFromUriApi28(context, uri)
                } else {
                    decodeBitmapFromUriLegacy(context, uri)
                } ?: return false

            val outFile = localJpegFile(context, recipeId)
            writeJpegFile(bitmap, outFile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * [Bitmap.compress] returns false for [Bitmap.Config.HARDWARE] bitmaps; normalize first.
     */
    private fun writeJpegFile(bitmap: Bitmap, outFile: File): Boolean {
        val software =
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
                bitmap.recycle()
                copy
            } else {
                bitmap
            }
        try {
            FileOutputStream(outFile).use { fos ->
                if (!software.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)) {
                    outFile.delete()
                    return false
                }
            }
            return true
        } finally {
            software.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeBitmapFromUriApi28(context: Context, uri: Uri): Bitmap? {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            if (w > 0 && h > 0) {
                val longEdge = max(w, h)
                if (longEdge > MAX_EDGE_PX) {
                    val scale = MAX_EDGE_PX.toFloat() / longEdge
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        }
    }

    private fun decodeBitmapFromUriLegacy(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        } ?: return null

        val w = bitmap.width
        val h = bitmap.height
        val longEdge = max(w, h)
        if (longEdge > MAX_EDGE_PX) {
            val scale = MAX_EDGE_PX.toFloat() / longEdge
            val nw = (w * scale).roundToInt().coerceAtLeast(1)
            val nh = (h * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }
        return bitmap
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var size = 1
        val longEdge = max(width, height)
        while (longEdge / size > maxEdge) size *= 2
        return size
    }
}
