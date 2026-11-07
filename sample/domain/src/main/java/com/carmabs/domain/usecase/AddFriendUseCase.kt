package com.carmabs.domain.usecase

import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.usecase.EmaUseCase

class AddFriendUseCase(private val repository: FriendsRepository) :
    EmaUseCase<AddFriendUseCase.Input, Friend>() {

    override suspend fun useCaseFunction(input: Input): Friend =
        repository.addFriend(input.name.trim(), input.surname.trim())

    data class Input(val name: String, val surname: String)
}
