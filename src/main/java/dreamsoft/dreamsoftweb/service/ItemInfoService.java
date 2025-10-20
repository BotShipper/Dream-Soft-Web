package dreamsoft.dreamsoftweb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dreamsoft.dreamsoftweb.config.GameConfig;
import dreamsoft.dreamsoftweb.dto.ItemInfoDto;
import dreamsoft.dreamsoftweb.entity.ItemInfo;
import dreamsoft.dreamsoftweb.mapper.ItemInfoMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemInfoService {

    private final GameConfig gameConfig;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ItemInfoMapper  itemInfoMapper;

    // Cache data của tất cả các game
    private final Map<String, List<ItemInfo>> gameRewardsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Loading reward data for all games...");
        gameConfig.getConfigs().forEach((gameKey, gameInfo) -> {
            try {
                loadGameData(gameKey, gameInfo.getDataFile());
                log.info("Loaded {} items for game: {}",
                        gameRewardsCache.get(gameKey).size(), gameInfo.getName());
            } catch (Exception e) {
                log.error("Failed to load data for game: {}", gameKey, e);
                gameRewardsCache.put(gameKey, new ArrayList<>());
            }
        });
    }

    private void loadGameData(String gameKey, String filePath) throws IOException {
        Resource resource = resourceLoader.getResource(filePath);
        if (!resource.exists()) {
            log.warn("Data file not found for game {}: {}", gameKey, filePath);
            gameRewardsCache.put(gameKey, new ArrayList<>());
            return;
        }

        List<ItemInfo> rewards = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {}
        );

        gameRewardsCache.put(gameKey, rewards);
    }

    public List<ItemInfoDto> getAllRewards(String gameKey) {
        List<ItemInfo> rewards = gameRewardsCache.get(gameKey);

        return itemInfoMapper.toDtoList(rewards);
    }

    public Optional<ItemInfo> getRewardById(String gameKey, Integer infoId) {
        return gameRewardsCache.getOrDefault(gameKey, new ArrayList<>())
                .stream()
                .filter(reward -> reward.getId() == infoId)
                .findFirst();
    }

    public void reloadGameData(String gameKey) {
        GameConfig.GameInfo gameInfo = gameConfig.getConfigs().get(gameKey);
        if (gameInfo != null) {
            try {
                loadGameData(gameKey, gameInfo.getDataFile());
                log.info("Reloaded data for game: {}", gameKey);
            } catch (IOException e) {
                log.error("Failed to reload data for game: {}", gameKey, e);
            }
        }
    }

    public Set<String> getAvailableGames() {
        return gameConfig.getConfigs().keySet();
    }

    public String getGameName(String gameKey) {
        GameConfig.GameInfo gameInfo = gameConfig.getConfigs().get(gameKey);
        return gameInfo != null ? gameInfo.getName() : gameKey;
    }
}
