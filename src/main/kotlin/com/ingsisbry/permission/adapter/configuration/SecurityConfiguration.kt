package com.ingsisbry.permission.adapter.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Every request needs an Auth0 access token, except the actuator health probes. Its signature,
 * issuer, audience and expiry are validated from `spring.security.oauth2.resourceserver.jwt.*`
 * in application.yml.
 *
 * Scopes say what the caller may do with the API; which snippet they may touch is decided by
 * the permissions stored here.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests {
                it
                    // Health probes for the orchestrator. Only `health` is exposed (application.yml).
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    // Spring maps each value of the token's "scope" claim to "SCOPE_<value>".
                    .requestMatchers(HttpMethod.GET, "/permissions/**")
                    .hasAuthority(READ_SCOPE)
                    .requestMatchers("/permissions/**")
                    .hasAuthority(WRITE_SCOPE)
                    .anyRequest()
                    .denyAll()
            }.oauth2ResourceServer { it.jwt(withDefaults()) }
            // Bearer tokens make the API stateless: no HTTP session, no cookie to protect.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
        return http.build()
    }

    private companion object {
        const val READ_SCOPE = "SCOPE_read:snippets"
        const val WRITE_SCOPE = "SCOPE_write:snippets"
    }
}
