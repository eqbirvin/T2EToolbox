package com.t2e.toolbox.eqbirvin.data

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "FlashlightRepository"

class FlashlightRepository(
    private val context: Context,
    private val settingsRepo: SettingsRepository
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private var primaryCameraId: String? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            Log.d(TAG, "onTorchModeChanged: cameraId=$cameraId, enabled=$enabled")
            if (primaryCameraId == null || cameraId == primaryCameraId) {
                _isFlashlightOn.value = enabled
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            Log.d(TAG, "onTorchModeUnavailable: cameraId=$cameraId")
            if (cameraId == primaryCameraId) {
                _isFlashlightOn.value = false
            }
        }
    }

    init {
        findPrimaryCameraId()
        registerTorchCallback()
    }

    private fun findPrimaryCameraId() {
        try {
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
            for (id in cameraIds) {
                val chars = cameraManager?.getCameraCharacteristics(id)
                val flashAvailable = chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars?.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    primaryCameraId = id
                    break
                }
            }
            if (primaryCameraId == null && cameraIds.isNotEmpty()) {
                primaryCameraId = cameraIds.firstOrNull()
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException finding camera with flash", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException finding camera with flash", e)
        }
    }

    private fun registerTorchCallback() {
        try {
            cameraManager?.registerTorchCallback(
                { runnable -> runnable.run() },
                torchCallback
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException registering torch callback", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException registering torch callback", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException registering torch callback", e)
        }
    }

    suspend fun toggleFlashlight(): Boolean = withContext(Dispatchers.IO) {
        val target = !_isFlashlightOn.value
        setFlashlight(target)
    }

    suspend fun setFlashlight(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cm = cameraManager ?: return@withContext false
        val camId = primaryCameraId ?: return@withContext false
        try {
            cm.setTorchMode(camId, enabled)
            _isFlashlightOn.value = enabled
            true
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException setting torch mode to $enabled", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException setting torch mode to $enabled", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting torch mode to $enabled", e)
            false
        }
    }
}
