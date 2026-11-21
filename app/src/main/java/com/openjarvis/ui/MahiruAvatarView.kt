package com.openjarvis.ui

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.widget.FrameLayout
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlin.math.abs

class MahiruAvatarView(
    context: Context,
    private val onAvatarTapped: () -> Unit
) : FrameLayout(context) {

    private var downX = 0f
    private var downY = 0f

    private val sceneView = SceneView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }

    init {
        addView(sceneView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        sceneView.modelLoader.createModelInstance(
            assetFileLocation = "models/Mahiru_Shiina.glb"
        )?.let { modelInstance ->
            val avatarNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 1.4f,
                centerOrigin = Position(0f, -0.7f, 0f)
            ).apply {
                playAnimation(0)
            }
            sceneView.addChildNode(avatarNode)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
            MotionEvent.ACTION_UP -> {
                val movedX = abs(ev.x - downX)
                val movedY = abs(ev.y - downY)
                if (movedX < 15f && movedY < 15f) {
                    onAvatarTapped()
                }
            }
        }
        return false
    }

    fun destroy() {
        sceneView.destroy()
    }
}
