package com.carmabs.domain.usecase

import com.carmabs.domain.model.User
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.usecase.EmaUseCase

class AddFriendUseCase(private val repository: FriendsRepository) :
    EmaUseCase<AddFriendUseCase.Input, User>() {

    override suspend fun useCaseFunction(input: Input): User =
        repository.addFriend(input.name.trim(), input.surname.trim())

    data class Input(val name: String, val surname: String)
}
