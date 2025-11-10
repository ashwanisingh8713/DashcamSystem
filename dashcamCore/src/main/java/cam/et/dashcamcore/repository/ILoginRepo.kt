package cam.et.dashcamcore.repository

import cam.et.dashcamcore.domain.model.login.ForgotPasswordResponse
import cam.et.dashcamcore.domain.model.login.SignInResponse
import cam.et.dashcamcore.domain.model.login.SignUpResponse


interface ILoginRepo {
    suspend fun signIn(param: Any?): SignInResponse
    suspend fun signUp(param: Any?): SignUpResponse
    suspend fun forgotPassword(param: Any?): ForgotPasswordResponse
}