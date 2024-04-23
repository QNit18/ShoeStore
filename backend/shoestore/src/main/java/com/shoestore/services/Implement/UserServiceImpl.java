package com.shoestore.services.Implement;

import com.shoestore.components.JwtTokenUtils;
import com.shoestore.dtos.UserDTO;
import com.shoestore.exceptions.DataNotFoundException;
import com.shoestore.exceptions.PermissionDenyException;
import com.shoestore.models.Role;
import com.shoestore.models.User;
import com.shoestore.repositories.RoleRepository;
import com.shoestore.repositories.UserRepository;
import com.shoestore.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtil;
    private final AuthenticationManager authenticationManager;


    @Override
    public User createUser(UserDTO userDTO) throws Exception {
        // Register User
        String phoneNumber = userDTO.getPhoneNumber();
        // Checking phone number has exists ?
        if(userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }

        Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataNotFoundException("Role not found"));

        if ( role.getName().toUpperCase().equals(Role.ADMIN)){
            throw new PermissionDenyException("You cannot register a account with role ADMIN");
        }

        //convert from userDTO => user
        User newUser = User.builder()
                .fullName(userDTO.getFullName())
                .phoneNumber(userDTO.getPhoneNumber())
                .password(userDTO.getPassword())
                .address(userDTO.getAddress())
                .dateOfBirth(userDTO.getDateOfBirth())
                .facebookAccountId(userDTO.getFacebookAccountId())
                .googleAccountId(userDTO.getGoogleAccountId())
                .build();

        newUser.setRole(role);
        // If create by google, facebook then not encode password
        if (userDTO.getFacebookAccountId() == 0 && userDTO.getGoogleAccountId() == 0) {
            String password = userDTO.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            newUser.setPassword(encodedPassword);
        }
        return userRepository.save(newUser);
    }

    @Override
    public String login(String phoneNumber, String password) throws DataNotFoundException {
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);
        if(user.isEmpty()){
            throw new DataNotFoundException("Invalid phone number / password");
        }
        User existingUser = user.get();
        // Check password
        if (existingUser.getFacebookAccountId() == 0 && existingUser.getGoogleAccountId() == 0) {
            if (!passwordEncoder.matches(password, existingUser.getPassword())){
                throw new BadCredentialsException("Wrong phone number / password");
            }
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                phoneNumber, password,
                existingUser.getAuthorities()
        );
        // authenticate with Java spring security
        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateToken(existingUser);
    }
}
