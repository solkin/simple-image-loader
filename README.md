# Simple Image Loader

[![](https://jitpack.io/v/solkin/simple-image-loader.svg)](https://jitpack.io/#solkin/simple-image-loader)

Modern image loading library for Android. Simple by design, powerful under the hood.

![GifDemo](/art/simple-image-loader-demo.gif)

## Features

| Feature | Description |
|---------|-------------|
| **Kotlin-first** | Native Kotlin API with extension functions and DSL |
| **Lightweight** | Only ~18Kb, single dependency ([Disk LRU Cache](https://github.com/solkin/disk-lru-cache)) |
| **Fast** | Memory cache, disk cache, image downsampling, request cancellation |
| **Composable** | Chain multiple handlers — they all execute in order |
| **Extensible** | Add custom loaders, decoders, or cache implementations |

## Installation

### Step 1. Add JitPack repository

```groovy
// settings.gradle.kts (recommended)
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// or in root build.gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2. Add the dependency

```groovy
dependencies {
    implementation 'com.github.solkin:simple-image-loader:VERSION'
}
```

> 💡 Use a specific release tag, short commit hash, or `anyBranch-SNAPSHOT` as VERSION.

## Quick Start

Load an image into an `ImageView` with a single line:

```kotlin
imageView.fetch("https://example.com/image.jpg")
```

### Supported URI Schemes

| Scheme | Example |
|--------|---------|
| HTTP/HTTPS | `https://example.com/image.jpg` |
| File | `file:///sdcard/photo.jpg` |
| Asset | `file:///android_asset/image.jpg` |
| Content | `content://media/external/images/media/123` |

## Configuration DSL

Customize loading behavior with a type-safe DSL:

```kotlin
imageView.fetch("https://example.com/image.jpg") {
    centerCrop()
    crossfade()
    placeholder(R.drawable.loading)
    error(R.drawable.error)
}
```

### Scale Types

```kotlin
centerCrop()    // Scale and crop to fill the view
fitCenter()     // Scale to fit within the view bounds
centerInside()  // Scale down only if larger than view
```

### Animations

```kotlin
crossfade()         // Fade in with default 300ms duration
crossfade(500)      // Fade in with custom duration (ms)
```

### Placeholders & Errors

```kotlin
placeholder(R.drawable.loading)   // Show while loading
placeholder(drawable)             // Pass Drawable directly

error(R.drawable.error)           // Show on failure
error(drawable)                   // Pass Drawable directly
```

### Transformations

Apply image transformations:

```kotlin
imageView.fetch(url) {
    transform {
        circleCrop()           // Crop to circle
        rounded(16)            // Rounded corners (px)
        grayscale()            // Convert to grayscale
        blur(25)               // Apply blur effect
    }
}

// Or apply individually
imageView.fetch(url) {
    transform(CircleCropTransformation())
}
```

### Callbacks

```kotlin
imageView.fetch(url) {
    onLoading { imageView ->
        // Called when loading starts
    }
    onSuccess { imageView, drawable ->
        // Called on successful load
    }
    onError { imageView, throwable ->
        // Called on failure
    }
}
```

### Cache Control

```kotlin
imageView.fetch(url) {
    memoryCache(enabled = true)
    diskCache(enabled = false)
    
    // Or with policies
    memoryCache(CachePolicy.READ_ONLY)
    diskCache(CachePolicy.DISABLED)
}
```

### Size Override

```kotlin
imageView.fetch(url) {
    size(200, 200)              // Fixed size
    size(Size.ORIGINAL)         // Load at original size
}
```

### Reusable Configurations

Create configurations once, use everywhere:

```kotlin
// Define once
val avatarConfig = imageRequest<ImageView> {
    transform { circleCrop() }
    crossfade()
    placeholder(R.drawable.avatar_placeholder)
    error(R.drawable.avatar_error)
}

// Reuse
imageView1.fetch(url1, avatarConfig)
imageView2.fetch(url2, avatarConfig)
imageView3.fetch(url3, avatarConfig)
```

### Handler Composition

All handlers are **composable** — call multiple options and they all execute in sequence:

```kotlin
imageView.fetch(url) {
    centerCrop()    // 1. Sets scale type
    crossfade()     // 2. Animates alpha
    onSuccess { _, _ -> analytics.track("image_loaded") }  // 3. Track event
}
```

## Custom Transformations

Create your own image transformations:

```kotlin
class SepiaTransformation : Transformation {
    override val key = "sepia"
    
    override fun transform(bitmap: Bitmap): Bitmap {
        // Apply sepia effect
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(sepiaMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}

// Usage
imageView.fetch(url) {
    transform(SepiaTransformation())
}
```

### Built-in Transformations

| Transformation | Description |
|----------------|-------------|
| `circleCrop()` | Crop image to circle |
| `rounded(radiusPx)` | Round corners |
| `grayscale()` | Convert to grayscale |
| `blur(radius)` | Apply blur effect |

## Advanced Configuration

### Custom ImageLoader

Access or configure the singleton loader:

```kotlin
// Get the loader
val imageLoader = context.imageLoader()

// Initialize with custom configuration (call before first use)
context.initImageLoader(
    decoders = listOf(BitmapDecoder()),
    fileProvider = FileProviderImpl(
        cacheDir,
        DiskCacheImpl(DiskLruCache.create(cacheDir, 15_728_640L)),
        UrlLoader(),
        FileLoader(assets),
        ContentLoader(contentResolver)
    ),
    memoryCache = MemoryCacheImpl(),
    mainExecutor = MainExecutorImpl(),
    backgroundExecutor = Executors.newFixedThreadPool(10)
)
```

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer                              │
├────────────────────────────┬────────────────────────────────┤
│   ImageView.fetch()        │     Compose (coming soon)      │
│   (View binding, DSL)      │     (AsyncImage Composable)    │
└────────────────────────────┴────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    ImageRepository                          │
│         (Core loading & caching, UI-agnostic)               │
├─────────────────────────────────────────────────────────────┤
│  • load(url, width, height): Result?                        │
│  • loadAsync(url, width, height): Future<Result?>           │
│  • getCached(url, width, height): Result?                   │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                      │
├─────────────────┬─────────────┬─────────────────────────────┤
│   MemoryCache   │  DiskCache  │       FileProvider          │
│     (LRU)       │    (LRU)    │  ┌───────────────────────┐  │
├─────────────────┼─────────────┤  │      Loaders          │  │
│     Decoder     │             │  │  • UrlLoader (http)   │  │
│    (Bitmap)     │             │  │  • FileLoader (file)  │  │
│                 │             │  │  • ContentLoader      │  │
└─────────────────┴─────────────┴──┴───────────────────────┴──┘
```

### Direct Repository Access

For custom UI frameworks or advanced use cases, access the repository directly:

```kotlin
// Get the repository (UI-agnostic)
val repository = context.imageRepository()

// Load synchronously (call from background thread)
val result = repository.load(url, width, height)
val drawable = result?.getDrawable()

// Load asynchronously
val future = repository.loadAsync(url, width, height)
val result = future.get()

// Check cache only
val cached = repository.getCached(url, width, height)
```

### Adding Custom Loaders

Support new URI schemes by implementing `Loader`:

```kotlin
class FtpLoader : Loader {
    override val schemes = listOf("ftp", "sftp")
    
    override fun load(uriString: String, file: File): Boolean {
        // Download file via FTP
        return success
    }
}

// Register in FileProvider
context.initImageLoader(
    fileProvider = FileProviderImpl(
        cacheDir, diskCache,
        UrlLoader(), 
        FileLoader(assets),
        FtpLoader()  // Your custom loader
    )
)
```

### Adding Custom Decoders

Support new image formats by implementing `Decoder`:

```kotlin
class SvgDecoder : Decoder {
    override fun probe(file: File): Boolean {
        return file.name.endsWith(".svg")
    }
    
    override fun decode(file: File, width: Int, height: Int): Result? {
        // Decode SVG to Bitmap/Drawable
        return SvgResult(drawable)
    }
}
```

### Debug Logging

Logging is disabled by default for production. Enable it for debugging:

```kotlin
// Enable built-in Logcat output
SimpleImageLoaderLog.logger = Logger.LOGCAT

// Or use a custom logger (e.g., Timber)
SimpleImageLoaderLog.logger = object : Logger {
    override fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    override fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
    }
}

// Disable logging (default)
SimpleImageLoaderLog.logger = Logger.NONE
```

When enabled, logs include:
- Request lifecycle (`ImageLoader` tag)
- Cache hits/misses (`ImageRepository` tag)
- Disk operations (`FileProvider` tag)

## Jetpack Compose (Experimental)

Use `ImageRepository` directly with Compose:

```kotlin
@Composable
fun AsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var painter by remember { mutableStateOf<Painter?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }

        LaunchedEffect(url, widthPx, heightPx) {
            withContext(Dispatchers.IO) {
                val result = context.imageRepository().load(url, widthPx, heightPx)
                result?.getDrawable()?.let { drawable ->
                    painter = BitmapDrawable(
                        context.resources,
                        (drawable as BitmapDrawable).bitmap
                    ).toPainter()
                }
            }
        }

        painter?.let {
            Image(
                painter = it,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

> A dedicated `imageloader-compose` module is planned for future releases.

## RecyclerView Usage

Works seamlessly with RecyclerView — previous requests are automatically cancelled:

```kotlin
class MyAdapter : RecyclerView.Adapter<ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.fetch(items[position].imageUrl) {
            centerCrop()
            crossfade()
            withPlaceholder(R.drawable.placeholder)
            whenError(R.drawable.error)
        }
    }
}
```

## Requirements

- **Min SDK**: 16+
- **Java**: 8+

## R8 / ProGuard

No additional rules required.

## Migration from v0.9.x to v1.1.0

### Breaking Changes

v1.1.0 introduces a new type-safe DSL with `@DslMarker` annotation, transformations support, and improved API naming.

### DSL Method Renames

| v0.9.x | v1.1.0 |
|--------|--------|
| `withPlaceholder(R.drawable.ic)` | `placeholder(R.drawable.ic)` |
| `withPlaceholder(drawable)` | `placeholder(drawable)` |
| `whenError(R.drawable.ic)` | `error(R.drawable.ic)` |
| `whenError(R.drawable.ic, color)` | `error(R.drawable.ic)` + custom handler |

### Handler Changes

| v0.9.x | v1.1.0 |
|--------|--------|
| `successHandler { viewHolder, result -> }` | `onSuccess { imageView, drawable -> }` |
| `placeholderHandler { viewHolder -> }` | `onLoading { imageView -> }` |
| `errorHandler { viewHolder -> }` | `onError { imageView, throwable -> }` |

### Migration Example

**Before (v0.9.x):**
```kotlin
import com.tomclaw.imageloader.util.centerCrop
import com.tomclaw.imageloader.util.withPlaceholder
import com.tomclaw.imageloader.util.whenError
import com.tomclaw.imageloader.util.crossfade

imageView.fetch(url) {
    centerCrop()
    crossfade()
    withPlaceholder(R.drawable.placeholder)
    whenError(R.drawable.error, Color.RED)
}
```

**After (v1.1.0):**
```kotlin
import com.tomclaw.imageloader.util.fetch

imageView.fetch(url) {
    centerCrop()
    crossfade()
    placeholder(R.drawable.placeholder)
    error(R.drawable.error)
}
```

### New Features in v1.1.0

#### Transformations
```kotlin
imageView.fetch(url) {
    transform {
        circleCrop()
        rounded(16)
        grayscale()
    }
}
```

#### Reusable Configurations
```kotlin
val config = imageRequest<ImageView> {
    centerCrop()
    crossfade()
}
imageView.fetch(url, config)
```

#### Cache Control
```kotlin
imageView.fetch(url) {
    memoryCache(enabled = false)
    diskCache(CachePolicy.READ_ONLY)
}
```

#### Direct Repository Access
```kotlin
// For Compose or custom UI
val repository = context.imageRepository()
val result = repository.load(url, width, height)
```

### Removed

- `withPlaceholder(drawableRes, tintColor)` — use custom `onLoading` handler instead
- `whenError(drawableRes, tintColor)` — use custom `onError` handler instead
- `fetchLegacy()` — use `fetch()` with new DSL
- All legacy DSL functions (`centerCrop()`, `fitCenter()`, etc. as `Handlers` extensions)
- Direct assignment to `Handlers.success/placeholder/error` fields (now `private set`)

## License

```
MIT License

Copyright (c) 2021 Igor Solkin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
