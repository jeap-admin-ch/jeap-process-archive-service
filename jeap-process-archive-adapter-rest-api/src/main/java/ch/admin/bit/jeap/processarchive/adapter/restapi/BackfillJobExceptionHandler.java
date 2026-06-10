package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobException;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobExceptionReason;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BackfillJobController.class)
class BackfillJobExceptionHandler {

    @ExceptionHandler(BackfillJobException.class)
    ResponseEntity<String> handleBackfillJobException(BackfillJobException exception) {
        HttpStatus status = exception.getReason() == BackfillJobExceptionReason.CONFLICT ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<Void> handleAuthorizationDeniedException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
