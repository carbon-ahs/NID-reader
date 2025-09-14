//package com.example.nidreader
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.util.Log
//import android.view.ViewGroup
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.ImageCaptureException
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color as ComposeColor
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.compose.LocalLifecycleOwner
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.isGranted
//import com.google.accompanist.permissions.rememberPermissionState
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.suspendCancellableCoroutine
//import java.io.File
//import kotlin.coroutines.resumeWithException
//import android.graphics.Color
//
//
////@OptIn(ExperimentalPermissionsApi::class)
////@Composable
////fun CameraScreen(onBack: () -> Unit = {}) {
////    val context = LocalContext.current
////    val lifecycleOwner = LocalLifecycleOwner.current
////
////    // Camera permission state
////    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
////
////    LaunchedEffect(Unit) {
////        if (!cameraPermissionState.status.isGranted) {
////            cameraPermissionState.launchPermissionRequest()
////        }
////    }
////
////    if (cameraPermissionState.status.isGranted) {
////        AndroidView(
////            factory = { ctx ->
////                PreviewView(ctx).apply {
////                    layoutParams = ViewGroup.LayoutParams(
////                        ViewGroup.LayoutParams.MATCH_PARENT,
////                        ViewGroup.LayoutParams.MATCH_PARENT
////                    )
////                    scaleType = PreviewView.ScaleType.FILL_CENTER
////                }
////            },
////            modifier = Modifier.fillMaxSize(),
////            update = { previewView ->
////                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
////                cameraProviderFuture.addListener({
////                    val cameraProvider = cameraProviderFuture.get()
////
////                    val preview = Preview.Builder().build().also {
////                        it.setSurfaceProvider(previewView.surfaceProvider)
////                    }
////
////                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
////
////                    try {
////                        cameraProvider.unbindAll()
////                        cameraProvider.bindToLifecycle(
////                            lifecycleOwner,
////                            cameraSelector,
////                            preview
////                        )
////                    } catch (exc: Exception) {
////                        exc.printStackTrace()
////                    }
////                }, ContextCompat.getMainExecutor(context))
////            }
////        )
////    } else {
////        Box(
////            modifier = Modifier.fillMaxSize(),
////            contentAlignment = Alignment.Center
////        ) {
////            Text("Camera permission required")
////        }
////    }
////}
//
//@Composable
//fun CameraScreenWithOverlay(onBack: () -> Unit = {}) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val imageCapture = remember { ImageCapture.Builder().build() }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//
//        // 1️⃣ Camera Preview (back layer)
//        AndroidView(
//            factory = { ctx ->
//                PreviewView(ctx).apply {
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                    scaleType = PreviewView.ScaleType.FILL_CENTER
//                }
//            },
//            modifier = Modifier.fillMaxSize(),
//            update = { previewView ->
//                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//                cameraProviderFuture.addListener({
//                    val cameraProvider = cameraProviderFuture.get()
//
//                    val preview = Preview.Builder().build().also {
//                        it.setSurfaceProvider(previewView.surfaceProvider)
//                    }
//
//                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//                    try {
//                        cameraProvider.unbindAll()
//                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
//                    } catch (exc: Exception) {
//                        exc.printStackTrace()
//                    }
//                }, ContextCompat.getMainExecutor(context))
//            }
//        )
//
//        // 2️⃣ Overlay rectangle (middle layer)
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val boxWidth = size.width * 0.8f
//            val boxHeight = size.height * 0.25f
//            val left = (size.width - boxWidth) / 2
//            val top = (size.height - boxHeight) / 2
//            val rect = Rect(left, top, left + boxWidth, top + boxHeight)
//
//            drawRect(
//                color = ComposeColor.Green,
//                topLeft = Offset(rect.left, rect.top),
//                size = Size(rect.width, rect.height),
//                style = Stroke(width = 5f)
//            )
//        }
//
//        // 3️⃣ Capture Button (front layer, bottom center)
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(bottom = 48.dp),
//            contentAlignment = Alignment.BottomCenter
//        ) {
//            Button(
//                onClick = {
//                    takePhoto(context, imageCapture) { bitmap ->
//                        val cropped = cropToOverlay(bitmap)
//
//                        // OCR on cropped image
//                        val scope = CoroutineScope(Dispatchers.Main)
//                        scope.launch {
//                            try {
//                                val text = runTextRecognition(cropped)
//                                val fields = extractNidFields(text)
////                                val data = extractNidData(cropped)
//                                val data = extractNidDataWithDetection(cropped)
//                                Log.d("NID", "Name: ${fields.name}, DOB: ${fields.dob}, NID: ${fields.nid}")
//                                Log.d("TAG", "CameraScreenWithOverlay: ${data.values}")
//
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }
//                        }
//                    }
//                }
//            ) {
//                Text("Capture")
//            }
//
//        }
//    }
//}
//
//private fun takePhoto(
//    context: Context,
//    imageCapture: ImageCapture,
//    onPhotoCaptured: (Bitmap) -> Unit
//) {
//    val outputOptions = ImageCapture.OutputFileOptions.Builder(
//        File.createTempFile("captured_", ".jpg", context.cacheDir)
//    ).build()
//
//    imageCapture.takePicture(
//        outputOptions,
//        ContextCompat.getMainExecutor(context),
//        object : ImageCapture.OnImageSavedCallback {
//            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                val bitmap = BitmapFactory.decodeFile(outputFileResults.savedUri?.path
//                    ?: return)
//                onPhotoCaptured(bitmap)
//            }
//
//            override fun onError(exc: ImageCaptureException) {
//                exc.printStackTrace()
//            }
//        }
//    )
//}
//
//fun cropToOverlay(bitmap: Bitmap, boxWidthRatio: Float = 0.8f, boxHeightRatio: Float = 0.25f): Bitmap {
//    val width = bitmap.width
//    val height = bitmap.height
//
//    val cropWidth = (width * boxWidthRatio).toInt()
//    val cropHeight = (height * boxHeightRatio).toInt()
//
//    val left = (width - cropWidth) / 2
//    val top = (height - cropHeight) / 2
//
//    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
//}
//
//suspend fun runTextRecognition(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
//    val image = InputImage.fromBitmap(bitmap, 0)
//    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//    recognizer.process(image)
//        .addOnSuccessListener { visionText ->
//            cont.resume(visionText.text) {}
//        }
//        .addOnFailureListener { e ->
//            cont.resumeWithException(e)
//        }
//}
//
////fun preprocessForDigits(bitmap: Bitmap): Bitmap {
////    val width = bitmap.width
////    val height = bitmap.height
////    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
////
////    for (x in 0 until width) {
////        for (y in 0 until height) {
////            val pixel = bitmap.getPixel(x, y)
////            val r = Color.red(pixel)
////            val g = Color.green(pixel)
////            val b = Color.blue(pixel)
////            val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
////            val bw = if (gray > 150) 255 else 0
////            result.setPixel(x, y, Color.rgb(bw, bw, bw))
////        }
////    }
////    return result
////}
//
//
//data class NidData(
//    val name: String?,
//    val dob: String?,
//    val nid: String?
//)
//
//fun extractNidFields(text: String): NidData {
////    val nameRegex = Regex("Name[:\\s]+([A-Za-z\\s]+)")
//    val nameRegex = Regex("Name[:\\s]+([A-Za-z\\s\\.]+)")
//    val dobRegex = Regex("(\\d{2}[-/]\\d{2}[-/]\\d{4}|\\d{2}\\s\\w+\\s\\d{4})")
////    val nidRegex = Regex("(\\d{10,17})")
//    val nidRegex = Regex("\\b(\\d{10,17})\\b")
//
//
//    val name = nameRegex.find(text)?.groupValues?.get(1)?.trim()
//    val dob = dobRegex.find(text)?.groupValues?.get(1)?.trim()
//    val nid = nidRegex.find(text)?.groupValues?.get(1)?.trim()
//
//    return NidData(name, dob, nid)
//}
//
//
//
//suspend fun extractNidData(bitmap: Bitmap): Map<String, String> {
//    val results = mutableMapOf<String, String>()
//
//    // ---------- Step 1: Full OCR for Name + DOB ----------
//    val fullText = runTextRecognition(bitmap)
//
////    // Extract Name (assume first 2 lines contain it)
////    val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
////    if (lines.isNotEmpty()) results["name"] = lines[0]
////    if (lines.size > 1) results["father_name"] = lines[1] // optional
//
//    val nameRegex = Regex("Name[:\\s]+([A-Za-z\\s\\.]+)")
//    val name = nameRegex.find(fullText)?.groupValues?.get(1)?.trim()
//    if (name != null) results["name"] = name
//
//
//    // Extract DOB (both formats: 01 Jan 1990 OR 01/01/1990)
//    val dobRegex = Regex("\\b(\\d{2}[-/ ]\\d{2}[-/ ]\\d{4}|\\d{2} [A-Za-z]{3} \\d{4})\\b")
//    val dob = dobRegex.find(fullText)?.value
//    if (dob != null) results["dob"] = dob
//
//    // Extract NID if possible (non-smart card usually includes "ID NO")
//    val nidRegexFull = Regex("\\b(\\d{10,17})\\b")
//    val nidFull = nidRegexFull.find(fullText)?.value
//    if (nidFull != null) results["nid"] = nidFull
//
//    // ---------- Step 2: If NID Missing → Crop Smart NID Number Area ----------
//    if (results["nid"].isNullOrEmpty()) {
//        val numberRegion = cropSmartNidNumber(bitmap)
//        val processed = preprocessForDigits(numberRegion)
//        val croppedText = runTextRecognition(processed)
//
//        val nidCrop = nidRegexFull.find(croppedText)?.value
//        if (nidCrop != null) results["nid"] = nidCrop
//    }
//
//    return results
//}
//
//fun cropSmartNidNumber(bitmap: Bitmap): Bitmap {
//    val width = bitmap.width
//    val height = bitmap.height
//
//    // Bottom-right strip
//    val cropWidth = (width * 0.6f).toInt()
//    val cropHeight = (height * 0.15f).toInt()
//
//    val left = width - cropWidth
//    val top = height - cropHeight
//
//    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
//}
//
//fun preprocessForDigits(bitmap: Bitmap): Bitmap {
//    val width = bitmap.width
//    val height = bitmap.height
//    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//
//    for (x in 0 until width) {
//        for (y in 0 until height) {
//            val pixel = bitmap.getPixel(x, y)
//            val r = Color.red(pixel)
//            val g = Color.green(pixel)
//            val b = Color.blue(pixel)
//            val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
//            val bw = if (gray > 150) 255 else 0
//            result.setPixel(x, y, Color.rgb(bw, bw, bw))
//        }
//    }
//    return result
//}
//
//suspend fun extractNidDataWithDetectionOld(bitmap: Bitmap): Map<String, String> {
//    val results = mutableMapOf<String, String>()
//
//    // ---------- Step 1: Full OCR ----------
//    val fullText = runTextRecognition(bitmap)
//
////    val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
////    if (lines.isNotEmpty()) results["name"] = lines[0]
////    if (lines.size > 1) results["father_name"] = lines[1] // optional
//
//    val nameRegex = Regex("Name[:\\s]+([A-Za-z\\s\\.]+)")
//    val name = nameRegex.find(fullText)?.groupValues?.get(1)?.trim()
//    if (name != null) results["name"] = name
//
//    // DOB detection (both formats supported)
//    val dobRegex = Regex("\\b(\\d{2}[-/ ]\\d{2}[-/ ]\\d{4}|\\d{2} [A-Za-z]{3} \\d{4})\\b")
//    val dob = dobRegex.find(fullText)?.value
//    if (dob != null) results["dob"] = dob
//
//    // NID detection (10–17 digits)
//    val nidRegex = Regex("\\b(\\d{10,17})\\b")
//    val nidFromFull = nidRegex.find(fullText)?.value
//
//    // ---------- Step 2: Decide Card Type ----------
//    val isSmartCard = when {
//        nidFromFull != null -> false // old NID usually exposes number in text
//        fullText.contains("Smart", ignoreCase = true) -> true
//        else -> true // fallback assume smart
//    }
//
//    results["card_type"] = if (isSmartCard) "Smart NID" else "Old NID"
//
//    // ---------- Step 3: Handle NID Number ----------
//    if (!isSmartCard) {
//        // Old card → use detected full text number
//        if (nidFromFull != null) results["nid"] = nidFromFull
//    } else {
//        // Smart card → crop bottom-right area and OCR again
//        val numberRegion = cropSmartNidNumber(bitmap)
//        val processed = preprocessForDigits(numberRegion)
//        val croppedText = runTextRecognition(processed)
//
//        val nidFromCrop = nidRegex.find(croppedText)?.value
//        if (nidFromCrop != null) results["nid"] = nidFromCrop
//    }
//
//    return results
//}
//
//suspend fun extractNidDataWithDetection(bitmap: Bitmap): Map<String, String> {
//    val results = mutableMapOf<String, String>()
//
//    val fullText = runTextRecognition(bitmap)
//    val lines = fullText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
//
//    // Extract name
//    val name = extractName(lines)
//    if (name != null) results["name"] = name
//
//    // Extract dob
//    val dob = extractDob(fullText)
//    if (dob != null) results["dob"] = dob
//
//    // Try to get NID number from full OCR (old cards)
//    val nidRegex = Regex("\\b\\d{10,17}\\b")
//    val nidFromFull = nidRegex.find(fullText)?.value
//
//    if (nidFromFull != null) {
//        results["nid"] = nidFromFull
//        results["card_type"] = "Old NID"
//    } else {
//        // fallback → smart NID
//        val smartNid = extractSmartNidNumber(bitmap)
//        if (smartNid != null) results["nid"] = smartNid
//        results["card_type"] = "Smart NID"
//    }
//
//    return results
//}
//
//
//
////fun cropSmartNidNumber(bitmap: Bitmap): Bitmap {
////    val width = bitmap.width
////    val height = bitmap.height
////
////    // bottom-right strip (adjust ratios as needed)
////    val cropWidth = (width * 0.6f).toInt()
////    val cropHeight = (height * 0.15f).toInt()
////
////    val left = width - cropWidth
////    val top = height - cropHeight
////
////    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
////}
//
////fun extractName(lines: List<String>): String? {
////    return lines.firstOrNull {
////        it.isNotBlank() &&
////                !it.contains("Date of Birth", ignoreCase = true) &&
////                !it.any { ch -> ch.isDigit() }
////    }
////}
//
//fun extractDob(text: String): String? {
//    val dobRegex = Regex("(\\d{2}[-/ ]\\d{2}[-/ ]\\d{4}|\\d{2} [A-Za-z]{3} \\d{4})")
//    return dobRegex.find(text)?.value
//}
//
//suspend fun extractSmartNidNumber(bitmap: Bitmap): String? {
//    val cropped = cropSmartNidNumber(bitmap)
//    val processed = preprocessForDigits(cropped)
//    val text = runTextRecognition(processed)
//
//    val nidRegex = Regex("\\b\\d{10,17}\\b")
//    return nidRegex.find(text)?.value
//}
//
//fun extractName(lines: List<String>): String? {
//    val ignoreKeywords = listOf(
//        "government", "people", "republic", "bangladesh",
//        "date of birth", "smart nid", "national", "id", "name"
//    )
//
//    return lines.firstOrNull { line ->
//        line.isNotBlank()
//                && !ignoreKeywords.any { kw -> line.contains(kw, ignoreCase = true) }
//                && !line.any { it.isDigit() } // name shouldn't have digits
//                && line.length > 3 // avoid too short junk
//    }
//}
//
//
//
//
//
//
//
