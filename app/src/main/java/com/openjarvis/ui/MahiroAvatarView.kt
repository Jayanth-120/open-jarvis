package com.openjarvis.service

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.widget.FrameLayout
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

class MahiroAvatarView(
    context: Context,
    private val onAvatarTapped: () -> Unit
) : FrameLayout(context) {

    private val sceneView = SceneView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        isOpaque = false
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
        if (ev.action == MotionEvent.ACTION_UP) {
            onAvatarTapped()
        }
        return false
    }

    fun destroy() {
        sceneView.destroy()
    }
}
