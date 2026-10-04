package com.carrental.controller;

import com.carrental.dto.UserRequest;
import com.carrental.model.Role;
import com.carrental.model.User;
import com.carrental.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AccountController {

    @Autowired
    private UserService userService;

    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@RequestBody UserRequest request, HttpServletRequest httpRequest) {
        try {
            User user = userService.register(request);
            createAuthenticatedSession(user, httpRequest);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request, HttpServletRequest httpRequest) {
        try {
            User user = userService.login(request);
            createAuthenticatedSession(user, httpRequest);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        return userService.getUserByEmail(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void createAuthenticatedSession(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
    }

    // User profiles and account management.
    @GetMapping("/api/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) Role role) {
        if (role != null) {
            return ResponseEntity.ok(userService.getUsersByRole(role));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }


    @GetMapping("/portal")
    public ResponseEntity<Void> portal(Authentication authentication) {
        String page = "/login.html";
        if (authentication != null) {
            for (var authority : authentication.getAuthorities()) {
                switch (authority.getAuthority()) {
                    case "ROLE_ADMIN": page = "/admin.html"; break;
                    case "ROLE_FLEET_MANAGER": page = "/fleet-manager.html"; break;
                    case "ROLE_STAFF": page = "/rental-staff.html"; break;
                    case "ROLE_DRIVER": page = "/driver.html"; break;
                    case "ROLE_CUSTOMER": page = "/customer.html"; break;
                    default: break;
                }
            }
        }
        return ResponseEntity.status(302).header("Location", page).build();
    }
}
