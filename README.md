# Simple Image Loader [![](https://jitpack.io/v/solkin/simple-image-loader.svg)](https://jitpack.io/#solkin/simple-image-loader)

Modern image loading library for Android. Simple by design, powerful under the hood.

- **Kotlin**: Simple Image Loader is Kotlin-native and uses no any dependencies except myself [Disk LRU Cache](https://github.com/solkin/disk-lru-cache)
- **Fast**: contains lots of optimizations: memory cache, disk cache, images downsampling, requests cancelling and more
- **Lightweight**: ~18Kb 😄
- **Simple**: minimal boilerplate, simple API, based on every-day needs, extensible for features you need
- **Flexible**: not found something you need, like need FTP transport, maybe custom memory or disk caching, SVG displaying support, etc? You can easily add it on your project, because Simple Image Loader is modular and full of simple abstractions.

![GifDemo](/art/simple-image-loader-demo.gif)

## Gradle

Step 1. Add this in your root `build.gradle`

```
    allprojects {
        repositories {
            maven { url 'https://jitpack.io' }
        }
    }
```

Step 2. Add the dependency

```
    dependencies {
        compile 'com.github.solkin:simple-image-loader:VERSION'
    }
```

If you like to stay on the bleeding edge, or use certain commit as your dependency, you can use the short commit hash or anyBranch-SNAPSHOT as the version.

## Demo

Please see the demo app for library usage example.

## Quick Start

To load an image into an `ImageView`, use the `load` extension function:

```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// File
imageView.load("file:///path/to/image.jpg")

// Asset
imageView.load("file:///android_asset/image.jpg")

// Content
imageView.load("content://media/external_primary/images/media/90")
```

Requests can be configured with an optional trailing lambda:

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    centerCrop()
    withPlaceholder(R.drawable.ic_placeholder)
    whenError(R.drawable.ic_error, redColor)
}
```

Also, you can add options by creating extension functions:

```kotlin
fun Handlers<ImageView>.centerInside() = apply {
    successHandler { viewHolder, result ->
        with(viewHolder.get()) {
            setImageDrawable(null)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            colorFilter = null
            setImageDrawable(result.getDrawable())
        }
    }
}
```

#### Image Loader

`imageView.load` uses the singleton `SimpleImageLoader`. The singleton `SimpleImageLoader` can be accessed using an extension function:

```kotlin
val imageLoader = context.imageLoader()
```

To configure custom singleton `SimpleImageLoader`, run `context.initImageLoader` prior to another calls and change any module you need:

```kotlin
val imageLoader = context.initImageLoader(
    decoder = BitmapDecoder(),                              // Maybe, you need extraordinary images decoder?
    fileProvider = FileProviderImpl(
        cacheDir,
        DiskCacheImpl(                                      // LRU disk cache for your images
            DiskLruCache.create(cacheDir, 15728640L)
        ),
        UrlLoader(),                                        // vararg; http/https scheme support
        FileLoader(assets),                                 // vararg; file scheme support
        ContentLoader(contentResolver)                      // vararg; content scheme support
    ),
    memoryCache = MemoryCacheImpl(),                        // Caching images on memory
    mainExecutor = MainExecutorImpl(),                      // Simple executor for main thread
    backgroundExecutor = Executors.newFixedThreadPool(10)   // Executor service for background operations
)
```

## Requirements

- Min SDK 14+
- [Java 8+](https://coil-kt.github.io/coil/getting_started/#java-8)


## License
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