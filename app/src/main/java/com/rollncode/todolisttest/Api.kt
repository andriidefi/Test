package com.rollncode.todolisttest

import com.google.gson.JsonSyntaxException
import io.reactivex.*
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 *
 */

const val BASE_URL = "https://hn.algolia.com/api/v1/"
const val GET_ITEMS = "search_by_date"
const val TAGS = "tags"
const val PAGE = "page"

object ApiClient {
    val instance: Api = ApiClientImpl()
}

private class ApiClientImpl(private val api: Api = ApiGenerator(BASE_URL, Api::class.java).api) : Api by api

interface Api {

    @GET(GET_ITEMS)
    fun getItems(@Query(PAGE) page: Int = 1,
                 @Query(TAGS) tags: String = "story"): Single<Response>
}


internal class ApiGenerator<out T>(baseUrl: String, api: Class<T>) {

    val api: T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(createClient())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(ApiHandler(Schedulers.io()))

        .build()
        .create(api)

    private fun createClient(): OkHttpClient {
        val client = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
        return client.build()
    }
}

internal class ApiHandler(scheduler: Scheduler) : CallAdapter.Factory() {

    private val original: RxJava2CallAdapterFactory
            = RxJava2CallAdapterFactory.createWithScheduler(scheduler)

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>?
            = original.get(returnType, annotations, retrofit)?.let { Wrapper(it) }

    private class Wrapper<R>(private val wrapped: CallAdapter<R, *>) : CallAdapter<R, Any> {
        override fun adapt(call: Call<R>?): Any? {
            call ?: return null
            val result = wrapped.adapt(call)

            return when (result) {
                is Maybe<*> -> result.onErrorResumeNext(Function { Maybe.error(wrap(it)) })
                is Single<*>     -> result.onErrorResumeNext { Single.error(wrap(it)) }
                is Completable -> result.onErrorResumeNext { Completable.error(wrap(it)) }
                is Observable<*> -> result.onErrorResumeNext(Function { Observable.error(wrap(it)) })

                else             -> result
            }
        }

        override fun responseType(): Type = wrapped.responseType()

        private fun wrap(throwable: Throwable) = when (throwable) {
            is HttpException -> {
                val exception = ApiException.http(throwable)
                exception
            } // We had non-200 http error
            is JsonSyntaxException -> ApiException.parse(throwable) // We had json parsing error
            is SocketTimeoutException -> ApiException.timeout(throwable) // A network error happened
            is IOException -> ApiException.network(throwable) // A network error happened

            else                      -> ApiException.unknown(throwable) // We don't know what happened. We need to simply convert to an unknown error
        }
    }
}
class ApiException internal constructor(message: String,
//                                        /** Response object containing status code, headers, body, etc.  */
//                                        val response: ErrorResponse?,
                                        /** The event kind which triggered this error.  */
                                        @ApiError val error: Int,
                                        exception: Throwable?) : RuntimeException(message, exception) {

    companion object {
        fun http(exception: HttpException): ApiException {
            val response = exception.response()
            val message = if (response == null) {
                if (exception.message().isEmpty()) exception.code().toString() else exception.message()

            } else {
                response.raw().message()
            }
            return ApiException(message, ApiError.HTTP, exception)
        }

        fun network(exception: IOException): ApiException {
            return ApiException(exception.message ?: "network", ApiError.NETWORK, exception)
        }

        fun parse(exception: JsonSyntaxException): ApiException {
            return ApiException(exception.message ?: "parse", ApiError.CONVERSION, exception)
        }

        fun unknown(exception: Throwable): ApiException {
            return ApiException(exception.message ?: "unknown", ApiError.UNKNOWN, exception)
        }

        fun timeout(exception: SocketTimeoutException): ApiException {
            return ApiException("Connection timed out", ApiError.TIMEOUT_EXCEPTION, exception)
        }
    }
}

annotation class ApiError {
    companion object {
        /**
         * An internal error occurred while attempting to execute a request. It is best practice to
         * re-throw this exception so your application crashes.
         */
        const val UNKNOWN = 0x0
        /**
         * An [IOException] occurred while communicating to the server.
         */
        const val NETWORK = 0xA
        /**
         * A non-200 HTTP status code was received from the server.
         */
        const val HTTP = 0xB
        /**
         * Json parsing error.
         */
        const val CONVERSION = 0xC
        /**
         * Timeout error.
         */
        const val TIMEOUT_EXCEPTION = 0xD
    }
}