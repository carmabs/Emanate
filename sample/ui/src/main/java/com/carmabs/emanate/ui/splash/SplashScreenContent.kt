package com.carmabs.emanate.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.action.EmaActionDispatcher
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.sample.ui.R
import com.carmabs.emanate.ui.base.compose.BaseScreenComposable
import com.carmabs.emanate.ui.theme.EmaSampleTheme

/**
 * First screen of the sample: the logo appears while the view model waits before opening the login
 */
class SplashScreenContent : BaseScreenComposable<EmaState.EMPTY, EmaAction.EMPTY>() {

    @Composable
    override fun onState(state: EmaState.EMPTY, actions: EmaActionDispatcher<EmaAction.EMPTY>) {
        val entrance = remember { Animatable(0f) }
        LaunchedEffect(Unit) { entrance.animateTo(1f, tween(durationMillis = 600)) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.screen_padding))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(entrance.value)
                    .graphicsLayer { translationY = (1f - entrance.value) * 24.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(96.dp),
                    painter = painterResource(id = R.drawable.ic_ema_logo),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(id = R.string.ema_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.ema_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
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

    @Preview
    @Composable
    private fun PreviewNormal() {
        EmaSampleTheme {
            onState(state = EmaState.EMPTY, actions = EmaActionDispatcher.EMPTY)
        }
    }
}
