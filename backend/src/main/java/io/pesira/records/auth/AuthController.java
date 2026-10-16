package io.pesira.records.auth;

import io.pesira.records.user.AppUser;
import io.pesira.records.user.AppUserRepository;
import io.pesira.records.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users,
                          TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AppUser user = users.findByEmail(authentication.getName()).orElseThrow();
        Jwt token = tokenService.issue(user);
        return new LoginResponse(token.getTokenValue(), token.getExpiresAt(), UserResponse.from(user));
    }

    // One message for every failure (unknown email, wrong password, disabled user),
    // so the response never reveals whether an account exists.
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleFailedLogin() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
