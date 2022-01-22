package models;

import lombok.Data;

import java.util.List;

@Data
public class ListShareFilesMessage implements AbstractMessage{

    private List<String> shareFiles;

    public ListShareFilesMessage(List<String> shareFiles) {
        this.shareFiles = shareFiles;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.RECEIVE_SHARE_FILES;
    }
}
