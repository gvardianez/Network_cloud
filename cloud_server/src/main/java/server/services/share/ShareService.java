package server.services.share;

import java.util.List;

public interface ShareService {

    void start();

    List<String> loadShareFiles(String nickName);

    void shareFile(String nickName, String fileName, String filePath);

    void deleteFile(String filePath);
}
