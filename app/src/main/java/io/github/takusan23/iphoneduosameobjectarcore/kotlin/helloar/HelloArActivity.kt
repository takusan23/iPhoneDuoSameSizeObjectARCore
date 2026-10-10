/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.takusan23.iphoneduosameobjectarcore.kotlin.helloar

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import io.github.takusan23.iphoneduosameobjectarcore.java.common.helpers.CameraPermissionHelper
import io.github.takusan23.iphoneduosameobjectarcore.java.common.helpers.DepthSettings
import io.github.takusan23.iphoneduosameobjectarcore.java.common.helpers.FullScreenHelper
import io.github.takusan23.iphoneduosameobjectarcore.java.common.helpers.InstantPlacementSettings
import io.github.takusan23.iphoneduosameobjectarcore.java.common.samplerender.SampleRender
import io.github.takusan23.iphoneduosameobjectarcore.kotlin.common.helpers.ARCoreSessionLifecycleHelper

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
class HelloArActivity : ComponentActivity() {
    companion object {
        private const val TAG = "HelloArActivity"
    }

    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper
    lateinit var view: HelloArView
    lateinit var renderer: HelloArRenderer

    val instantPlacementSettings = InstantPlacementSettings()
    val depthSettings = DepthSettings()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ARCore session lifecycle helper and configuration.
        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
        // If Session creation or Session.resume() fails, display a message and log detailed
        // information.
        arCoreSessionHelper.exceptionCallback =
            { exception ->
                val message =
                    when (exception) {
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"

                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        else -> "Failed to create AR session: $exception"
                    }
                Log.e(TAG, "ARCore threw an exception", exception)
                view.snackbarHelper.showError(this, message)
            }

        // Configure session features, including: Lighting Estimation, Depth mode, Instant Placement.
        arCoreSessionHelper.beforeSessionResume = ::configureSession
        lifecycle.addObserver(arCoreSessionHelper)

        // Set up the Hello AR renderer.
        renderer = HelloArRenderer(this)
        lifecycle.addObserver(renderer)

        // Set up Hello AR UI.
        view = HelloArView(this)
        lifecycle.addObserver(view)
        setContent {
            // 既にある view は使いまわす + 自作部分だけ compose
            Box {
                AndroidView(factory = { view.root })

                val isShowObjSelectBottomSheet = remember { mutableStateOf(false) }
                if (isShowObjSelectBottomSheet.value) {
                    ModalBottomSheet(onDismissRequest = { isShowObjSelectBottomSheet.value = false }) {
                        LazyColumn {
                            items(HelloArRenderer.OBJECT_LIST) { obj ->
                                Text(
                                    text = obj,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (renderer.nextTapAddMeshObjName == obj) {
                                                Color.LightGray
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            renderer.nextTapAddMeshObjName = obj
                                            isShowObjSelectBottomSheet.value = false
                                        }
                                        .padding(10.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                val isShowObjDeleteBottomSheet = remember { mutableStateOf(false) }
                if (isShowObjDeleteBottomSheet.value) {
                    ModalBottomSheet(onDismissRequest = { isShowObjDeleteBottomSheet.value = false }) {
                        LazyColumn {
                            items(renderer.wrappedAnchors) { anchor ->
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(text = anchor.objName)
                                        Text(text = DateUtils.getRelativeTimeSpanString(anchor.addTimeMs).toString())
                                    }
                                    Button(onClick = {
                                        renderer.removeAnchors(anchor)
                                        isShowObjDeleteBottomSheet.value = false
                                    }) {
                                        Text(text = "削除")
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Button(onClick = { isShowObjSelectBottomSheet.value = true }) {
                        Text(text = "オブジェクト選択")
                    }
                    Button(onClick = { isShowObjDeleteBottomSheet.value = true }) {
                        Text(text = "オブジェクト削除")
                    }
                }
            }
        }

        // Sets up an example renderer using our HelloARRenderer.
        SampleRender(view.surfaceView, renderer, assets)

        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)
    }

    // Configure the session, using Lighting Estimation, and Depth mode.
    fun configureSession(session: Session) {
        session.configure(
            session.config.apply {
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                // Depth API is used if it is configured in Hello AR's settings.
                depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }

                // Instant Placement is used if it is configured in Hello AR's settings.
                instantPlacementMode =
                    if (instantPlacementSettings.isInstantPlacementEnabled) {
                        InstantPlacementMode.LOCAL_Y_UP
                    } else {
                        InstantPlacementMode.DISABLED
                    }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
    }
}
