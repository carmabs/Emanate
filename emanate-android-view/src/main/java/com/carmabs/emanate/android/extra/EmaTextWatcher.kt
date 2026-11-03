@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extra

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.carmabs.emanate.core.constants.STRING_EMPTY

/**
 *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 *
 * Date: 2019-11-08
 */

@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class EmaTextWatcher(private val editText: EditText, private val action: (String?) -> Unit) {

    private var previousText = STRING_EMPTY
    private val textWatcher: TextWatcher by lazy {
        object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: STRING_EMPTY
                if (newText != previousText) {
                    removeListener()
                    action(newText)
                    restoreListener()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s?.toString() ?: STRING_EMPTY
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        }
    }

    init {
        editText.addTextChangedListener(textWatcher)
    }

    fun removeListener(): TextWatcher {
        editText.removeTextChangedListener(textWatcher)
        return textWatcher
    }

    fun restoreListener() {
        editText.addTextChangedListener(textWatcher)
    }
}
