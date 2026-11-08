package com.carmabs.domain.exception

/**
 * The friends couldn't be read, stored or deleted
 */
data class FriendsException(val message: String = "The friends storage failed, please retry it")
