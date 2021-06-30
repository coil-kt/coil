package coil.compose

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val STATE_LOADED = 0
private const val STATE_EMPTY = 1

@Composable
internal fun updateFadeInTransition(key: Any, durationMillis: Int): FadeInTransition {
    // Create our transition state, which allow us to control the state and target states.
    val transitionState = remember(key) {
        MutableTransitionState(STATE_EMPTY).apply {
            targetState = STATE_LOADED
        }
    }

    // Our actual transition, which reads our transitionState.
    val transition = updateTransition(
        transitionState = transitionState,
        label = "Image fadeIn"
    )

    // Alpha animates over the first 50%.
    val alpha = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis / 2) },
        targetValueByState = { if (it == STATE_LOADED) 1f else 0f },
        label = "Image fadeIn alpha",
    )

    // Brightness animates over the first 75%.
    val brightness = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis * 3 / 4) },
        targetValueByState = { if (it == STATE_LOADED) 1f else 0.8f },
        label = "Image fadeIn brightness",
    )

    // Saturation animates over whole duration.
    val saturation = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis) },
        targetValueByState = { if (it == STATE_LOADED) 1f else 0f },
        label = "Image fadeIn saturation",
    )

    return remember(transition) {
        FadeInTransition(alpha, brightness, saturation)
    }.apply {
        isFinished = transitionState.currentState == STATE_LOADED
    }
}

@Stable
internal class FadeInTransition(
    alpha: State<Float> = mutableStateOf(0f),
    brightness: State<Float> = mutableStateOf(0f),
    saturation: State<Float> = mutableStateOf(0f),
) {
    val alpha by alpha
    val brightness by brightness
    val saturation by saturation
    var isFinished by mutableStateOf(false)
}
