package com.carmabs.emanate.compose.extension

import com.carmabs.emanate.compose.ui.EmaComposableScreenContent
import kotlin.reflect.KClass

val <T : EmaComposableScreenContent<*, *>> KClass<T>.routeId: String
    get() = "screen/${this.java.name}"
