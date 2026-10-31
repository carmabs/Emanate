package com.carmabs.emanate.compose.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.carmabs.emanate.compose.extension.activity
import com.carmabs.emanate.compose.extension.navigateBack
import com.carmabs.emanate.core.navigator.EmaNavigationNode
import com.carmabs.emanate.core.state.EmaEvent

@Composable
fun <D : EmaEvent> rememberEmaNodeNavigator(
    navHostController: NavHostController,
    navigatorProvider: (Activity, NavHostController) -> EmaComposableNodeNavigator<D>
): EmaComposableNodeNavigator<D> {
    val activity = LocalContext.activity
    val navigator = remember {
        navigatorProvider.invoke(activity, navHostController)
    }
    return navigator
}

abstract class EmaComposableNodeNavigator<D : EmaEvent>(
    protected val activity: Activity,
    protected val navController: NavHostController
) {

    private var previousNavigationNode: EmaNavigationNode<D>? = null

    fun navigate(navigationNode: EmaNavigationNode<D>, onNavigated: ((D) -> Unit)? = null) {
        if (navigationNode.id != previousNavigationNode?.id) {
            val destination = navigationNode.value
            if (previousNavigationNode?.hasPreviousNode(node = navigationNode) == true) {
                previousNavigationNode = navigationNode
                navigateBack()
            } else {
                previousNavigationNode = navigationNode
                onNavigation(destination)
                onNavigated?.invoke(destination)
            }
        }
    }

    /**
     * Pops the back stack, and closes the activity when there is nothing left and [closeActivityWhenBackstackIsEmpty].
     */
    fun navigateBack(closeActivityWhenBackstackIsEmpty: Boolean = true): Boolean =
        navController.navigateBack(closeActivityWhenBackstackIsEmpty)

    abstract fun onNavigation(navigationEvent: D): Boolean
}
