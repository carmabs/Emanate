package com.carmabs.domain.usecase

import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.usecase.EmaUseCase

class GetFriendsUseCase(private val repository: FriendsRepository) : EmaUseCase<Unit, List<Friend>>() {

    override suspend fun useCaseFunction(input: Unit): List<Friend> = repository.getFriends()
}
