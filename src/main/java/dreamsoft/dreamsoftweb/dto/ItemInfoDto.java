package dreamsoft.dreamsoftweb.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemInfoDto {
    private int infoId;
    private String name;
}
