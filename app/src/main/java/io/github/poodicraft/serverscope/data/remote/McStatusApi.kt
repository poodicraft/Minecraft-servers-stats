package io.github.poodicraft.serverscope.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** https://mcstatus.io/docs: free, no API key, results cached for about a minute. */
interface McStatusApi {

    /** The raw body is parsed by [io.github.poodicraft.serverscope.data.StatusParser]. */
    @GET("v2/status/{edition}/{address}")
    suspend fun status(
        @Path("edition") edition: String,
        @Path("address") address: String,
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://api.mcstatus.io/"
    }
}
