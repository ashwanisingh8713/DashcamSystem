package cam.et.dashcamsystem.app.presentation.contract

import cam.et.dashcamcore.domain.model.login.SignInResponse
import cam.et.dashcamcore.domain.model.login.SignUpResponse
import cam.et.dashcamsystem.app.model.ResourceUiState
import cam.et.dashcamsystem.app.mvi.IUiEffect
import cam.et.dashcamsystem.app.mvi.IUiEvent
import cam.et.dashcamsystem.app.mvi.IUiState
import cam.et.dashcamsystem.data.repo.LoginRequestBody

interface LoginContract {
    sealed interface Event : IUiEvent {
        data class OnTryCheckAgainClick(val loginRequestBody: LoginRequestBody) : Event
        data object OnSignUpClick : Event
        data class OnForgotPasswordClick(val email: String) : Event
        data object OnBackToSignInClick : Event
        data class OnGoToHomeScreenClick(val signInResponse: SignInResponse) : Event
        data class OnLoginClick(val loginRequestBody: LoginRequestBody) : Event
    }

    data class SignInState(
        val loginResponse: ResourceUiState<SignInResponse>
    ) : IUiState

    data class SignUpState(
        val loginResponse: ResourceUiState<SignUpResponse>
    ) : IUiState


    sealed interface Effect : IUiEffect {
        data class NavigateToForgotPasswordScreen(val email: String) : Effect
        data class NavigateToHomeScreen(val signInResponse: SignInResponse) : Effect
        data object NavigateToSignUpScreen : Effect
        data object NavigateToSignInScreen : Effect
        data object IDLE : Effect
    }
}