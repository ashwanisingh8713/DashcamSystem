package cam.et.dashcamsystem.app.viewmodel

import cam.et.dashcamsystem.app.model.ResourceUiState
import cam.et.dashcamsystem.app.mvi.MviBaseViewModel
import cam.et.dashcamsystem.app.presentation.contract.LoginContract
import kotlinx.coroutines.CoroutineScope

class SignUpViewModel(coroutineScope: CoroutineScope): MviBaseViewModel<LoginContract.Event, LoginContract.SignUpState, LoginContract.Effect>(coroutineScope) {

    override fun createInitialState(): LoginContract.SignUpState =
        LoginContract.SignUpState(loginResponse = ResourceUiState.Idle)

    override fun handleEvent(event: LoginContract.Event) {
        // TODO, yet to implement
    }

}