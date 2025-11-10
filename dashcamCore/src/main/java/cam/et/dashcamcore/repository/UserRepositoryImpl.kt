package cam.et.dashcamcore.repository

import cam.et.dashcamcore.domain.model.login.ForgotPasswordResponse
import cam.et.dashcamcore.domain.model.login.SignInResponse
import cam.et.dashcamcore.domain.model.login.SignUpResponse
import cam.et.dashcamcore.domain.model.profile.ProfileResponse
import cam.et.dashcamcore.domain.repo.IUserRepository

class UserRepositoryImpl(private val loginRepo: ILoginRepo, private val profileRepo: IProfileRepo):
    IUserRepository {

    override suspend fun signIn(param: Any?): SignInResponse {
        return loginRepo.signIn(param)
    }

    override suspend fun signUp(param: Any?): SignUpResponse {
        return loginRepo.signUp(param)
    }

    override suspend fun forgotPassword(param: Any?): ForgotPasswordResponse {
        return loginRepo.forgotPassword(param)
    }

    override suspend fun getProfile(param: Any?): ProfileResponse {
        return profileRepo.getProfile(param)
    }

    override suspend fun editProfile(param: Any?): ProfileResponse {
        return profileRepo.editProfile(param)
    }

}