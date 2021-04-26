@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.tintText
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.getPrefTheme
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Returns coordinates of view.
 * Used for animation
 *
 * @return coordinates of view
 */
fun View.getCoordinates() = Point((left + right) / 2, (top + bottom) / 2)

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
fun View.snack(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    if (f != null) {
        snack.f()
    }
    val theme = context.getPrefTheme(Injekt.get())
    if (ThemeUtil.isPitchBlack(context, theme)) {
        val textView: TextView =
            snack.view.findViewById(com.google.android.material.R.id.snackbar_text)
        val button: Button? =
            snack.view.findViewById(com.google.android.material.R.id.snackbar_action)
        textView.setTextColor(Color.WHITE)
        button?.setTextColor(Color.WHITE)
        snack.view.backgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
    }
    snack.show()
    return snack
}

fun View.snack(
    resource: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null
): Snackbar {
    return snack(context.getString(resource), length, f)
}

fun Snackbar.getText(): CharSequence {
    val textView: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
    return textView.text
}

inline fun View.visible() {
    visibility = View.VISIBLE
}

inline fun View.invisible() {
    visibility = View.INVISIBLE
}

inline fun View.gone() {
    visibility = View.GONE
}

inline val View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

inline val View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

inline val View.marginRight: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0

inline val View.marginLeft: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0

object RecyclerWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.updatePaddingRelative(bottom = insets.systemWindowInsetBottom)
        // v.updatePaddingRelative(bottom = v.paddingBottom + insets.systemWindowInsetBottom)
        return insets
    }
}

object ControllerViewWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.updateLayoutParams<FrameLayout.LayoutParams> {
            val attrsArray = intArrayOf(android.R.attr.actionBarSize)
            val array = v.context.obtainStyledAttributes(attrsArray)
            topMargin = insets.systemWindowInsetTop + array.getDimensionPixelSize(0, 0)
            array.recycle()
        }
        return insets
    }
}

object HeightTopWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        val topInset = insets.systemWindowInsetTop
        v.setPadding(0, topInset, 0, 0)
        if (v.layoutParams.height != topInset) {
            v.layoutParams.height = topInset
            v.requestLayout()
        }
        return insets
    }
}

fun View.doOnApplyWindowInsets(f: (View, WindowInsets, ViewPaddingState) -> Unit) {
    // Create a snapshot of the view's padding state
    val paddingState = createStateForView(this)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, paddingState)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.applyWindowInsetsForController() {
    setOnApplyWindowInsetsListener(ControllerViewWindowInsetsListener)
    requestApplyInsetsWhenAttached()
}

fun View.checkHeightThen(f: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (height > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        }
    )
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.requestApplyInsets()
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        )
    }
}

inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    val params = layoutParams as T
    block(params)
    layoutParams = params
}

inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

private fun createStateForView(view: View) = ViewPaddingState(
    view.paddingLeft,
    view.paddingTop,
    view.paddingRight,
    view.paddingBottom,
    view.paddingStart,
    view.paddingEnd
)

data class ViewPaddingState(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val start: Int,
    val end: Int
)

inline fun View.updatePaddingRelative(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
) {
    setPaddingRelative(start, top, end, bottom)
}

fun BottomSheetDialog.setEdgeToEdge(
    activity: Activity,
    contentView: View,
    setTopMargin: Int = -1
) {
    window?.setBackgroundDrawable(null)
    window?.navigationBarColor = activity.window.navigationBarColor
    val isLight = (activity.window?.decorView?.systemUiVisibility ?: 0) and View
        .SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLight) {
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
    window?.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows =
        false
    window?.findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
        false
    contentView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View
        .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    val insets = activity.window.decorView.rootWindowInsets
    (contentView.parent as View).background = null
    contentView.post {
        (contentView.parent as View).background = null
    }
    contentView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        leftMargin = insets.systemWindowInsetLeft
        rightMargin = insets.systemWindowInsetRight
    }
    if (setTopMargin > 0) (contentView.parent as View).updateLayoutParams<ViewGroup.MarginLayoutParams> {
        height = activity.window.decorView.height - insets.systemWindowInsetTop - setTopMargin
    }
    else if (setTopMargin == 0) contentView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = insets.systemWindowInsetTop
    }
    contentView.requestLayout()
}

fun setBottomEdge(view: View, activity: Activity) {
    val marginB = view.marginBottom
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        bottomMargin = marginB + activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
    }
}

fun SwipeRefreshLayout.setStyle() {
    setColorSchemeColors(context.getResourceColor(R.attr.actionBarTintColor))
    setProgressBackgroundColorSchemeColor(context.getResourceColor(R.attr.colorPrimaryVariant))
}

fun MaterialButton.resetStrokeColor() {
    strokeColor = ColorStateList.valueOf(
        ColorUtils.setAlphaComponent(
            context.getResourceColor(
                R.attr.colorOnSurface
            ),
            31
        )
    )
}

fun TextView.setTextColorRes(@ColorRes id: Int) {
    setTextColor(context.contextCompatColor(id))
}

@SuppressLint("RestrictedApi")
fun BottomNavigationView.getItemView(@IdRes id: Int): BottomNavigationItemView? {
    val order = (menu as MenuBuilder).findItemIndex(id)
    return (getChildAt(0) as BottomNavigationMenuView).getChildAt(order) as? BottomNavigationItemView
}

fun RecyclerView.smoothScrollToTop() {
    val linearLayoutManager = layoutManager as? LinearLayoutManager
    if (linearLayoutManager != null) {
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = 0
        val firstItemPos = linearLayoutManager.findFirstVisibleItemPosition()
        if (firstItemPos > 15) {
            scrollToPosition(15)
            post {
                linearLayoutManager.startSmoothScroll(smoothScroller)
            }
        } else {
            linearLayoutManager.startSmoothScroll(smoothScroller)
        }
    } else {
        scrollToPosition(0)
    }
}

fun View.rowsForValue(value: Int): Int {
    return rowsForValue((value / 2f) - .5f)
}

fun View.rowsForValue(value: Float): Int {
    val size = 1.5f.pow(value)
    val trueSize = AutofitRecyclerView.MULTIPLE * ((size * 100 / AutofitRecyclerView.MULTIPLE).roundToInt()) / 100f
    val dpWidth = (measuredWidth.pxToDp / 100f).roundToInt()
    return max(1, (dpWidth / trueSize).roundToInt())
}

var View.compatToolTipText: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        tooltipText
    } else {
        ""
    }
    set(value) {
        ViewCompat.setTooltipText(this, value)
    }

@SuppressLint("RestrictedApi")
inline fun View.popupMenu(
    items: List<Pair<Int, Int>>,
    selectedItemId: Int? = null,
    noinline onMenuItemClick: MenuItem.() -> Unit
): PopupMenu {
    val popup = PopupMenu(context, this, Gravity.NO_GRAVITY)
    items.forEach { (id, stringRes) ->
        popup.menu.add(0, id, 0, stringRes)
    }

    if (selectedItemId != null) {
        val blendedAccent = ColorUtils.blendARGB(
            context.getResourceColor(android.R.attr.colorAccent),
            context.getResourceColor(android.R.attr.textColorPrimary),
            0.5f
        )
        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        val emptyIcon = ContextCompat.getDrawable(context, R.drawable.ic_blank_24dp)
        popup.menu.forEach { item ->
            item.icon = when (item.itemId) {
                selectedItemId -> ContextCompat.getDrawable(context, R.drawable.ic_check_24dp)?.mutate()?.apply {
                    setTint(blendedAccent)
                }
                else -> emptyIcon
            }
            if (item.itemId == selectedItemId) {
                item.title = item.title?.tintText(blendedAccent)
            }
        }
    }

    popup.setOnMenuItemClickListener {
        it.onMenuItemClick()
        true
    }

    popup.show()
    return popup
}
