package com.carmabs.emanate.test

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.initializer.EmaInitializer
import com.carmabs.emanate.core.source.EmaSourcePlugin
import com.carmabs.emanate.core.source.SourceSetupScope
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaLifecycle
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private data class CartState(val items: Int = 0) : EmaState

private sealed interface CartAction : EmaAction {
    data object Add : CartAction
}

private data class CartInitializer(val items: Int) : EmaInitializer

private data class ItemBought(val id: Int) : EmaAction

private data class CouponApplied(val code: String) : EmaAction

/**
 * Implemented by actions of any screen, so it may be one of the actions of the cart
 */
private interface TrackedAction : EmaAction

private class CartViewModel :
    EmaViewModel<CartState, CartAction, EmaEvent.EMPTY>(CartState(), Emanate.configuration.copy(logger = null)) {

    override fun setup() = source {
        middlewares {
            add<EmaAction> { next() }
            add<TrackedAction> { next() }
        }
        reducers {
            add<CartAction> { state.copy(items = state.items + 1) }
            add<CartInitializer> { action -> state.copy(items = action.items) }
            add<ItemBought> { state.copy(items = state.items + 1) }
        }
        sideEffects {
            add<EmaLifecycle.Resumed> { }
        }
        broadcasts {
            listen<ItemBought>()
        }
    }
}

/**
 * Forgets to listen to the broadcast actions it handles
 */
private class ForgetfulCartViewModel :
    EmaViewModel<CartState, CartAction, EmaEvent.EMPTY>(CartState(), Emanate.configuration.copy(logger = null)) {

    override fun setup() = source {
        reducers {
            add<CouponApplied> { state }
        }
        sideEffects {
            add<ItemBought> { }
        }
    }
}

private sealed interface DiscountAction : EmaAction {
    data object Applied : DiscountAction
}

/**
 * Plugin that applies a discount when the cart is created
 */
private class DiscountPlugin : EmaSourcePlugin<CartState, DiscountAction> {
    override fun install(setup: SourceSetupScope<CartState, DiscountAction, Nothing>) {
        setup.sideEffects {
            add<CartInitializer> { dispatch(DiscountAction.Applied) }
        }
    }
}

/**
 * Handles the actions of a plugin, installed or not
 */
private class DiscountCartViewModel(private val withPlugin: Boolean) :
    EmaViewModel<CartState, CartAction, EmaEvent.EMPTY>(CartState(), Emanate.configuration.copy(logger = null)) {

    override fun setup() = source {
        if (withPlugin) {
            plugins { add(DiscountPlugin()) }
        }
        reducers {
            add<DiscountAction.Applied> { state }
        }
    }
}

private abstract class BaseViewModel<S : EmaState, A : EmaAction>(initialState: S) :
    EmaViewModel<S, A, EmaEvent.EMPTY>(initialState, Emanate.configuration.copy(logger = null))

/**
 * The type of its actions is given to EmaViewModel through a base class
 */
private class InheritedCartViewModel : BaseViewModel<CartState, CartAction>(CartState()) {

    override fun setup() = source {
        reducers {
            add<CartAction> { state }
            add<CouponApplied> { state }
        }
    }
}

class SetupAssertionsTest {

    @Test
    fun `a setup whose pieces receive actions passes`() {
        CartViewModel().assertSetup()
    }

    @Test
    fun `the pieces of broadcast actions that aren't listened to are reported`() {
        val error = assertFailsWith<AssertionError> { ForgetfulCartViewModel().assertSetup() }

        assertEquals(
            "The reducer of CouponApplied never runs: ForgetfulCartViewModel doesn't receive CouponApplied. If it is " +
                "broadcast by another source, add listen<CouponApplied>() in the broadcasts block.\n" +
                "The side effect of ItemBought never runs: ForgetfulCartViewModel doesn't receive ItemBought. If it " +
                "is broadcast by another source, add listen<ItemBought>() in the broadcasts block.",
            error.message
        )
    }

    @Test
    fun `the pieces of the actions of an installed plugin receive them`() {
        DiscountCartViewModel(withPlugin = true).assertSetup()
    }

    @Test
    fun `the pieces of the actions of a plugin that isn't installed are reported`() {
        val error = assertFailsWith<AssertionError> { DiscountCartViewModel(withPlugin = false).assertSetup() }

        assertEquals(true, error.message?.startsWith("The reducer of Applied never runs"))
    }

    @Test
    fun `the type of the actions is found through the base classes`() {
        val error = assertFailsWith<AssertionError> { InheritedCartViewModel().assertSetup() }

        assertEquals(1, error.message?.lines()?.size)
        assertEquals(true, error.message?.startsWith("The reducer of CouponApplied never runs"))
    }
}
