@file:OptIn(InternalSerializationApi::class)

package com.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: Int? = null,
    val status: String? = null,
    val message: String? = null,
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    val code: String? = null
)