package payment.service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity  // Enables @PreAuthorize
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    /*public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }*/

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for JWT
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Allow Swagger
                        .requestMatchers("/payment/**").hasAuthority("ADMIN")  // Restrict Payment API
                        .requestMatchers("/inventory/**").hasAuthority("ADMIN")  // Restrict Inventory API
                        .anyRequest().authenticated()  // Secure all other endpoints
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)  // Apply JWT filter
                .build();
    }
}

