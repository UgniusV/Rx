package com.dandy.ugnius.dandy.login.views.components

import android.content.Context
import android.graphics.Color.RED
import android.util.AttributeSet
import android.graphics.Color.WHITE
import android.os.Handler
import android.widget.LinearLayout
import com.dandy.ugnius.dandy.R
import com.dandy.ugnius.dandy.fade
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.loading_animation.view.*

class LoadingView constructor(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private companion object {
        const val LOOP_FRAME = 120
        const val ERROR_START_FRAME = 658
        const val ERROR_END_FRAME = 820
    }

    private val subject = PublishSubject.create<Int>()
    private var disposable: Disposable? = null

    init {
        inflate(context, R.layout.loading_animation, this) as LinearLayout
        loadingAnimation.speed = 1.5F
        loadingAnimation.addAnimatorUpdateListener {
            val frame = loadingAnimation.frame
            if (frame == LOOP_FRAME) {
                subject.onNext(LOOP_FRAME)
            } else if (frame == ERROR_END_FRAME) {
                cancelAnimation()
            }
        }
    }

    fun playLoadingAnimation(message: String) {
        information.text = message
        information.setTextColor(WHITE)
        loadingAnimation.setMinAndMaxFrame(1, 120)
        loadingAnimation.playAnimation()
        fade(views = *arrayOf(loadingAnimation, information), values = floatArrayOf(0F, 0.5F, 1F))
    }

    fun playErrorAnimation(message: String, onStop: () -> Unit = {}) {
        disposable = subject.subscribe {
            loadingAnimation.setMinAndMaxFrame(ERROR_START_FRAME, ERROR_END_FRAME)
            Handler().postDelayed(
                {
                    information.setTextColor(RED)
                    information.text = message
                }
                , 700
            )
            fade(views = *arrayOf(loadingAnimation, information),
                values = floatArrayOf(1F, 0.5F, 0F),
                delay = 2000,
                onStop = { onStop() }
            )
        }
    }

    private fun cancelAnimation() {
        loadingAnimation.cancelAnimation()
        disposable?.dispose()
    }

}
