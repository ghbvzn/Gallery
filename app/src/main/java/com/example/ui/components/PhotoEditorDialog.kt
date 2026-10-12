package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.MediaItem
import com.example.data.PhotoCrop
import com.example.data.PhotoEdits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoEditorDialog(item: MediaItem, onDismiss: () -> Unit, onSaved: (Uri, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember(item.uriString) { mutableStateOf<Bitmap?>(null) }
    var crop by remember { mutableStateOf(PhotoCrop()) }
    var turns by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item.uriString) {
        try { source = withContext(Dispatchers.IO) { PhotoEdits.load(context, Uri.parse(item.uriString), 1024) } }
        catch (e: Exception) { error = e.message ?: "Cannot open photo" }
    }
    val preview = remember(source, crop, turns) { source?.let { PhotoEdits.transform(it, crop, turns) } }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Crop / rotate") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                preview?.let { Image(it.asImageBitmap(), "Edited photo preview",
                    Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Fit) }
                if (source == null && error == null) CircularProgressIndicator()
                Text("Save a separate JPEG copy (up to 4096 px). The original is kept. Location and camera metadata are not copied.")
                Row {
                    TextButton(enabled = !saving, onClick = { turns = (turns + 1) % 4 }) { Text("Rotate 90°") }
                    TextButton(enabled = !saving, onClick = { crop = PhotoCrop(); turns = 0 }) { Text("Reset") }
                }
                Text("Crop width")
                Slider(crop.width, { crop = crop.copy(width = it) }, enabled = !saving, valueRange = 0.1f..1f)
                Text("Crop height")
                Slider(crop.height, { crop = crop.copy(height = it) }, enabled = !saving, valueRange = 0.1f..1f)
                Text("Horizontal position")
                Slider(crop.horizontal, { crop = crop.copy(horizontal = it) }, enabled = !saving && crop.width < 1f)
                Text("Vertical position")
                Slider(crop.vertical, { crop = crop.copy(vertical = it) }, enabled = !saving && crop.height < 1f)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = source != null && !saving, onClick = {
                saving = true
                error = null
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            PhotoEdits.saveCopy(context, Uri.parse(item.uriString), crop, turns)
                        }
                        onSaved(result.first, result.second)
                    } catch (e: Exception) { error = e.message ?: "Could not save copy" }
                    finally { saving = false }
                }
            }) { Text(if (saving) "Saving…" else "Save copy") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}
