package org.group3.tutorlink.features.user.service;

import org.group3.tutorlink.features.user.dto.response.UserResponse;
import org.group3.tutorlink.features.user.mapper.UserMapper;
import org.group3.tutorlink.features.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UserService {
//    private final UserMapper userMapper;
    private static UserRepository userRepository;
//    @PreAuthorize("isAuthenticated()")
//    @Transactional(readOnly = true)
//    public UserResponse getMyProfile() {
//        return userMapper.toUserResponse(getCurrentUser());
//    }
}
