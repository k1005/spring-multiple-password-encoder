package com.example

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class AppUser(
    @Id
    var id: UUID,
    val name: String,
    var username: String,
    var password: String
)
