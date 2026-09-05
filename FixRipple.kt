import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import androidx.core.graphics.ColorUtils

fun createRipple(ink: Int): RippleDrawable {
    return RippleDrawable(
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(ink, 40)),
        null,
        null
    )
}
