package com.example.tournament.security;

import com.example.tournament.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var user = users.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Неверные данные входа"));
            if (user.getPasswordHash() == null)
                throw new UsernameNotFoundException("Неверные данные входа");
            return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                    .password(user.getPasswordHash()).roles(user.getRole())
                    .disabled(!Boolean.TRUE.equals(user.getEnabled())).build();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/account/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/account/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/tournaments/**", "/api/matches/**").hasRole("ORGANIZER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll());
        return http.build();
    }
}
