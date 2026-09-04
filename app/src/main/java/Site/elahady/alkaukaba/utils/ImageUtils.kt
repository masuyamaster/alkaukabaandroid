package site.elahady.alkaukaba.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import site.elahady.alkaukaba.R
import java.io.File
import java.io.FileOutputStream

/** Persiapan file foto profil sebelum di-upload: EXIF-rotate, center-crop persegi, downscale. */
object ImageUtils {

    private const val TARGET_SIZE = 800
    private const val JPEG_QUALITY = 85

    /** Tampilkan foto profil (Glide, dibulatkan) kalau ada, atau kembalikan placeholder ikon
     * gold+navy default kalau belum/tidak ada foto - dipakai di semua tempat avatar user
     * ditampilkan (ProfileActivity, sheet Edit Profil, header MainActivity) supaya konsisten.
     * imageTintList WAJIB dibersihkan sebelum load foto asli, kalau tidak foto ikut ke-tint
     * navy seperti ikon placeholder-nya. */
    fun loadAvatarInto(context: Context, imageView: ImageView, avatarUrl: String?, placeholderPaddingDp: Int) {
        val paddingPx = (placeholderPaddingDp * context.resources.displayMetrics.density).toInt()
        if (avatarUrl.isNullOrBlank()) {
            Glide.with(context).clear(imageView)
            imageView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            imageView.setImageResource(R.drawable.ic_person)
            imageView.background = ContextCompat.getDrawable(context, R.drawable.bg_circle_button)
            imageView.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gold_accent)
            imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.login_bg_deep)
        } else {
            imageView.background = null
            imageView.imageTintList = null
            imageView.setPadding(0, 0, 0, 0)
            Glide.with(context)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(imageView)
        }
    }

    /** @return file JPEG siap upload di cache dir, atau null kalau gambar sumber tidak bisa dibaca. */
    fun prepareAvatarFile(context: Context, source: Uri): File? {
        val resolver = context.contentResolver

        // inJustDecodeBounds = true membuat decodeStream SELALU return null by design (cuma
        // ngisi bounds.outWidth/outHeight, bukan alokasi Bitmap) - jangan pakai null itu buat
        // deteksi gagal, cek bounds-nya langsung setelah decode.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(source) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_SIZE)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val rotated = applyExifRotation(context, source, rawBitmap)
        val squared = centerCropSquare(rotated)
        val scaled = if (squared.width > TARGET_SIZE) {
            Bitmap.createScaledBitmap(squared, TARGET_SIZE, TARGET_SIZE, true)
        } else {
            squared
        }

        val outFile = File(context.cacheDir, "avatar_upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (rawBitmap !== rotated) rawBitmap.recycle()
        if (rotated !== squared) rotated.recycle()
        if (squared !== scaled) squared.recycle()

        return outFile
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        var halfW = width / 2
        var halfH = height / 2
        while (halfW / sampleSize >= targetSize && halfH / sampleSize >= targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifRotation(context: Context, source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }
}
