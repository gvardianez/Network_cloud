package models;

public enum MessageType {
    FILE,
    FILE_REQUEST,
    FILES_LIST,
    AUTH,
    REGISTRATION,
    ERROR,
    AUTH_OK,
    DEL_FILES,
    OPEN_DIR,
    CREATE_DIR,
    GO_BACK_DIR,
    SHARE_FILE,
    SEND_SHARE_FILES,
    RECEIVE_SHARE_FILES,
    OPEN_SHARE_DIR,
    REFRESH
}