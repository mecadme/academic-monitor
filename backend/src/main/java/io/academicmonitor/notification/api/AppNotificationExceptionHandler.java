package io.academicmonitor.notification.api;

import io.academicmonitor.notification.application.AppNotificationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AppNotificationController.class)
class AppNotificationExceptionHandler {
    @ExceptionHandler(AppNotificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void notificationNotFound() {}
}
