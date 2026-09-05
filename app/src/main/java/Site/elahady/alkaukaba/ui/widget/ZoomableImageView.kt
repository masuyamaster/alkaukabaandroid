package site.elahady.alkaukaba.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.min

/**
 * ImageView dengan pinch-to-zoom, pan, dan double-tap zoom - dipakai modal
 * zoom ilustrasi Fase Bulan. Ditulis manual (bukan library seperti PhotoView)
 * karena kebutuhannya sederhana (satu bitmap persegi, tanpa fitur lain) dan
 * repo ini belum punya dependency image-zoom apa pun.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrixValues = FloatArray(9)
    private val imgMatrix = Matrix()
    private var minScale = 1f
    private val maxScale = 5f
    private var bitmapW = 0f
    private var bitmapH = 0f

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = -1
    private var isDragging = false

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        bitmapW = bm?.width?.toFloat() ?: 0f
        bitmapH = bm?.height?.toFloat() ?: 0f
        post { resetMatrix() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetMatrix()
    }

    private fun resetMatrix() {
        if (bitmapW <= 0f || bitmapH <= 0f || width <= 0 || height <= 0) return
        val fitScale = min(width / bitmapW, height / bitmapH)
        minScale = fitScale
        imgMatrix.reset()
        imgMatrix.postScale(fitScale, fitScale)
        imgMatrix.postTranslate(
            (width - bitmapW * fitScale) / 2f,
            (height - bitmapH * fitScale) / 2f
        )
        imageMatrix = imgMatrix
    }

    private fun currentScale(): Float {
        imgMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && currentScale() > minScale + 0.01f) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        if (!isDragging && (abs(dx) > 4 || abs(dy) > 4)) isDragging = true
                        if (isDragging) {
                            imgMatrix.postTranslate(dx, dy)
                            constrainTranslation()
                            imageMatrix = imgMatrix
                        }
                        lastTouchX = x
                        lastTouchY = y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                if (event.getPointerId(pointerIndex) == activePointerId) {
                    val newIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newIndex)
                    lastTouchY = event.getY(newIndex)
                    activePointerId = event.getPointerId(newIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                activePointerId = -1
            }
        }
        return true
    }

    /** Jaga supaya bitmap yang di-zoom tidak bisa digeser sampai keluar dari area view. */
    private fun constrainTranslation() {
        imgMatrix.getValues(matrixValues)
        val scale = matrixValues[Matrix.MSCALE_X]
        val scaledW = bitmapW * scale
        val scaledH = bitmapH * scale

        matrixValues[Matrix.MTRANS_X] = if (scaledW <= width) {
            (width - scaledW) / 2f
        } else {
            matrixValues[Matrix.MTRANS_X].coerceIn(width - scaledW, 0f)
        }
        matrixValues[Matrix.MTRANS_Y] = if (scaledH <= height) {
            (height - scaledH) / 2f
        } else {
            matrixValues[Matrix.MTRANS_Y].coerceIn(height - scaledH, 0f)
        }
        imgMatrix.setValues(matrixValues)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val targetScale = (currentScale() * detector.scaleFactor).coerceIn(minScale, maxScale)
            val appliedFactor = targetScale / currentScale()
            imgMatrix.postScale(appliedFactor, appliedFactor, detector.focusX, detector.focusY)
            constrainTranslation()
            imageMatrix = imgMatrix
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (currentScale() > minScale + 0.01f) minScale else minScale * 2.5f
            val factor = target / currentScale()
            imgMatrix.postScale(factor, factor, e.x, e.y)
            constrainTranslation()
            imageMatrix = imgMatrix
            return true
        }
    }
}
