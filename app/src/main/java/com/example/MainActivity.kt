package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.ui.GalleryViewModel
import com.example.ui.screens.GalleryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val imageLoader = ImageLoader.Builder(this)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.35)
                .strongReferencesEnabled(true)
                .weakReferencesEnabled(true)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(250L * 1024 * 1024)
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .respectCacheHeaders(false)
        .allowHardware(true)
        .allowRgb565(true)
        .crossfade(false)
        .build()
    Coil.setImageLoader(imageLoader)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val galleryViewModel: GalleryViewModel = viewModel()
          GalleryScreen(viewModel = galleryViewModel)
        }
      }
    }
  }
}

