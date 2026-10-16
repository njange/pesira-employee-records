package io.pesira.records.user;

public record UserResponse(Long id, String email, String fullName, Role role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
