package coil.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build.VERSION.SDK_INT
import coil.transform.AnimatedTransformation

class RoundedCornerTransformation : AnimatedTransformation {
    override fun transform(canvas: Canvas): AnimatedTransformation.PixelFormat {
        val path = Path()
        path.fillType = Path.FillType.INVERSE_EVEN_ODD
        val width = canvas.width
        val height = canvas.height
        if (SDK_INT >= 21) {
            path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 20f, 20f, Path.Direction.CW)
        } else {
            path.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 20f, 20f, Path.Direction.CW)
        }
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        canvas.drawPath(path, paint)
        return AnimatedTransformation.PixelFormat.TRANSLUCENT
    }
}
