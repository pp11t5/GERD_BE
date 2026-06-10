package com.gerd.domain.auth.repository

import com.gerd.domain.auth.entity.AuthAccount
import com.gerd.domain.auth.entity.enums.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AuthAccountRepository : JpaRepository<AuthAccount, Long> {
    fun findByProviderAndProviderAccountId(
        provider: AuthProvider,
        providerAccountId: String,
    ): Optional<AuthAccount>

    fun existsByProviderAndProviderAccountId(
        provider: AuthProvider,
        providerAccountId: String,
    ): Boolean
}
