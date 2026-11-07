package com.carmabs.domain.usecase

import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.usecase.EmaUseCase

class DeleteFriendUseCase(private val repository: FriendsRepository) :
    EmaUseCase<DeleteFriendUseCase.Input, Unit>() {

    override suspend fun useCaseFunction(input: Input) = repository.deleteFriend(input.id)

    data class Input(val id: String)
}
