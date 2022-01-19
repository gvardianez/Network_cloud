package models;

import lombok.Data;

import java.util.List;

@Data
public class DeleteFilesMessage implements AbstractMessage {

    private List<String> filesForDelete;

    public DeleteFilesMessage(List<String> filesForDelete){
        this.filesForDelete = filesForDelete;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DEL_FILES;
    }
}
