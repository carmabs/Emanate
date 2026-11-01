package com.carmabs.emanate.compose.extension

import androidx.compose.ui.tooling.preview.datasource.LoremIpsum

/**
 * Created by Carlos Mateo Benito on 23/9/23.
 *
 * <p>
 * Copyright (c) 2023 by Carmabs. All rights reserved.
 * </p>
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo Benito</a>
 */
fun LoremIpsum.generate(): String = this.values.reduce { acc, s ->
    "$acc $s"
}
