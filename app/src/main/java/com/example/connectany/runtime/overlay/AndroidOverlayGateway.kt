package com.example.connectany.runtime.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.connectany.domain.overlay.OverlayController
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class AndroidOverlayGateway @Inject constructor(
    @ApplicationContext private val context: Context
) : OverlayController {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    // This will be called to mount the compose view
    fun setContent(view: View) {
        hideOverlay() // Ensure old view is removed
        this.overlayView = view
    }

    override fun showOverlay() {
        val view = overlayView ?: return
        if (view.parent != null) return // Already attached

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            // Handle missing permission or window token issues
        }
    }

    override fun hideOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            if (view.parent != null) {
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            // View may have already been detached or permission revoked
        }
    }
}
