package com.zephyr.boreal.ui.components

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("LongMethod")
@Composable
fun BarcodeScanner(
  onBarcodeScanned: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

  var hasCameraPermission by remember { mutableStateOf(false) }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { granted ->
        hasCameraPermission = granted
      },
    )

  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
      android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
      hasCameraPermission = true
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  if (hasCameraPermission) {
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner =
      remember {
        BarcodeScanning.getClient(
          BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(),
        )
      }
    val hasScanned = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
      onDispose {
        executor.shutdown()
        barcodeScanner.close()
      }
    }

    Box(modifier = modifier) {
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx)
          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

          cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview =
              Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
              }

            val imageAnalyzer =
              ImageAnalysis
                .Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                  analysis.setAnalyzer(
                    executor,
                    @OptIn(ExperimentalGetImage::class)
                    object : ImageAnalysis.Analyzer {
                      override fun analyze(imageProxy: ImageProxy) {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                          val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                          barcodeScanner
                            .process(image)
                            .addOnSuccessListener { barcodes ->
                              barcodes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                                if (hasScanned.compareAndSet(false, true)) {
                                  currentOnBarcodeScanned(value)
                                }
                              }
                            }.addOnFailureListener { e ->
                              Log.e("BarcodeScanner", "Barcode scanning failed", e)
                            }.addOnCompleteListener {
                              imageProxy.close()
                            }
                        } else {
                          imageProxy.close()
                        }
                      }
                    },
                  )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
              )
            } catch (exc: Exception) {
              Log.e("BarcodeScanner", "Use case binding failed", exc)
            }
          }, ContextCompat.getMainExecutor(ctx))
          previewView
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  } else {
    Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      com.zephyr.boreal.ui.components.ErrorCard(
        message =
          androidx.compose.ui.res
            .stringResource(com.zephyr.boreal.R.string.scanner_permission_required),
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(16.dp))
      com.zephyr.boreal.ui.components.BorealButton(
        text =
          androidx.compose.ui.res
            .stringResource(com.zephyr.boreal.R.string.scanner_open_settings),
        variant = com.zephyr.boreal.ui.components.ButtonVariant.NEUTRAL,
        onClick = {
          val intent =
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = android.net.Uri.fromParts("package", context.packageName, null)
            }
          context.startActivity(intent)
        },
      )
    }
  }
}
