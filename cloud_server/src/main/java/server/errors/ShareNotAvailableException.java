package server.errors;

public class ShareNotAvailableException extends RuntimeException {
    public ShareNotAvailableException(String message){
        super(message);
    }
}
