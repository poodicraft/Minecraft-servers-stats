package io.github.poodicraft.serverscope

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.github.poodicraft.serverscope.data.StatusRepository
import io.github.poodicraft.serverscope.data.local.SavedServersStore
import io.github.poodicraft.serverscope.data.local.serverScopeDataStore
import io.github.poodicraft.serverscope.data.remote.McStatusApi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class ServerScopeApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.imageHttpClient })) }
            .crossfade(true)
            .build()
}

/** Manual dependency wiring; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {

    private val userAgent = "ServerScope/${BuildConfig.VERSION_NAME} (Android)"

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
        }
        .build()

    /** Shares the connection pool with [httpClient]; head/skin renders can be slower. */
    val imageHttpClient: OkHttpClient = httpClient.newBuilder()
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val statusRepository = StatusRepository(
        Retrofit.Builder()
            .baseUrl(McStatusApi.BASE_URL)
            .client(httpClient)
            .build()
            .create(McStatusApi::class.java),
    )

    val savedServers = SavedServersStore(context.applicationContext.serverScopeDataStore)
}
