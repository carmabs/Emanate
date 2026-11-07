package com.carmabs.domain.model

import com.carmabs.emanate.core.constants.STRING_EMPTY
import kotlinx.serialization.Serializable

/**
 *  *<p>
 * Copyright (c) 2020, Carmabs. All rights reserved.
 * </p>
 *
 *
 * @author <a href=“mailto:apps.carmabs@gmail.com”>Carlos Mateo</a>
 */

@Serializable
data class User(
    val name: String = STRING_EMPTY,
    val surname: String = STRING_EMPTY,
    val role: Role = Role.BASIC,
    /**
     * Identifies the users stored in the list of friends. The users that log in don't have it.
     */
    val id: String = STRING_EMPTY
)
