package com.codeeditor.dto;

import com.codeeditor.model.User;

/** Public-facing view of a user (never exposes the password hash). */
public record UserDto(Long id, String username, String email) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
