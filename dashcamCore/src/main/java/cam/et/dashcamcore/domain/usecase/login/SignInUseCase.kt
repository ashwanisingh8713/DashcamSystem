package cam.et.dashcamcore.domain.usecase.login

import cam.et.dashcamcore.domain.model.login.SignInResponse
import cam.et.dashcamcore.domain.repo.IUserRepository
import cam.et.dashcamcore.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher

class SignInUseCase(
    private val repository: IUserRepository,
    dispatcher: CoroutineDispatcher
): BaseUseCase<Any?, SignInResponse>(dispatcher) {

    override suspend fun block(param: Any?): SignInResponse {
        return repository.signIn(param)
    }
}