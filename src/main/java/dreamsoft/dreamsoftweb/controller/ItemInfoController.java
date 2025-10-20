package dreamsoft.dreamsoftweb.controller;

import dreamsoft.dreamsoftweb.dto.ItemInfoDto;
import dreamsoft.dreamsoftweb.service.ItemInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item-info")
@RequiredArgsConstructor
public class ItemInfoController {

    private final ItemInfoService itemInfoService;

    // Mặc định load file game htht ngay từ đầu
    @GetMapping()
    public ResponseEntity<List<ItemInfoDto>> getPirateRewards() {
        return ResponseEntity.ok(itemInfoService.getAllRewards("htht"));
    }

    // Generic API cho tất cả các game
    @GetMapping("/{gameKey}")
    public ResponseEntity<List<ItemInfoDto>> getGameRewards(
            @PathVariable String gameKey) {

        if (!itemInfoService.getAvailableGames().contains(gameKey)) {
            return ResponseEntity.notFound().build();
        }
        List<ItemInfoDto> rewards = itemInfoService.getAllRewards(gameKey);

        return ResponseEntity.ok(rewards);
    }

    // Lấy tất cả item các game
    @GetMapping("/games")
    public ResponseEntity<Map<String, String>> getAvailableGames() {
        Map<String, String> games = itemInfoService.getAvailableGames()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        key -> key,
                        itemInfoService::getGameName
                ));
        return ResponseEntity.ok(games);
    }

    // Tải lại data từ json
    @PostMapping("/{gameKey}/reload")
    public ResponseEntity<String> reloadGameData(@PathVariable String gameKey) {
        if (!itemInfoService.getAvailableGames().contains(gameKey)) {
            return ResponseEntity.notFound().build();
        }

        itemInfoService.reloadGameData(gameKey);
        return ResponseEntity.ok("Data reloaded for game: " + gameKey);
    }

}
