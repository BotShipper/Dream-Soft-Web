package dreamsoft.dreamsoftweb.mapper;

import dreamsoft.dreamsoftweb.dto.ItemInfoDto;
import dreamsoft.dreamsoftweb.entity.ItemInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemInfoMapper {
    @Mapping(source = "id", target = "infoId")
    ItemInfoDto toDto(ItemInfo itemInfo);

    List<ItemInfoDto> toDtoList(List<ItemInfo> itemInfos);
}
