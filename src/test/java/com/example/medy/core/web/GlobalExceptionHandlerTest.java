package com.example.medy.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_returnsBadRequestWithFieldErrors() {
        FieldError fieldError = new FieldError("patientRequestDTO", "firstName", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        assertThat(problem.getProperties()).containsKey("errors");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) problem.getProperties().get("errors");
        assertThat(errors).containsExactly(Map.of("field", "firstName", "message", "must not be blank"));
    }

    @Test
    void handleResponseStatus_usesGivenStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        ProblemDetail problem = handler.handleResponseStatus(ex);

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleResponseStatus_fallsBackToGenericDetail_whenNoReasonGiven() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        ProblemDetail problem = handler.handleResponseStatus(ex);

        assertThat(problem.getDetail()).isEqualTo("Request failed");
    }

    @Test
    void handleTypeMismatch_returnsBadRequestNamingTheOffendingParameter() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("moduleCode");

        ProblemDetail problem = handler.handleTypeMismatch(ex);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Invalid value for 'moduleCode'");
    }

    @Test
    void handleNoResourceFound_returnsNotFound() {
        NoResourceFoundException ex =
                new NoResourceFoundException(HttpMethod.GET, "/swagger-ui/index.html", "swagger-ui/index.html");

        ProblemDetail problem = handler.handleNoResourceFound(ex);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("No such endpoint");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getDetail()).isEqualTo("Access denied");
    }

    @Test
    void handleUnexpected_returnsInternalServerErrorWithoutLeakingExceptionDetails() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("sensitive internal detail"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getDetail()).doesNotContain("sensitive internal detail");
    }
}
