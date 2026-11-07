package com.carmabs.emanate.ui.environment

/**
 * The environment of the build that is running: mock, dev or pro.
 *
 * Each flavor of the app creates its own and injects it, so the ui only reads it and doesn't know the flavors.
 * @param tone the color family of its label, so the environments can be told apart at a glance, or
 * [EnvironmentTone.Invisible] when nothing must tell the user about the environment, like in the real build
 */
data class AppEnvironment(
    val name: String,
    val tone: EnvironmentTone = EnvironmentTone.Tertiary
)

enum class EnvironmentTone { Tertiary, Secondary, Invisible }
