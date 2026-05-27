package com.airbnb.core.ui

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * Lightweight reusable text-input dialog.
 *
 * Usage:
 *   EditTextDialog.show(
 *       context   = requireContext(),
 *       title     = "Edit Bio",
 *       initial   = currentBio,
 *       hint      = "Tell us about yourself…",
 *       maxLength = 200,
 *       multiLine = true
 *   ) { newText -> viewModel.updateBio(newText) }
 */
object EditTextDialog {

    fun show(
        context: Context,
        title: String,
        initial: String = "",
        hint: String = "",
        maxLength: Int = 500,
        multiLine: Boolean = false,
        onSave: (String) -> Unit
    ) {
        val input = EditText(context).apply {
            setText(initial)
            this.hint = hint
            setSelection(initial.length)
            if (multiLine) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                maxLines = 6
                minLines = 3
            } else {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                maxLines = 1
                setSingleLine(true)
            }
            filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
        }

        // Character counter
        val counter = TextView(context).apply {
            text = "${initial.length}/$maxLength"
            textSize = 11f
            setTextColor(0x80FFFFFF.toInt())
            setPadding(0, 4, 4, 0)
            gravity = android.view.Gravity.END
        }

        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                counter.text = "${s?.length ?: 0}/$maxLength"
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
        })

        val pad = (20 * context.resources.displayMetrics.density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
            addView(counter)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) onSave(text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}