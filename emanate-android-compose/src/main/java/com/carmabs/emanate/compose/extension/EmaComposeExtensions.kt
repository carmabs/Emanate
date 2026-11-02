package com.carmabs.emanate.compose.extension

import com.carmabs.emanate.compose.ui.EmaScreenContent
import kotlin.reflect.KClass

val <T : EmaScreenContent<*, *>> KClass<T>.routeId: String
    get() = "screen/${this.java.name}"
