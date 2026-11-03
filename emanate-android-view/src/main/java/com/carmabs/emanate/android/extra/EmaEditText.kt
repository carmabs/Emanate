@file:Suppress("DEPRECATION")

package com.carmabs.emanate.android.extra

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import com.carmabs.emanate.core.utils.EmaUtils

/**
 * Created by Carlos Mateo Benito on 2019-11-17.
 *
 * <p>
 * Copyright (c) 2019 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
@Deprecated(
    "Emanate will work only with Compose and EmaViewModel. Build the screens with emanate-compose-android and EmaViewModel instead of Views."
)
class EmaEditText : EditText {

    private var emaTextWatcher: EmaTextWatcher? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setText(text: String?) {
        EmaUtils.Values.checkUpdate(this.text.toString(), text) {
            super.setText(text)
        }
    }

    fun setEmaTextWatcherListener(listener: (String?) -> Unit) {
        emaTextWatcher?.removeListener()
        emaTextWatcher = EmaTextWatcher(this, listener)
    }
}
