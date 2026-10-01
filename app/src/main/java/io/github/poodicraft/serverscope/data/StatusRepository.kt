package io.github.poodicraft.serverscope.data

import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.data.model.ServerStatus
import io.github.poodicraft.serverscope.data.remote.McStatusApi
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface StatusResult {
    /** [lookupMillis] is how long the API took to answer; the API itself reports no game ping. */
    data class Success(val status: ServerStatus, val lookupMillis: Long) : StatusResult
    data class Failure(val error: StatusError) : StatusResult
}

class StatusRepository(private val api: McStatusApi) {

    /** Never throws (except for coroutine cancellation): every failure becomes a [StatusError]. */
    suspend fun fetch(address: ServerAddress, edition: Edition): StatusResult = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        try {
            val response = api.status(edition.apiPath, address.query)
            val lookupMillis = (System.nanoTime() - started) / 1_000_000
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                return@withContext StatusResult.Failure(StatusError.fromHttpCode(response.code()))
            }
            val body = response.body()?.use { it.string() }
                ?: return@withContext StatusResult.Failure(StatusError.BadResponse)
            StatusResult.Success(StatusParser.parse(body, edition), lookupMillis)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StatusResult.Failure(StatusError.fromThrowable(e))
        }
    }
}
