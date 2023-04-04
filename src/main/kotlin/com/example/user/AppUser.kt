package com.example.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class AppUser(
    @Id
    var id: UUID,
    val name: String,
    @Column(unique = true)
    var username: String,
    var password: String
)
