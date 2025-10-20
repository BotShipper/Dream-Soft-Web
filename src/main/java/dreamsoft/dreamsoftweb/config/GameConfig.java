package dreamsoft.dreamsoftweb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "game")
public class GameConfig {

    @Data
    public static class GameInfo {
        private String name;
        private String dataFile;
    }

    private Map<String, GameInfo> configs = new HashMap<>();
}
