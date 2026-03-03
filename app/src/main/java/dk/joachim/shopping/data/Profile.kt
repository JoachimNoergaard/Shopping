package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String = "",
    val email: String = "",
    val activationCode: String = "",
)
