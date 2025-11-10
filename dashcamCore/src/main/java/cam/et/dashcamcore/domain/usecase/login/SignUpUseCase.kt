package cam.et.dashcamcore.domain.usecase.login

import cam.et.dashcamcore.domain.model.login.SignUpResponse
import cam.et.dashcamcore.domain.repo.IUserRepository
import cam.et.dashcamcore.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher

class SignUpUseCase(private val repository: IUserRepository, dispatcher: CoroutineDispatcher): BaseUseCase<Any?, SignUpResponse>(dispatcher) {
    override suspend fun block(param: Any?): SignUpResponse {
        return repository.signUp(param)
    }

}