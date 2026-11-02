package com.carmabs.emanate.serialization

import com.carmabs.emanate.core.Emanate
import com.carmabs.emanate.core.action.EmaAction
import com.carmabs.emanate.core.savestate.EmaStateStore
import com.carmabs.emanate.core.savestate.backupOf
import com.carmabs.emanate.core.state.EmaEvent
import com.carmabs.emanate.core.state.EmaState
import com.carmabs.emanate.core.viewmodel.EmaViewModel
import com.carmabs.emanate.test.resetTestConfiguration
import com.carmabs.emanate.test.testStateStore
import com.carmabs.emanate.test.useTestConfiguration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private data class LoginState(
    val userName: String = "",
    val rememberMe: Boolean = false,
    val password: String = "",
    val recentUsers: List<String> = emptyList()
) : EmaState

@Serializable
private data class LoginForm(val userName: String, val rememberMe: Boolean)

private sealed interface LoginAction : EmaAction {
    data class UserNameWritten(val userName: String) : LoginAction
    data class RememberMeChecked(val rememberMe: Boolean) : LoginAction
    data class PasswordWritten(val password: String) : LoginAction
    data class UserLoggedIn(val userName: String) : LoginAction
}

/**
 * Saves the form without the password, with the type of the codec inferred from save, and the recent users with an
 * explicit serializer
 */
private class LoginViewModel(private val store: EmaStateStore) :
    EmaViewModel<LoginState, LoginAction, EmaEvent.EMPTY>(LoginState(), Emanate.configuration.copy(logger = null)) {

    override fun setup() = source {
        plugins {
            +backupOf(store) {
                add(
                    key = "form",
                    codec = json(),
                    save = { LoginForm(userName, rememberMe) },
                    restore = { form -> copy(userName = form.userName, rememberMe = form.rememberMe) }
                )
                add(
                    key = "recentUsers",
                    codec = json(ListSerializer(String.serializer())),
                    save = { recentUsers.takeIf { it.isNotEmpty() } },
                    restore = { copy(recentUsers = it) }
                )
            }
        }
        reducers {
            add<LoginAction> { action ->
                when (action) {
                    is LoginAction.UserNameWritten -> state.copy(userName = action.userName)
                    is LoginAction.RememberMeChecked -> state.copy(rememberMe = action.rememberMe)
                    is LoginAction.PasswordWritten -> state.copy(password = action.password)
                    is LoginAction.UserLoggedIn -> state.copy(recentUsers = state.recentUsers + action.userName)
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EmaJsonCodecTest {

    @BeforeTest
    fun setUp() {
        useTestConfiguration()
    }

    @AfterTest
    fun tearDown() {
        resetTestConfiguration()
    }

    @Test
    fun `the values are saved in JSON`() = runTest {
        val store = testStateStore()
        val viewModel = LoginViewModel(store)

        viewModel.onCreated(null)
        viewModel.dispatch(LoginAction.UserNameWritten("ana"))
        viewModel.dispatch(LoginAction.RememberMeChecked(true))
        viewModel.dispatch(LoginAction.PasswordWritten("secret"))
        viewModel.dispatch(LoginAction.UserLoggedIn("ana"))
        advanceUntilIdle()

        assertEquals(
            mapOf("form" to """{"userName":"ana","rememberMe":true}""", "recentUsers" to """["ana"]"""),
            store.values
        )
        viewModel.close()
    }

    @Test
    fun `the values are restored`() = runTest {
        val store = testStateStore()
        store.write("form", """{"userName":"ana","rememberMe":true}""")
        store.write("recentUsers", """["ana","luis"]""")
        val viewModel = LoginViewModel(store)

        viewModel.onCreated(null)
        advanceUntilIdle()

        assertEquals(
            LoginState(userName = "ana", rememberMe = true, recentUsers = listOf("ana", "luis")),
            viewModel.stateFlow.value
        )
        viewModel.close()
    }

    @Test
    fun `a value that can't be decoded isn't restored`() = runTest {
        val store = testStateStore()
        store.write("form", """{"name":"ana"}""")
        store.write("recentUsers", "not json")
        val viewModel = LoginViewModel(store)

        viewModel.onCreated(null)
        advanceUntilIdle()

        assertEquals(LoginState(), viewModel.stateFlow.value)
        assertEquals(mapOf("form" to """{"userName":"","rememberMe":false}"""), store.values)
        viewModel.close()
    }

    @Test
    fun `the codec can be used on its own`() {
        val codec = json<LoginForm>()

        assertEquals("""{"userName":"ana","rememberMe":false}""", codec.encode(LoginForm("ana", false)))
        assertEquals(LoginForm("ana", false), codec.decode("""{"userName":"ana","rememberMe":false}"""))
        assertNull(codec.decode("{}"))
    }
}
