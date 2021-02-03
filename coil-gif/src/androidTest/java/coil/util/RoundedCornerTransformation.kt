package coil.util

import android.graphics.*
import android.os.Build
import coil.size.Size
import coil.transform.AnimatedTransformation
import kotlin.math.min

class RoundedCornerTransformation : AnimatedTransformation {
    override fun transform(canvas: Canvas, size: Size): AnimatedTransformation.PixelFormat {
        val path = Path()
        path.fillType = Path.FillType.INVERSE_EVEN_ODD
        val width = canvas.width
        val height = canvas.height
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 20f, 20f, Path.Direction.CW)
        }
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        canvas.drawPath(path, paint)
        return AnimatedTransformation.PixelFormat.TRANSLUCENT
    }
}
