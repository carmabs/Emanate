package com.carmabs.emanate.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.compose.ui.EmaScreenContent
import com.carmabs.emanate.ui.compose.EnvironmentLabel
import com.carmabs.emanate.ui.environment.AppEnvironment
import com.carmabs.emanate.ui.utils.RenderPreview

/**
 * First screen of the sample: the logo appears while the view model waits before opening the login
 */
class SplashScreenContent(
    private val environment: AppEnvironment,
    /**
     * Called when the logo has been drawn, so the system splash, which has the same logo, can be removed
     */
    private val onLogoDrawn: () -> Unit = {}
) : EmaScreenContent<EmaState.EMPTY, EmaAction.EMPTY> {

    @Composable
    override fun onState(state: EmaState.EMPTY, actions: EmaActionDispatcher<EmaAction.EMPTY>) {
        val entrance = remember { Animatable(0f) }
        LaunchedEffect(Unit) { entrance.animateTo(1f, tween(durationMillis = 600)) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.screen_padding))
        ) {
            // The logo is still, in the center and with the size of the icon of the system splash, which is replaced by
            // this screen without a jump: only what is around the logo appears
            Image(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(LOGO_SIZE)
                    .drawWithContent {
                        drawContent()
                        onLogoDrawn()
                    },
                painter = painterResource(id = R.drawable.ic_ema_logo),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = NAME_OFFSET)
                    .alpha(entrance.value)
                    .graphicsLayer { translationY = (1f - entrance.value) * 16.dp.toPx() },
                text = stringResource(id = R.string.ema_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Below the name, so the environment is known from the first screen
            EnvironmentLabel(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = ENVIRONMENT_OFFSET)
                    .alpha(entrance.value),
                environment = environment
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .alpha(entrance.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.width(96.dp))
                Text(
                    text = stringResource(id = R.string.ema_by),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private companion object {
        // Must match the logo of ic_splash_logo, the icon of the system splash
        val LOGO_SIZE = 144.dp

        // The name goes below the logo, which has its content in the center of its square
        val NAME_OFFSET = 92.dp

        // The label of the environment goes below the name
        val ENVIRONMENT_OFFSET = 136.dp
    }
}

@PreviewLightDark
@Composable
private fun PreviewNormal() {
    RenderPreview {
        SplashScreenContent(AppEnvironment("mock")).onState(state = EmaState.EMPTY, actions = EmaActionDispatcher.EMPTY)
    }
}
