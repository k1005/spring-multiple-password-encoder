package com.example.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppUserRepo: JpaRepository<AppUser, UUID> {

    fun findFirstByUsername(username: String): AppUser?
}