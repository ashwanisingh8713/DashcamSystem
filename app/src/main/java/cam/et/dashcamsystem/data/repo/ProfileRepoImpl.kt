package cam.et.dashcamsystem.data.repo


import cam.et.dashcamcore.domain.model.profile.ProfileResponse
import cam.et.dashcamcore.repository.IProfileRepo
import io.ktor.client.HttpClient

class ProfileRepoImpl(private val endPoint: String,
                      private val httpClient: HttpClient): IProfileRepo {
    override suspend fun getProfile(param: Any?): ProfileResponse {
        // TODO("Not yet implemented")
        return ProfileResponse("")
    }

    override suspend fun editProfile(param: Any?): ProfileResponse {
        // TODO("Not yet implemented")
        return ProfileResponse("")
    }
}
