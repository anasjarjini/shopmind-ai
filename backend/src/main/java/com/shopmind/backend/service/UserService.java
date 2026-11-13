package com.shopmind.backend.service;

import com.shopmind.backend.dto.ChangePasswordRequest;
import com.shopmind.backend.dto.UpdateProfileRequest;
import com.shopmind.backend.dto.UserResponse;
import com.shopmind.backend.entity.User;
import com.shopmind.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Autowired
    private  UserRepository userRepository;
    @Autowired
    private  PasswordEncoder passwordEncoder;



    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest req) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        currentUser.setFullName(req.getFullName().trim());
        currentUser.setPhone(req.getPhone());
        currentUser.setCity(req.getCity());
        currentUser.setAddress(req.getAddress());
        return UserResponse.from(userRepository.save(currentUser));
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(req.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        currentUser.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(currentUser);
    }
}