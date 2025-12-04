package ru.netology.nework.error

import java.io.IOException
import java.sql.SQLException

sealed class AppError(var code: String) : RuntimeException() {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is ErrorCode400And500 -> ErrorCode400And500
            is AppError -> e
            is SQLException -> DbError
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

class ApiError(val status: Int, code: String): AppError(code)

object NetworkError : AppError("error_network") {
    private fun readResolve(): Any = NetworkError
}

object DbError : AppError("error_db") {
    private fun readResolve(): Any = DbError
}

object UnknownError: AppError("error_unknown") {
    private fun readResolve(): Any = UnknownError
}

object ErrorCode400And500: AppError("error_code_400_and_500") {
    private fun readResolve(): Any = ErrorCode400And500

}