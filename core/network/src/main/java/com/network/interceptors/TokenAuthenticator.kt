package com.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnauthorizedInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            // TODO: Trigger global logout (e.g., via a SharedFlow or Navigation event)
            // This will be finalized once we have the Auth flow logic
        }

        return response
    }
}