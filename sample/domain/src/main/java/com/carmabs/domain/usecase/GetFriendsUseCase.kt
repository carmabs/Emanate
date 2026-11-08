package com.carmabs.domain.usecase

import com.carmabs.domain.exception.FriendsException
import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.usecase.EmaUseCase

class GetFriendsUseCase(private val repository: FriendsRepository) : EmaUseCase<Unit, EmaResult<List<Friend>, FriendsException>>() {

    override suspend fun useCaseFunction(input: Unit): EmaResult<List<Friend>, FriendsException> = repository.getFriends()
}
