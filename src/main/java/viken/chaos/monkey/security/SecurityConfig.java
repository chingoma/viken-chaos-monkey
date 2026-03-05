package viken.chaos.monkey.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration with role-based access control.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(ChaosSecurityProperties securityProperties,
                                                 PasswordEncoder passwordEncoder) {
        if (!securityProperties.isEnabled()) {
            return new InMemoryUserDetailsManager();
        }

        UserDetails viewer = buildUser(securityProperties.getViewer(), "VIEWER", passwordEncoder);
        UserDetails operator = buildUser(securityProperties.getOperator(), "OPERATOR", passwordEncoder);
        UserDetails admin = buildUser(securityProperties.getAdmin(), "ADMIN", passwordEncoder);
        UserDetails auditor = buildUser(securityProperties.getAuditor(), "AUDITOR", passwordEncoder);
        return new InMemoryUserDetailsManager(viewer, operator, admin, auditor);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ChaosSecurityProperties securityProperties) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        if (!securityProperties.isEnabled()) {
            return http
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                    .build();
        }

        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chaos/status")
                        .hasAnyRole("VIEWER", "AUDITOR", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/chaos/experiments")
                        .hasAnyRole("VIEWER", "AUDITOR", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/chaos/experiments/**")
                        .hasAnyRole("VIEWER", "AUDITOR", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/chaos/experiments")
                        .hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/chaos/experiments/**")
                        .hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/chaos/emergency/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .build();
    }

    private UserDetails buildUser(ChaosSecurityProperties.UserCredentials credentials,
                                  String role,
                                  PasswordEncoder passwordEncoder) {
        if (credentials == null || isBlank(credentials.getUsername()) || isBlank(credentials.getPassword())) {
            throw new IllegalStateException("Security credentials are missing for role " + role);
        }

        return User.withUsername(credentials.getUsername())
                .password(passwordEncoder.encode(credentials.getPassword()))
                .roles(role)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
