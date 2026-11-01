package com.carmabs.emanate.compose.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.carmabs.emanate.android.permission.EmaAndroidPermissionManager
import com.carmabs.emanate.android.permission.EmaContractMultiplePermission
import com.carmabs.emanate.android.permission.EmaContractSinglePermission
import com.carmabs.emanate.compose.extension.activity
import com.carmabs.emanate.compose.utils.Compose
import com.carmabs.emanate.core.manager.EmaPermissionManager
import com.carmabs.emanate.core.manager.PermissionState
import com.carmabs.emanate.core.model.EmaMultiplePermissionRequest
import com.carmabs.emanate.core.model.EmaPermissionRequest
import com.carmabs.emanate.core.utils.EmaUtils
import kotlinx.coroutines.CoroutineScope

/**
 * Created by Carlos Mateo Benito on 15/9/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Composable
fun rememberEmaPermissionManager(): EmaPermissionManager {
    val permissionManager: EmaPermissionManager = if (EmaUtils.Preview.Compose.isInPreview()) {
        EmaPreviewPermissionManager
    } else {
        val context = LocalContext.current

        val activity = LocalContext.activity

        val singleContract = remember {
            EmaContractSinglePermission {
                activity.shouldShowRequestPermissionRationale(it)
            }
        }
        val multipleContract = remember {
            EmaContractMultiplePermission {
                activity.shouldShowRequestPermissionRationale(it)
            }
        }

        val singlePermissionManager = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = singleContract.contract
        )

        val multiplePermissionManager = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = multipleContract.contract
        )
        remember {
            EmaAndroidPermissionManager(
                context = context,
                activitySinglePermissionResultLauncher = singlePermissionManager,
                activityMultiplePermissionResultLauncher = multiplePermissionManager,
                contractSinglePermission = singleContract,
                contractMultiplePermission = multipleContract
            )
        }
    }

    return permissionManager
}

private object EmaPreviewPermissionManager : EmaPermissionManager {
    override suspend fun requestPermission(permission: String): PermissionState = PermissionState.GRANTED

    override suspend fun requestMultiplePermission(vararg permission: String): Map<String, PermissionState> =
        permission.associateWith {
            PermissionState.GRANTED
        }

    override fun isPermissionGranted(permission: String): PermissionState = PermissionState.GRANTED

    override fun areAllPermissionsGranted(vararg permission: String): Boolean = true

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean = true

    override suspend fun requestCoarseLocationPermission(): PermissionState = PermissionState.GRANTED

    override suspend fun requestFineLocationPermission(): PermissionState = PermissionState.GRANTED

    override fun isLocationFineGranted(): PermissionState = PermissionState.GRANTED

    override fun isLocationBackgroundGranted(): PermissionState = PermissionState.GRANTED

    override fun isLocationCoarseGranted(): PermissionState = PermissionState.GRANTED

    override fun handleRequest(request: EmaPermissionRequest, scope: CoroutineScope, permission: String) {
        request.onPermissionResponse.invoke(PermissionState.GRANTED)
    }

    override fun responseRequest(request: EmaPermissionRequest, permissionState: PermissionState) {
        request.onPermissionResponse.invoke(permissionState)
    }

    override fun handleRequestMultiple(
        request: EmaMultiplePermissionRequest,
        scope: CoroutineScope,
        vararg permission: String
    ) {
        request.onPermissionResponse.invoke(permission.associateWith { PermissionState.GRANTED })
    }

    override fun responseRequest(request: EmaMultiplePermissionRequest, permissionMap: Map<String, PermissionState>) {
        request.onPermissionResponse.invoke(permissionMap)
    }
}
