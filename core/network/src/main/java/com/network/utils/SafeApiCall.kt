package com.network.utils

import com.network.model.ApiError
import com.utils.network.Resource
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
private val json = Json {
    ignoreUnknownKeys = true // 🔥 important (prevents crash if fields change)
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(apiCall.invoke())
    } catch (throwable: Throwable) {

        when (throwable) {

            is IOException -> {
                Resource.Error("Connection Error: Please check your internet")
            }

            is HttpException -> {
                val errorBody = throwable.response()?.errorBody()?.string()

                val message = try {
                    val apiError = errorBody?.let {
                        json.decodeFromString<ApiError>(it)
                    }
                    apiError?.message ?: "Server Error: Code ${throwable.code()}"
                } catch (e: Exception) {
                    "Server Error: Code ${throwable.code()}"
                }

                Resource.Error(message)
            }

            else -> {
                Resource.Error("Unknown Error: ${throwable.message}")
            }
        }
    }
}