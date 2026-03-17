package com.revplay.musicplatform.ads.service.impl;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.repository.AdRepository;
import com.revplay.musicplatform.ads.service.AdminAdService;
import com.revplay.musicplatform.config.AwsProperties;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.config.StorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class AdminAdServiceImpl implements AdminAdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAdServiceImpl.class);

    private final AdRepository adRepository;
    private final FileStorageProperties fileStorageProperties;
    private final StorageProperties storageProperties;
    private final AwsProperties awsProperties;
    private final S3Client s3Client;

    public AdminAdServiceImpl(AdRepository adRepository, FileStorageProperties fileStorageProperties,
                              StorageProperties storageProperties, AwsProperties awsProperties,
                              @Nullable S3Client s3Client) {
        this.adRepository = adRepository;
        this.fileStorageProperties = fileStorageProperties;
        this.storageProperties = storageProperties;
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
    }

    @Override
    @Transactional
    public Ad uploadAd(String title, MultipartFile file, Integer durationSeconds) {
        validateInput(title, file, durationSeconds);
        String filename = UUID.randomUUID() + ".mp3";
        String mediaUrl;

        if (isS3Storage()) {
            mediaUrl = uploadAdToS3(file, filename);
        } else {
            mediaUrl = storeAdLocally(file, filename);
        }

        LocalDateTime now = LocalDateTime.now();
        Ad ad = new Ad();
        ad.setTitle(title.trim());
        ad.setMediaUrl(mediaUrl);
        ad.setDurationSeconds(durationSeconds);
        ad.setIsActive(true);
        ad.setStartDate(now);
        ad.setEndDate(now.plusMonths(6));

        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad uploaded successfully: adId={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    private String storeAdLocally(MultipartFile file, String filename) {
        Path adsDir = Path.of(fileStorageProperties.getBaseDir(), fileStorageProperties.getAdsDir());
        try {
            Files.createDirectories(adsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create ads directory {}", adsDir.toAbsolutePath(), e);
            throw new BadRequestException("Could not initialize ads storage");
        }

        Path targetPath = adsDir.resolve(filename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to store ad file {}", targetPath.toAbsolutePath(), e);
            throw new BadRequestException("Could not store ad audio file");
        }
        return "/" + fileStorageProperties.getBaseDir() + "/" + fileStorageProperties.getAdsDir() + "/" + filename;
    }

    private String uploadAdToS3(MultipartFile file, String filename) {
        String key = fileStorageProperties.getAdsDir() + "/" + filename;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return buildPublicUrl(key);
        } catch (IOException | S3Exception e) {
            LOGGER.error("Failed to upload ad file to S3 with key {}", key, e);
            throw new BadRequestException("Could not store ad audio file");
        }
    }

    @Override
    @Transactional
    public Ad deactivateAd(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", id));
        ad.setIsActive(false);
        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad deactivated: adId={}", id);
        return saved;
    }

    @Override
    @Transactional
    public Ad activateAd(Long id) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", id));
        ad.setIsActive(true);
        Ad saved = adRepository.save(ad);
        LOGGER.info("Ad activated: adId={}", id);
        return saved;
    }

    private void validateInput(String title, MultipartFile file, Integer durationSeconds) {
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("title is required");
        }
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new BadRequestException("durationSeconds must be > 0");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean isMp3 = lowerName.endsWith(".mp3") || "audio/mpeg".equals(contentType) || "audio/mp3".equals(contentType);
        if (!isMp3) {
            throw new BadRequestException("Only mp3 files are allowed");
        }
    }

    private boolean isS3Storage() {
        return "s3".equalsIgnoreCase(storageProperties.getType());
    }

    private String buildPublicUrl(String key) {
        return "https://" + awsProperties.getS3().getBucket() + ".s3." + awsProperties.getRegion() + ".amazonaws.com/" + key;
    }
}
