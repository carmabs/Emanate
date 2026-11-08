package com.carmabs.domain.usecase

import com.carmabs.domain.exception.FriendsException
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.usecase.EmaUseCase

class DeleteFriendUseCase(private val repository: FriendsRepository) :
    EmaUseCase<DeleteFriendUseCase.Input, EmaResult<Unit, FriendsException>>() {

    override suspend fun useCaseFunction(input: Input): EmaResult<Unit, FriendsException> = repository.deleteFriend(input.id)

    data class Input(val id: String)
}
