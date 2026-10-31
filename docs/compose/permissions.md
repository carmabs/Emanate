# Permissions (Android)

`EmaPermissionManager` wraps the Android permission flow in `suspend` functions and a small request model that fits
the state/ViewModel flow. The permission result is one of three values:

| `PermissionState`             | Meaning                                                              |
|-------------------------------|----------------------------------------------------------------------|
| `GRANTED`                     | The permission is granted.                                           |
| `NOT_GRANTED`                 | Not granted.                                                         |
| `NOT_GRANTED_SHOULD_EXPLAIN`  | Not granted, and Android recommends showing a rationale.             |

Declare the permissions in the manifest as usual.

## The manager

The screen creates the manager with `rememberEmaPermissionManager()`. In a preview it returns a manager that grants
everything.

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

The ViewModel does not touch Android, so it **puts the need of the permission in the state** as plain data, and the
view executes it. The answer comes back as an action, which the reducers and side effects handle like any other.
The reducers stay pure: the state only says that the permission is being requested, and the view creates the
`EmaPermissionRequest`, with the callback that dispatches the answer.

```kotlin
data class LocationState(
    val requestingPermission: Boolean = false,
    val locationGranted: Boolean = false
) : EmaState

sealed interface LocationAction : EmaAction {
    data object AllowClicked : LocationAction
    data class PermissionAnswered(val permissionState: PermissionState) : LocationAction
}
```

```kotlin
class LocationViewModel : EmaViewModel<LocationState, LocationAction, LocationEvent>(LocationState()) {

    override fun setup() = source {
        reducers {
            add<LocationAction> { action ->
                when (action) {
                    is LocationAction.AllowClicked -> state.copy(requestingPermission = true)
                    is LocationAction.PermissionAnswered -> state.copy(
                        // The request is done, so it isn't executed again
                        requestingPermission = false,
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

The screen turns the state into a request, as shown [below](#executing-the-request-in-the-screen).
`permissionManager.handleRequest(request, scope, permission)` executes it, and ignores
`EmaPermissionRequest.cancelRequest()`, the "nothing to do" request. `EmaMultiplePermissionRequest` and
`handleRequestMultiple` work the same way for several permissions.

As the answer is an action, a test can dispatch `PermissionAnswered` directly to check every outcome, without a device.

If the process is killed while the user is in the system settings, keep what the user typed with a
[backup](../guides/process-death.md).

## Executing the request in the screen

The ViewModel puts a flag in the state, and the screen creates the `EmaPermissionRequest` and executes it every time
the flag changes:

```kotlin
@Composable
override fun onState(state: LocationState, actions: EmaActionDispatcher<LocationAction>) {
    val permissionManager = rememberEmaPermissionManager()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.requestingPermission) {
        val request = if (state.requestingPermission) {
            // The answer of the user is dispatched as an action
            EmaPermissionRequest.createRequest { actions.dispatch(LocationAction.PermissionAnswered(it)) }
        } else {
            EmaPermissionRequest.cancelRequest()
        }
        permissionManager.handleRequest(request, scope, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
```

The suspend functions of the manager can also be called from a coroutine of the screen:

```kotlin
scope.launch {
    val cameraState = permissionManager.requestPermission(Manifest.permission.CAMERA)
}
```
