package io.academicmonitor.identity.api;

import io.academicmonitor.identity.application.AuthFailure;
import io.academicmonitor.identity.application.AuthView;
import io.academicmonitor.identity.application.AuthenticatedAcademicContext;
import io.academicmonitor.identity.application.AuthenticationService;
import io.academicmonitor.identity.application.InstitutionSelectionRequired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService service;
    private final AuthenticatedAcademicContext context;
    private final AuthCookies cookies;
    private final CookieCsrfTokenRepository csrf;

    public AuthController(
            AuthenticationService service,
            AuthenticatedAcademicContext context,
            AuthCookies cookies,
            CookieCsrfTokenRepository csrf) {
        this.service = service;
        this.context = context;
        this.cookies = cookies;
        this.csrf = csrf;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/login")
    public AuthView login(
            @Valid @RequestBody LoginRequest login, HttpServletRequest request, HttpServletResponse response) {
        var session = service.login(login.email(), login.password(), login.institutionId());
        cookies.set(response, session);
        csrf.saveToken(csrf.generateToken(request), request, response);
        return session.view();
    }

    @PostMapping("/refresh")
    public AuthView refresh(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String token, HttpServletResponse response) {
        var session = service.refresh(token);
        cookies.set(response, session);
        return session.view();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        // A valid persisted refresh cookie authenticates logout even after access expiration.
        try {
            service.logout(token);
        } finally {
            cookies.clear(response);
        }
        csrf.saveToken(csrf.generateToken(request), request, response);
    }

    @GetMapping("/me")
    public AuthView me() {
        return service.me(context.current());
    }

    @ExceptionHandler(AuthFailure.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail authenticationFailed(HttpServletResponse response) {
        cookies.clear(response);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, AuthFailure.INVALID_CREDENTIALS);
        problem.setProperty("code", "AUTHENTICATION_FAILED");
        return problem;
    }

    @ExceptionHandler(InstitutionSelectionRequired.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail selectionRequired(InstitutionSelectionRequired exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setProperty("code", "INSTITUTION_SELECTION_REQUIRED");
        problem.setProperty("institutions", exception.institutions());
        return problem;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail invalidLoginRequest() {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La solicitud de inicio de sesión no es válida.");
        problem.setProperty("code", "INVALID_LOGIN_REQUEST");
        return problem;
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 1024) String password,
            UUID institutionId) {
        public LoginRequest {
            if (email != null) {
                email = email.trim();
            }
        }

        @Override
        public String toString() {
            return "LoginRequest[credentials redacted]";
        }
    }
}
