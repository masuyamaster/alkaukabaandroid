package site.elahady.alkaukaba.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * Export sebuah View (report card di [site.elahady.alkaukaba.ui.awalbulan.LaporanHisabActivity])
 * jadi file PDF satu halaman, persis seperti tampilannya di layar -- menggantikan versi lama
 * yang menggambar teks polos langsung ke Canvas PDF.
 *
 * Disimpan lewat MediaStore (API 29+) supaya kompatibel dengan scoped storage; di API < 29
 * jatuh ke path publik lama (butuh WRITE_EXTERNAL_STORAGE, dicek oleh pemanggil).
 */
object HilalPdfService {

    private const val PDF_PAGE_WIDTH_PT = 595 // lebar A4 dalam point (72dpi)

    fun exportViewAsPdf(context: Context, view: View, fileName: String): Uri? {
        if (view.width <= 0 || view.height <= 0) return null

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).let { canvas -> view.draw(canvas) }

        val pageHeightPt = (bitmap.height.toFloat() / bitmap.width.toFloat() * PDF_PAGE_WIDTH_PT).toInt()
        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH_PT, pageHeightPt, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, null, Rect(0, 0, PDF_PAGE_WIDTH_PT, pageHeightPt), null)
            pdfDocument.finishPage(page)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, pdfDocument, fileName)
            } else {
                saveLegacy(pdfDocument, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
            bitmap.recycle()
        }
    }

    private fun saveViaMediaStore(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out -> pdfDocument.writeTo(out) } ?: return null
        return uri
    }

    private fun saveLegacy(pdfDocument: PdfDocument, fileName: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        return Uri.fromFile(file)
    }
}
