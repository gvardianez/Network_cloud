package server.errors;

public class LoginIsNotAvailableException extends RuntimeException {
    public LoginIsNotAvailableException(String message) {
        super(message);
    }
}
