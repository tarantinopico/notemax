package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatShortDate(timeMillis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}
