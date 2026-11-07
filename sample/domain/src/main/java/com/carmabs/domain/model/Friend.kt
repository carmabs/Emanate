package com.carmabs.domain.model

import kotlinx.serialization.Serializable

/**
 * A friend created by the admin and stored on the device. Unlike the [User] that logs in, it has no role and it always
 * has the [id] the storage gave to it.
 */
@Serializable
data class Friend(
    val id: String,
    val name: String,
    val surname: String
)
