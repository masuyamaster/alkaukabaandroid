package Site.elahady.alkaukaba.utils

import Site.elahady.alkaukaba.model.HilalResult
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object HilalPdfService {

    fun generatePdf(context: Context, result: HilalResult) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Judul
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("HASIL PERHITUNGAN AWAL BULAN", 50f, 50f, paint)

        // Isi Log (Sesuai Screenshot "Ijtima")
        paint.textSize = 12f
        paint.isFakeBoldText = false

        var yPosition = 80f
        val lines = result.calculationLog.split("\n")

        for (line in lines) {
            canvas.drawText(line, 50f, yPosition, paint)
            yPosition += 20f
        }

        pdfDocument.finishPage(page)

        // Simpan File
        val fileName = "Hisab_Hilal_${System.currentTimeMillis()}.pdf"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF Disimpan di Download/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal simpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}