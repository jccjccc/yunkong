package com.dashabi.control.data

/**
 * 统一的请求结果。Err.http / code 便于界面区分 401（Key 失效）、403（缺少 Scope）、
 * 404（资源不存在）、429（限流）等场景。
 */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(
        val message: String,
        val code: String? = null,
        val http: Int? = null,
        val retryAfterSec: Int? = null,
        val requestId: String? = null
    ) : ApiResult<Nothing>
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Ok)?.value

fun ApiResult<*>.isOk(): Boolean = this is ApiResult.Ok

fun ApiResult<*>.errorMessage(): String? = (this as? ApiResult.Err)?.message
