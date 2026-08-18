package ru.netology.nework.util

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

sealed interface SwipeDirection {
    object Left : SwipeDirection
    object Right : SwipeDirection
    object Up : SwipeDirection
    object Down : SwipeDirection
}

typealias SwipeListener = (SwipeDirection) -> Unit

private const val SWIPE_DISTANCE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

@SuppressLint("ClickableViewAccessibility")
fun View.detectSwipe(
    distanceThreshold: Int = SWIPE_DISTANCE_THRESHOLD,
    velocityThreshold: Int = SWIPE_VELOCITY_THRESHOLD,
    listener: SwipeListener,
) {
    val detector = GestureDetector(
        context,
        createGestureListener(this, listener, distanceThreshold, velocityThreshold)
    )
    setOnTouchListener { _, event -> detector.onTouchEvent(event) }
}

private fun createGestureListener(
    view: View,
    listener: SwipeListener,
    distanceThreshold: Int,
    velocityThreshold: Int
): GestureDetector.SimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        view.performClick()
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean = determineSwipeDirection(
        e1 = e1,
        e2 = e2,
        velocityX = velocityX,
        velocityY = velocityY,
        distanceThreshold = distanceThreshold,
        velocityThreshold = velocityThreshold
    )?.let {
        listener(it)
        true
    } ?: false

    override fun onDown(e: MotionEvent): Boolean = true
}

private fun determineSwipeDirection(
    e1: MotionEvent?,
    e2: MotionEvent,
    velocityX: Float,
    velocityY: Float,
    distanceThreshold: Int,
    velocityThreshold: Int
): SwipeDirection? {
    val distanceX = e2.x - (e1?.x ?: 0f)
    val distanceY = e2.y - (e1?.y ?: 0f)

    return when {
        (abs(distanceX) > abs(distanceY)) &&
                (abs(distanceX) > distanceThreshold) &&
                (abs(velocityX) > velocityThreshold) -> {
            if (distanceX > 0) SwipeDirection.Right else SwipeDirection.Left
        }

        (abs(distanceY) > abs(distanceX)) &&
                (abs(distanceY) > distanceThreshold) &&
                (abs(velocityY) > velocityThreshold) -> {
            if (distanceY > 0) SwipeDirection.Down else SwipeDirection.Up
        }

        else -> null
    }
}