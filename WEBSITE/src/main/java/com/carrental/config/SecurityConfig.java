package com.carrental.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().endsWith(".html") || request.getRequestURI().equals("/portal")) {
                                response.sendRedirect("/login.html");
                            } else {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                            }
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/fleet-manager.html").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers("/rental-staff.html").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/driver.html").hasRole("DRIVER")
                        .requestMatchers("/customer.html").hasRole("CUSTOMER")
                        .requestMatchers("/portal").authenticated()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/me").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/maintenance").hasAnyRole("STAFF", "FLEET_MANAGER", "ADMIN")
                        .requestMatchers("/api/maintenance/**").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/driver-return-reports").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/driver-return-reports").hasAnyRole("DRIVER", "STAFF", "ADMIN")
                        .requestMatchers("/api/pickups/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/returns/**").hasAnyRole("STAFF", "ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/returns/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/returns/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/vehicles/**").hasAnyRole("FLEET_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/confirm").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/*").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll());

        return http.build();
    }

    // Browser origin rules and static uploads resource mapping.
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                Path uploadDir = Paths.get("uploads");
                String uploadPath = uploadDir.toFile().getAbsolutePath();
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadPath + "/");
            }
        };
    }
}
