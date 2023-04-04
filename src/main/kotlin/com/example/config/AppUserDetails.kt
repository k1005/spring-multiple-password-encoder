package com.example.config

import com.example.user.AppUser
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class AppUserDetails(
    val user: AppUser
) : UserDetails {
    override fun getAuthorities() = listOf<SimpleGrantedAuthority>(SimpleGrantedAuthority("USER"))
    override fun getPassword() = user.password
    override fun getUsername() = user.username
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}