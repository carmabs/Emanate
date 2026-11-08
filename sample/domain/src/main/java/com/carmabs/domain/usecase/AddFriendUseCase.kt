package com.carmabs.domain.usecase

import com.carmabs.domain.exception.FriendsException
import com.carmabs.domain.model.Friend
import com.carmabs.domain.repository.FriendsRepository
import com.carmabs.emanate.core.model.EmaResult
import com.carmabs.emanate.core.usecase.EmaUseCase

class AddFriendUseCase(private val repository: FriendsRepository) :
    EmaUseCase<AddFriendUseCase.Input, EmaResult<Friend, FriendsException>>() {

    override suspend fun useCaseFunction(input: Input): EmaResult<Friend, FriendsException> =
        repository.addFriend(input.name.trim(), input.surname.trim())

    data class Input(val name: String, val surname: String)
}
