package com.carrental.service;

import com.carrental.dto.UserRequest;
import com.carrental.model.Role;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User register(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());
        // Public registration must never grant operational or administrator roles.
        user.setRole(Role.CUSTOMER);

        return userRepository.save(user);
    }

    public User login(UserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!request.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }

    public User updateUser(Long id, User updatedDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (updatedDetails.getRole() == Role.DRIVER) {
            String drivingLicense = updatedDetails.getDrivingLicense();
            if (drivingLicense == null || drivingLicense.isBlank()) {
                throw new IllegalArgumentException("Driving licence is required when assigning the Driver role");
            }
            user.setDrivingLicense(drivingLicense.trim());
        }

        if (updatedDetails.getName() != null) user.setName(updatedDetails.getName());
        if (updatedDetails.getPhone() != null) user.setPhone(updatedDetails.getPhone());
        if (updatedDetails.getRole() != null) user.setRole(updatedDetails.getRole());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
