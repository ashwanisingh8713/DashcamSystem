package cam.et.dashcamcore.domain.model.login

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val success: Boolean = true,
    val message: String = "",
    val redirectUrl: String? = null,
    val token: String? = null,
    val user: User? = null
)

@Serializable
data class User(
    val addressId: Int,
    val alternateCountryCode: Int,
    val alternateNumber: Long,
    val countryCode: Int,
    val email: String,
    val firstName: String,
    val isDeleted: Boolean,
    val mobileNumber: String,
    val password: String,
    val photo: String? = null,
    val primaryContact: Boolean,
    val remarks: String? = null,
    val status: String? = null,
    val unitId: String? = null,
    val userId: Int
)


