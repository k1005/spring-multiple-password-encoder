# SpringBoot Multiple PasswordEncoder

스프링 시큐리티 사용중 간혹 여러 유형의 패스워드 엔코더가 필요한 경우가 있다.
그럴때 필요한 설정이 무엇인지 확인해 보자.

결론만 보고 싶다면 [PasswordEncoder](#passwordencoder) 섹션으로 바로 이동.

## 프로젝트 설정

이번 테스트에 필요한 스프링부트 디펜던시는 다음과 같다.

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- h2 database

이번 테스트는 빠른 속도로 작성하기 위해 `kotlin` 언어를 사용한다.
이를 위해 build.gradle.kts 파일에서 allopen 설정을 추가한다.

```kotlin
plugins {
    kotlin("plugin.allopen") version "1.7.22"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

## 사용자 설정

로그인할 사용자의 데이터를 데이터베이스에 저장하고 해당 데이터로 로그인을 할 수 있도록 한다.

### Entity

먼저 Entity 설정을 해 보자. 테스트를 위한 설정이니 최대한 가볍게 작성했다.

```kotlin
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
```

### Repository

앞서 만든 Entity를 이용할 수 있는 repository 작성을 해야 한다.
다른 기능이 필요한 것이 아니기에 마찬가지로 로그인에 필요한 username 으로 사용자를 추출하는 부분만 작성한다. 

```kotlin
package com.example.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AppUserRepo: JpaRepository<AppUser, UUID> {

    fun findFirstByUsername(username: String): AppUser?
}
```

### UserDetails

로그인시에 SpringSecurity 에서는 기본적으로 UserDetailsService와 UserDetails 인터페이스를 상속받은 객체를 사용하게 되어 있다.

우선 UserDetails를 구현하는 구현체를 만들어 보자.

```kotlin
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
```

### UserDetailsService

앞서 만든 Repository를 통해 사용자를 구해서 UserDetails를 구현한 구현체로 반환해 주는 서비스를 생성한다.

```kotlin
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
```

## PasswordEncoder

패스워드 엔코더를 구현한다.

```kotlin
package com.example.config

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.util.DigestUtils

class AppPasswordEncoder: PasswordEncoder {
    private val pe = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    override fun encode(rawPassword: CharSequence): String {
        return pe.encode(rawPassword)
    }

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean {
        return DigestUtils.md5DigestAsHex(rawPassword.toString().toByteArray()) == encodedPassword ||
                pe.matches(rawPassword, encodedPassword)
    }
}
```

기본 패스워드 알고리즘은 딜리게이트 바이크립트 알고리즘을 사용하고 보조 알고리즘으로 MD5 알고리즘을 활용한다.

여기서 주의해야 할 것은 `fun matches()` 함수 안에서 비교 순서이다.
`DelegatingPasswordEncoder` 먼저 검사를 하게 되면 아래와 같은 오류를 만날 수 있다.

```text
java.lang.IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"
```

그것은 `DelegatingPasswordEncoder`는 선언 아이디를 찾지 못하면 false 응답이 아닌 Exception 처리가 되어 있기 때문이다.

이제 `UserDetailsService` 와 `PasswordEncoder` 를 `@Bean`으로 등록하자.

```kotlin
package com.example.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration(proxyBeanMethods = false)
class SecurityConfig(
    val userDetailsService: AppUserDetailsService
) {

    @Bean
    fun userDetailsService(): UserDetailsService {
        return userDetailsService
    }

    @Bean
    fun customPasswordEncoder(): PasswordEncoder {
        return AppPasswordEncoder()
    }

}
```

## Test

간단히 테스트하기 위해 `ApplicationRunner`를 작성해 `Bean` 으로 등록하자.

```kotlin
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
```

`AppUserRepo` 구현체를 받아 사용자를 임의로 2명 각각 다른 password 알고리즘을 사용해 등록한다.
그리고 `UserDetailsService`를 통해 각각 받아서 `@Bean`으로 등록된 `PasswordEncoder`를 통해 검증을 통과 하는지 확인한다.

```text
md5 password matches is true
dele password matches is true
```

이와 같은 결과로 두가지 알고리즘 모두 정상적으로 진행되는 것을 확인할 수 있다.

---

[GitHub Repository](https://github.com/k1005/spring-multiple-password-encoder)
에 전체 소스가 있습니다.
