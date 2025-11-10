package cam.et.dashcamcore.domain.repo

import cam.et.dashcamcore.domain.model.login.ForgotPasswordResponse
import cam.et.dashcamcore.domain.model.login.SignInResponse
import cam.et.dashcamcore.domain.model.login.SignUpResponse
import cam.et.dashcamcore.domain.model.profile.ProfileResponse

interface IUserRepository {
    
    suspend fun signIn(param: Any?): SignInResponse
    suspend fun signUp(param: Any?): SignUpResponse
    suspend fun forgotPassword(param: Any?): ForgotPasswordResponse

    suspend fun getProfile(param: Any?): ProfileResponse
    suspend fun editProfile(param: Any?): ProfileResponse
}