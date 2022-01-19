package server.errors;

public class NicknameIsNotAvailableException extends RuntimeException {
    public NicknameIsNotAvailableException(String message) {
        super(message);
    }
}
