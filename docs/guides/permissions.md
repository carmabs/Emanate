# Permissions

`EmaPermissionManager` wraps the Android permission flow in `suspend` functions and a small request model that fits
the state/ViewModel flow. The permission result is one of three values:

| `PermissionState`             | Meaning                                                              |
|-------------------------------|----------------------------------------------------------------------|
| `GRANTED`                     | The permission is granted.                                           |
| `NOT_GRANTED`                 | Not granted.                                                         |
| `NOT_GRANTED_SHOULD_EXPLAIN`  | Not granted, and Android recommends showing a rationale.             |

Declare the permissions in the manifest as usual.

## The manager

The view creates the manager with `rememberEmaPermissionManager()`. See [Compose permissions](../compose/permissions.md).

```kotlin
permissionManager.isPermissionGranted(Manifest.permission.CAMERA)   // PermissionState
permissionManager.areAllPermissionsGranted(a, b)                    // Boolean
permissionManager.shouldShowRequestPermissionRationale(permission)  // Boolean
permissionManager.requestPermission(permission)                     // suspend → PermissionState
permissionManager.requestMultiplePermission(a, b)                   // suspend → Map<String, PermissionState>
```

Make one request at a time. Android cancels a request made while another one is on screen: it returns `NOT_GRANTED`
(an empty map for several permissions) instead of waiting.

### Location

There are shortcuts for location: `requestCoarseLocationPermission()`, `requestFineLocationPermission()`,
`isLocationCoarseGranted()`, `isLocationFineGranted()` and `isLocationBackgroundGranted()`.

Request the fine location with `requestFineLocationPermission()`, not with `requestPermission`: depending on the Android
version it must be requested together with the coarse location. `requestBackgroundLocationPermission(infoDialogType)` of
`EmaAndroidPermissionManager` requests the background location, which needs the fine location first. On recent Android
versions the user grants it in the system settings, and `infoDialogType` decides the explanation shown before opening
them: `InfoDialogType.Default(title, message)`, or `InfoDialogType.CustomDialog(alertDialog, onAcceptClickListener)`
to show your own dialog.

## Asking for a permission from the ViewModel

The ViewModel does not touch Android, so it **puts the request in the state**, and the view executes it. The answer
comes back as an action, which the reducers and side effects handle like any other:

```kotlin
data class LocationState(
    val permissionRequest: EmaPermissionRequest = EmaPermissionRequest.cancelRequest(),
    val locationGranted: Boolean = false
) : EmaState

sealed interface LocationAction : EmaAction {
    data object AllowClicked : LocationAction
    data class PermissionAnswered(val permissionState: PermissionState) : LocationAction
}
```

```kotlin
class LocationViewModel : EmaViewModel<LocationState, LocationAction, LocationEvent>(LocationState()) {

    // Created once: the answer of the user is dispatched as an action
    private val locationRequest = EmaPermissionRequest.createRequest { permissionState ->
        dispatch(LocationAction.PermissionAnswered(permissionState))
    }

    override fun setup() = source {
        reducers {
            add<LocationAction> { action ->
                when (action) {
                    is LocationAction.AllowClicked -> state.copy(permissionRequest = locationRequest)
                    is LocationAction.PermissionAnswered -> state.copy(
                        // Clears the request, so it isn't executed again
                        permissionRequest = EmaPermissionRequest.cancelRequest(),
                        locationGranted = action.permissionState == PermissionState.GRANTED
                    )
                }
            }
        }
        sideEffects {
            add<LocationAction.PermissionAnswered> { action ->
                if (action.permissionState == PermissionState.NOT_GRANTED_SHOULD_EXPLAIN) {
                    postEvent(LocationEvent.PermissionRationaleNeeded)
                }
            }
        }
    }
}
```

The view calls `permissionManager.handleRequest(request, scope, permission)` every time the request in the state changes.
`EmaPermissionRequest.cancelRequest()` is the "nothing to do" request, which `handleRequest` ignores.
`EmaMultiplePermissionRequest` and `handleRequestMultiple` work the same way for several permissions.

As the answer is an action, a test can dispatch `PermissionAnswered` directly to check every outcome, without a device.

If the process is killed while the user is in the system settings, keep what the user typed with a
[backup](process-death.md).
