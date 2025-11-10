package cam.et.dashcamcore.repository

import cam.et.dashcamcore.domain.model.profile.ProfileResponse

interface IProfileRepo {
    suspend fun getProfile(param: Any?): ProfileResponse
    suspend fun editProfile(param: Any?): ProfileResponse
}