package genesis.team.addon.util.LifeHackerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Payload {
    void execute() throws Exception;

    default double size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
