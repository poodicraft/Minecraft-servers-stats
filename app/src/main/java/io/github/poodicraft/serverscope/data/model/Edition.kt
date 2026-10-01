package io.github.poodicraft.serverscope.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Edition(val apiPath: String, val defaultPort: Int, val label: String) {
    JAVA(apiPath = "java", defaultPort = 25565, label = "Java"),
    BEDROCK(apiPath = "bedrock", defaultPort = 19132, label = "Bedrock"),
    ;

    companion object {
        fun fromName(name: String?): Edition = entries.firstOrNull { it.name == name } ?: JAVA
    }
}
