package ru.skypro.homework.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.skypro.homework.constants.ExceptionMessages;
import ru.skypro.homework.constants.UrlPrefixConstants;
import ru.skypro.homework.dto.ad.AdDto;
import ru.skypro.homework.dto.ad.AdsDto;
import ru.skypro.homework.dto.ad.CreateOrUpdateAdDto;
import ru.skypro.homework.dto.ad.ExtendedAdDto;
import ru.skypro.homework.dto.auth.Role;
import ru.skypro.homework.exception.AdNotFoundException;
import ru.skypro.homework.exception.UnauthorizedAccessException;
import ru.skypro.homework.mapper.AdMapper;
import ru.skypro.homework.model.AdsDao;
import ru.skypro.homework.model.UsersDao;
import ru.skypro.homework.repository.AdRepository;
import ru.skypro.homework.service.AdService;
import ru.skypro.homework.service.CurrentUserService;
import ru.skypro.homework.service.ImageService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация сервиса {@link AdService}.
 * Обеспечивает CRUD-операции с объявлениями, проверку прав доступа
 * (автор или администратор) и управление изображениями через {@link ImageService}.
 *
 * @see AdService
 * @see AdRepository
 * @see AdMapper
 * @see CurrentUserService
 * @see ImageService
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdServiceImpl implements AdService {

    private final AdRepository adRepository;
    private final AdMapper adMapper;

    private final CurrentUserService currentUserService;
    private final ImageService imageService;

    @Value("${app.image.ad-dir}")
    private String adImageDir;

    @Override
    @Transactional(readOnly = true)
    public AdsDto getAllAds() {
        List<AdsDao> ads = adRepository.findAll();
        List<AdDto> adDtos = ads.stream()
                                .map(adMapper::toAdDto)
                                .collect(Collectors.toList());
        AdsDto result = new AdsDto();
        result.setCount(adDtos.size());
        result.setResults(adDtos);
        return result;
    }

    @Override
    public AdDto addAd(String email, CreateOrUpdateAdDto properties, MultipartFile image) {
        UsersDao author = currentUserService.getUserByEmail(email);
        String imagePath = imageService.saveImage(image, adImageDir, UrlPrefixConstants.URL_PREFIX_ADS_IMAGES);

        AdsDao ad = adMapper.toAdEntity(properties);
        ad.setAuthor(author);
        ad.setImage(imagePath);
        AdsDao savedAd = adRepository.save(ad);

        log.info("Ad created with id: {} by user: {}", savedAd.getPk(), email);
        return adMapper.toAdDto(savedAd);
    }

    @Override
    @Transactional(readOnly = true)
    public ExtendedAdDto getAd(Integer id) {
        AdsDao ad = getAdById(id);
        return adMapper.toExtendedAdDto(ad);
    }

    @Override
    public void removeAd(Integer id, String email) {
        AdsDao ad = getAdById(id);
        checkPermissions(ad, email);

        if (ad.getImage() != null) {
            imageService.deleteImage(ad.getImage(), adImageDir);
        }
        adRepository.delete(ad);
        log.info("Ad deleted with id: {} by user: {}", id, email);
    }

    @Override
    public AdDto updateAd(Integer id, String email, CreateOrUpdateAdDto updateAd) {
        AdsDao ad = getAdById(id);
        checkPermissions(ad, email);
        adMapper.updateAdFromDto(updateAd, ad);
        AdsDao updatedAd = adRepository.save(ad);
        log.info("Ad updated with id: {} by user: {}", id, email);
        return adMapper.toAdDto(updatedAd);
    }

    @Override
    @Transactional(readOnly = true)
    public AdsDto getAdsMe() {
        UsersDao author = currentUserService.getCurrentUser(); // получаем текущего пользователя
        List<AdsDao> ads = adRepository.findByAuthorId(author.getId());
        List<AdDto> adDtos = ads.stream()
                                .map(adMapper::toAdDto)
                                .collect(Collectors.toList());
        AdsDto result = new AdsDto();
        result.setCount(adDtos.size());
        result.setResults(adDtos);
        return result;
    }

    @Override
    public byte[] updateImage(Integer id, String email, MultipartFile image) {
        AdsDao ad = getAdById(id);
        checkPermissions(ad, email);

        String newImagePath = imageService.saveImage(image, adImageDir, UrlPrefixConstants.URL_PREFIX_ADS_IMAGES);
        if (ad.getImage() != null) {
            imageService.deleteImage(ad.getImage(), adImageDir);
        }

        ad.setImage(newImagePath);
        adRepository.save(ad);
        log.info("Image updated for ad id: {} by user: {}", id, email);

        return imageService.readImageAsBytes(newImagePath, adImageDir);
    }

    private AdsDao getAdById(Integer id) {
        return adRepository.findById(id)
                           .orElseThrow(() -> new AdNotFoundException(String.format(ExceptionMessages.AD_NOT_FOUND, id)));
    }

    private void checkPermissions(AdsDao ad, String email) {
        UsersDao user = currentUserService.getUserByEmail(email);
        boolean isAuthor = ad.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new UnauthorizedAccessException(String.format(ExceptionMessages.UNAUTHORIZED_ACCESS, "ad"));
        }
    }
}