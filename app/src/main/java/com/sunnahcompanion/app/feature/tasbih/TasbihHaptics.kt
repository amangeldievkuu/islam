package com.sunnahcompanion.app.feature.tasbih

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Distinct vibration patterns so counting works eyes-free, phone in hand or pocket. */
@Singleton
class TasbihHaptics @Inject constructor(@ApplicationContext context: Context) {

    private val vibrator: Vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    private val tick: VibrationEffect =
        if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
            VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f).compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }

    private val milestone: VibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1)

    private val complete: VibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)

    // Touch usage respects the user's system haptics setting; hardware feedback is for milestones,
    // which carry information the user can't see when counting eyes-free.
    private val touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    private val hardware = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)

    fun perform(feedback: TasbihFeedback) {
        if (!vibrator.hasVibrator()) return
        when (feedback) {
            TasbihFeedback.TICK -> vibrator.vibrate(tick, touch)
            TasbihFeedback.MILESTONE -> vibrator.vibrate(milestone, hardware)
            TasbihFeedback.COMPLETE -> vibrator.vibrate(complete, hardware)
        }
    }
}
