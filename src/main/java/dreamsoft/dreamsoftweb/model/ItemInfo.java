package dreamsoft.dreamsoftweb.model;

import lombok.Data;

@Data
public class ItemInfo {
    private int id;
    private int itemType;
    private String name;
    private int levelRequire;
    private int characterType;
    private int quality;
    private int canTrade;
}
