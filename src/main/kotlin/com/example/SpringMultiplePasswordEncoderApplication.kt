package com.example

import com.example.user.AppUser
import com.example.user.AppUserRepo
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.util.DigestUtils
import java.util.*

@SpringBootApplication
class SpringMultiplePasswordEncoderApplication {

    @Bean
    fun runner(
        userRepo: AppUserRepo,
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): ApplicationRunner {
        return ApplicationRunner {
            run {
                val delegatePasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

                userRepo.saveAll(listOf(
                    AppUser(
                        id = UUID.randomUUID(),
                        username = "md5",
                        password = DigestUtils.md5DigestAsHex("pass".toByteArray()),
                        name = "MD5 User"
                    ),
                    AppUser(
                        id = UUID.randomUUID(),
                        username = "dele",
                        password = delegatePasswordEncoder.encode("pass"),
                        name = "Dele User"
                    )
                ))

                val md5 = userDetailsService.loadUserByUsername("md5")
                println("${md5.username} password matches is ${passwordEncoder.matches("pass", md5.password)}")
                val dele = userDetailsService.loadUserByUsername("dele")
                println("${dele.username} password matches is ${passwordEncoder.matches("pass", dele.password)}")

            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SpringMultiplePasswordEncoderApplication>(*args)
}
