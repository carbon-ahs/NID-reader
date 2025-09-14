package com.example.nidreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resumeWithException
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp


@Composable
fun CameraScreenWithBox(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val results = remember { mutableStateMapOf<String, String>() } // Reactive state for results

    Box(modifier = Modifier.fillMaxSize()) {

        // 1️⃣ Camera Preview (back layer)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2️⃣ Overlay rectangle (middle layer)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = size.width * 0.8f
            val boxHeight = size.height * 0.25f
            val left = (size.width - boxWidth) / 2
            val top = (size.height - boxHeight) / 2
            val rect = Rect(left, top, left + boxWidth, top + boxHeight)

            drawRect(
                color = ComposeColor.Green,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = 5f)
            )
        }

        // 3️⃣ Results Display (front layer, overlay on camera)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (results.isNotEmpty()) {
                // Show extracted results in a scrollable column
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .background(
                            ComposeColor.Black.copy(alpha = 0.7f), // Semi-transparent background for readability
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    results.forEach { (key, value) ->
                        Text(
                            text = "$key: $value",
                            color = ComposeColor.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Placeholder before capture
                Text(
                    text = "Point camera at NID and tap Capture",
                    modifier = Modifier.align(Alignment.Center),
                    color = ComposeColor.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4️⃣ Capture Button (front layer, bottom center)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    takePhoto(context, imageCapture) { bitmap ->
                        val cropped = cropToOverlay(bitmap)

                        // OCR on cropped image
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            try {
                                val data = extractNidDataWithDetection(cropped)
                                // Update the reactive state with results
                                results.clear()
                                results.putAll(data)
                                Log.d("TAG", "CameraScreenWithOverlay: ${data.values}")

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            ) {
                Text("Capture")
            }

        }
    }
}


private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        File.createTempFile("captured_", ".jpg", context.cacheDir)
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(outputFileResults.savedUri?.path
                    ?: return)
                onPhotoCaptured(bitmap)
            }

            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }
        }
    )
}

fun cropToOverlay(bitmap: Bitmap, boxWidthRatio: Float = 0.8f, boxHeightRatio: Float = 0.25f): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val cropWidth = (width * boxWidthRatio).toInt()
    val cropHeight = (height * boxHeightRatio).toInt()

    val left = (width - cropWidth) / 2
    val top = (height - cropHeight) / 2

    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
}


suspend fun extractNidDataWithDetection(bitmap: Bitmap): Map<String, String> {
    val results = mutableMapOf<String, String>()
    val fullText = runTextRecognition(bitmap)
    Log.d("TAG", "extractNidDataWithDetection: $fullText")
    val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

// Extract name
    val ignoreKeywords = listOf(
        "government", "people", "republic", "bangladesh",
        "date of birth", "smart nid", "national", "id", "name"
    )
    val nameRegex = Regex("Name[:\\s]+([A-Za-z\\s.]+)")
    val name = nameRegex.find(fullText)?.groupValues?.get(1)?.trim()


//    if (name != null) results["name"] = name

    if (name != null && !ignoreKeywords.any { keyword ->
            name.contains(keyword, ignoreCase = true)
        }) {
        results["name"] = name
    }

// Extract dob
    val dob = extractDob(fullText)
    if (dob != null) results["dob"] = dob

// Extract NID number based on specified formats
    var nidFound: String? = null
    var isSmartNid: Boolean = false

// Check for ID NO: format (non-smart, 9-10 continuous digits)
    val idNoRegex = Regex("ID NO[:\\s]+(\\b\\d{9,10}\\b)")
    nidFound = idNoRegex.find(fullText)?.groupValues?.get(1)
    if (nidFound != null) {
        isSmartNid = false
    } else {
        // Check for NID No format (smart, 3-3-4 digits with spaces)
        val nidNoRegex = Regex("NID No[.:\\s]+(\\b\\d{3}\\s\\d{3}\\s\\d{4}\\b)")
        nidFound = nidNoRegex.find(fullText)?.groupValues?.get(1)
        if (nidFound != null) {
            isSmartNid = true
        } else {
            // Fallback: Check for NID number on the next line after "NID No" or "ID NO"
            val nidIndex = lines.indexOfFirst { it.matches(Regex("NID No[.:\\s]*")) || it.matches(Regex("ID NO[:\\s]*")) }
            if (nidIndex != -1 && nidIndex < lines.size - 1) {
                val nextLine = lines[nidIndex + 1]
                val smartNidMatch = Regex("\\b\\d{3}\\s\\d{3}\\s\\d{4}\\b").find(nextLine)
                if (smartNidMatch != null) {
                    nidFound = smartNidMatch.value
                    isSmartNid = true
                } else {
                    val nonSmartNidMatch = Regex("\\b\\d{9,10}\\b").find(nextLine)
                    nidFound = nonSmartNidMatch?.value
                    if (nidFound != null) isSmartNid = false
                }
            }
        }
    }

    if (nidFound != null) {
        results["nid"] = nidFound
        results["card_type"] = if (isSmartNid) "Smart NID" else "Non-Smart NID"
    }

    return results
}

suspend fun runTextRecognition(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            cont.resume(visionText.text) {}
        }
        .addOnFailureListener { e ->
            cont.resumeWithException(e)
        }
}

fun extractName(lines: List<String>): String? {
    val ignoreKeywords = listOf(
        "government", "people", "republic", "bangladesh",
        "date of birth", "smart nid", "national", "id", "name"
    )

    return lines.firstOrNull { line ->
        line.isNotBlank()
                && !ignoreKeywords.any { kw -> line.contains(kw, ignoreCase = true) }
                && !line.any { it.isDigit() } // name shouldn't have digits
                && line.length > 3 // avoid too short junk
    }
}

fun extractDob(text: String): String? {
    val dobRegex = Regex("(\\d{2}[-/ ]\\d{2}[-/ ]\\d{4}|\\d{2} [A-Za-z]{3} \\d{4})")
    return dobRegex.find(text)?.value
}

suspend fun extractSmartNidNumber(bitmap: Bitmap): String? {
    val cropped = cropSmartNidNumber(bitmap)
    val processed = preprocessForDigits(cropped)
    val text = runTextRecognition(processed)
    android.util.Log.d("NIDDebug", "Recognized Text: $text")
    val nidRegex = Regex("\\b\\d{10,17}\\b")
    return nidRegex.find(text)?.value
}

fun cropSmartNidNumber(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
// Adjusted cropping to target NID number near bottom-right (e.g., last 20% width, 12% height, shifted up slightly)
    val cropWidth = (width * 0.2f).toInt()
    val cropHeight = (height * 0.12f).toInt()

    val left = width - cropWidth
    val top = height - (height * 0.15f).toInt() // Shift up by 3% more from the bottom

    val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)

// Log cropped image details
    android.util.Log.d("CropDebug", "Cropped Bitmap - Width: ${croppedBitmap.width}, Height: ${croppedBitmap.height}, Left: $left, Top: $top")

    return croppedBitmap
}

fun preprocessForDigits(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
//            val bw = if (gray > 150) 255 else 0
            val bw = if (gray > 130) 255 else 0
            result.setPixel(x, y, Color.rgb(bw, bw, bw))
        }
    }
    return result
}