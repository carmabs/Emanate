package com.carmabs.emanate.presentation.profile.creation

import kotlinx.serialization.Serializable

/**
 * Fields of the form saved by the screen, so they are restored after the system kills the process of the app, for
 * example while the user changes a permission in the settings. The dialogs are closed, so they aren't saved.
 */
@Serializable
data class ProfileCreationForm(val name: String, val surname: String)
