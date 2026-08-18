package ru.netology.nework.error

import java.io.IOException
import java.sql.SQLException

sealed class AppError(var code: String) : RuntimeException() {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is ErrorCode404 -> ErrorCode404()
            is ErrorCode403 -> ErrorCode403()
            is ErrorCode415 -> ErrorCode415()
            is AppError -> e
            is SQLException -> DbError()
            is IOException -> NetworkError()
            else -> UnknownError()
        }
    }
}

class ApiError(val status: Int, code: String): AppError(code)

class NetworkError : AppError("error_network") {
    private fun readResolve(): Any = NetworkError()
}

class DbError : AppError("error_db") {
    private fun readResolve(): Any = DbError()
}

class UnknownError: AppError("error_unknown") {
    private fun readResolve(): Any = UnknownError()
}

class ErrorCode400: AppError("error_code_400") {
    private fun readResolve(): Any = ErrorCode400()
}

class ErrorCode403: AppError("error_code_403") {
    private fun readResolve(): Any = ErrorCode403()
}

class ErrorCode404: AppError("error_code_404") {
    private fun readResolve(): Any = ErrorCode404()
}

class ErrorCode415: AppError("error_code_415") {
    private fun readResolve(): Any = ErrorCode415()
}