package com.anxietywatch.mobile.ui.screens

private val NAME_REGEX = Regex("^[\\p{L} .'-]{0,60}$")
private val PHONE_REGEX = Regex("^[0-9+()\\- ]{0,20}$")
private val NUMERIC_REGEX = Regex("^[0-9]{0,3}$")

fun filterName(input: String): String = if (NAME_REGEX.matches(input)) input else input.dropLast(1)
fun filterPhone(input: String): String = if (PHONE_REGEX.matches(input)) input else input.dropLast(1)
fun filterNumeric(input: String): String = if (NUMERIC_REGEX.matches(input)) input else input.dropLast(1)