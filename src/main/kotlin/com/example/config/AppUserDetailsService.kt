package com.example.config

import com.example.user.AppUserRepo
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(
    val repo: AppUserRepo
): UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = repo.findFirstByUsername(username) ?: throw UsernameNotFoundException("Not found username: $username")
        return AppUserDetails(user)
    }

}