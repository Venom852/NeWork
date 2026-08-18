package ru.netology.nework.util

object CountCalculator {
    fun calculator(number: Long): String {
        return when {
            number in 1000..9999 ->
                if (number in 1000..1099) number.toString()[0] + "K" else number.toString()[0] + "." + number.toString()[1] + "K"

            number in 10_000..99_999 -> number.toString().replaceRange(2, 4, "K")
            number in 100_000..999_999 -> number.toString().replaceRange(3, 5, "K")
            number in 1_000_000..9_999_999 ->
                if (number in 1_000_000..1_099_999) number.toString()[0] + "M" else number.toString()[0] + "." + number.toString()[1] + "M"

            else -> number.toString()
        }
    }
}