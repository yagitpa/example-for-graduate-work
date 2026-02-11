package ru.skypro.homework.mapper;

import ru.skypro.homework.config.MapStructConfig;
import ru.skypro.homework.dto.ad.AdDto;
import ru.skypro.homework.dto.ad.CreateOrUpdateAd;
import ru.skypro.homework.dto.ad.ExtendedAd;
import ru.skypro.homework.model.Ad;
import org.mapstruct.*;

@Mapper(config = MapStructConfig.class)
public interface AdMapper {

    @Mapping(source = "author.id", target = "author")
    AdDto toAdDto(Ad entity);

    @Mapping(source = "author.firstName", target = "authorFirstName")
    @Mapping(source = "author.lastName", target = "authorLastName")
    @Mapping(source = "author.email", target = "email")
    @Mapping(source = "author.phone", target = "phone")
    ExtendedAd toExtendedAdDto(Ad entity);

    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Ad toAdEntity(CreateOrUpdateAd dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "pk", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void updateAdFromDto(CreateOrUpdateAd dto, @MappingTarget Ad entity);
}