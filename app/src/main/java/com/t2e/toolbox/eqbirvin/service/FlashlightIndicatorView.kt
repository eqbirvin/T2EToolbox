package com.t2e.toolbox.eqbirvin.service

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import com.t2e.toolbox.eqbirvin.R

class FlashlightIndicatorView(context: Context) : FrameLayout(context) {

    private val imageView: ImageView
    private val pillContainer: FrameLayout

    init {
        val density = context.resources.displayMetrics.density

        // Create sleek pill container
        pillContainer = FrameLayout(context).apply {
            val pillWidth = (46 * density).toInt()
            val pillHeight = (20 * density).toInt()
            layoutParams = LayoutParams(pillWidth, pillHeight).apply {
                gravity = Gravity.CENTER
            }

            val pillBg = GradientDrawable().apply {
                cornerRadius = 10 * density
                setColor(Color.argb(220, 24, 24, 24))
                setStroke((1 * density).toInt(), Color.argb(80, 255, 255, 255))
            }
            background = pillBg
        }

        // Create horizontal flashlight icon
        imageView = ImageView(context).apply {
            val iconWidth = (30 * density).toInt()
            val iconHeight = (15 * density).toInt()
            layoutParams = LayoutParams(iconWidth, iconHeight).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.ic_statusbar_flashlight_horizontal)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#FFB300")) // Amber by default
        }

        pillContainer.addView(imageView)
        addView(pillContainer)
    }

    fun updateColor(colorArgb: Long) {
        val colorInt = colorArgb.toInt()
        imageView.imageTintList = ColorStateList.valueOf(colorInt)
    }

    fun setTapToDismiss(enabled: Boolean, onDismiss: () -> Unit) {
        isClickable = enabled
        pillContainer.isClickable = enabled
        if (enabled) {
            setOnClickListener { onDismiss() }
            pillContainer.setOnClickListener { onDismiss() }
        } else {
            setOnClickListener(null)
            pillContainer.setOnClickListener(null)
        }
    }
}
