@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.permission

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * Creates an [EmaAndroidPermissionManager] for a Fragment. Create it as a property of the Fragment, because the
 * activity result launchers must be registered before the Fragment is started.
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
fun EmaAndroidPermissionManager(fragment: Fragment): EmaAndroidPermissionManager = EmaAndroidPermissionManager(
    contextProvider = { fragment.requireContext() },
    shouldShowRequestPermissionRationale = { fragment.shouldShowRequestPermissionRationale(it) },
    registerSinglePermission = { contract ->
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission(), contract.contract)
    },
    registerMultiplePermission = { contract ->
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), contract.contract)
    }
)
