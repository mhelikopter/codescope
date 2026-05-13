package de.thkoeln.codescope.data.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseAuthResponse(
    val kind: String,

    @SerialName("localId")
    val localId: String,

    val email: String? = null,

    val displayName: String? = null,

    val photoUrl: String? = null,

    @SerialName("idToken")
    val idToken: String,

    @SerialName("refreshToken")
    val refreshToken: String,

    @SerialName("expiresIn")
    val expiresIn: String,

    val isNewUser: Boolean? = null,

    val providerId: String? = null
)