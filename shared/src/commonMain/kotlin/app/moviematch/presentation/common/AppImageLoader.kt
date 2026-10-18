package app.moviematch.presentation.common

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory

/**
 * Installs the singleton Coil [ImageLoader] used by every `AsyncImage` of the app.
 *
 * The Ktor network fetcher is registered **explicitly** instead of relying on Coil's `ServiceLoader`
 * discovery: that mechanism only exists on the JVM/Android targets, so without this call a remote
 * image would silently never load on iOS (and silently fail on Android if the artifact's service
 * file is ever dropped by shrinking). The HTTP engine is not passed here on purpose — each platform
 * provides its own (OkHttp on Android, Darwin on iOS).
 */
@Composable
fun InstallAppImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}
