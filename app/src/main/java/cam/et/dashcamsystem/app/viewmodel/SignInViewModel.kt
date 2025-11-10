package cam.et.dashcamsystem.app.viewmodel

import cam.et.dashcamcore.domain.usecase.login.SignInUseCase
import cam.et.dashcamsystem.app.model.ResourceUiState
import cam.et.dashcamsystem.app.mvi.MviBaseViewModel
import cam.et.dashcamsystem.app.presentation.contract.LoginContract
import kotlinx.coroutines.CoroutineScope

open class SignInViewModel(private val signInUseCase: SignInUseCase, coroutineScope: CoroutineScope): MviBaseViewModel<LoginContract.Event, LoginContract.SignInState, LoginContract.Effect>(coroutineScope) {

    override fun createInitialState(): LoginContract.SignInState = LoginContract.SignInState(loginResponse = ResourceUiState.Idle)

    override fun handleEvent(event: LoginContract.Event) {
        // TODO, yet to implement
    }

}