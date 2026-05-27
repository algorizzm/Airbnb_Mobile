package com.airbnb.core.ui

import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.toast(
    message: String?,
    duration: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(requireContext(), message ?: "", duration).show()
}

