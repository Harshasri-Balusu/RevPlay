package com.revplay.musicplatform.catalog.util;

import com.revplay.musicplatform.config.AwsProperties;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.config.StorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorageService {
    private static final Set<String> SONG_AUDIO_EXTENSIONS = Set.of(".mp3", ".wav", ".flac");
    private static final Set<String> PODCAST_AUDIO_EXTENSIONS = Set.of(".mp3", ".aac");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileStorageProperties properties;
    private final StorageProperties storageProperties;
    private final AwsProperties awsProperties;
    private final S3Client s3Client;
    private final Path songsPath;
    private final Path podcastsPath;
    private final Path imagesPath;

    public FileStorageService(FileStorageProperties properties, StorageProperties storageProperties,
                              AwsProperties awsProperties, @Nullable S3Client s3Client) {
        this.properties = properties;
        this.storageProperties = storageProperties;
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
        Path base = Path.of(properties.getBaseDir());
        this.songsPath = base.resolve(properties.getSongsDir());
        this.podcastsPath = base.resolve(properties.getPodcastsDir());
        this.imagesPath = base.resolve(properties.getImagesDir());
        if (isLocalStorage()) {
            ensureDirectories();
        }
    }


    public String storeSong(MultipartFile file) {
        return store(file, songsPath, properties.getSongsDir(), SONG_AUDIO_EXTENSIONS, "Audio file is required");
    }


    public String storePodcast(MultipartFile file) {
        return store(file, podcastsPath, properties.getPodcastsDir(), PODCAST_AUDIO_EXTENSIONS, "Audio file is required");
    }


    public Resource loadSong(String fileName) {
        return load(fileName, songsPath, properties.getSongsDir());
    }

    public Resource loadPodcast(String fileName) {
        return load(fileName, podcastsPath, properties.getPodcastsDir());
    }

    public String storeImage(MultipartFile file) {
        return store(file, imagesPath, properties.getImagesDir(), IMAGE_EXTENSIONS, "Image file is required");
    }

    public Resource loadImage(String fileName) {
        return load(fileName, imagesPath, properties.getImagesDir());
    }

    public void deleteImageFile(String fileName) {
        deleteFile(fileName, imagesPath, properties.getImagesDir());
    }

    public void deleteSongFile(String fileName) {
        deleteFile(fileName, songsPath, properties.getSongsDir());
    }

    public void deletePodcastFile(String fileName) {
        deleteFile(fileName, podcastsPath, properties.getPodcastsDir());
    }

    private String store(MultipartFile file, Path targetDir, String folder, Set<String> allowedExtensions, String missingMessage) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(missingMessage);
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String lower = original.toLowerCase(Locale.ROOT);
        String extension = allowedExtensions.stream()
            .filter(lower::endsWith)
            .findFirst()
            .orElse(null);
        if (extension == null) {
            throw new BadRequestException("Unsupported file type");
        }
        String fileName = UUID.randomUUID() + extension;
        if (isS3Storage()) {
            uploadToS3(file, buildS3Key(folder, fileName));
            return fileName;
        }
        Path target = targetDir.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file {}", fileName, ex);
            throw new BadRequestException("Failed to store file");
        }
        return fileName;
    }

    private Resource load(String fileName, Path localPath, String folder) {
        if (isS3Storage()) {
            return loadFromS3(fileName, folder);
        }
        return loadLocal(localPath.resolve(fileName));
    }

    private Resource loadLocal(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("File not found");
            }
            return new UrlResource(filePath.toUri());
        } catch (IOException ex) {
            log.error("Failed to load file {}", filePath, ex);
            throw new ResourceNotFoundException("File not found");
        }
    }

    private Resource loadFromS3(String fileName, String folder) {
        try {
            ResponseBytes<?> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(buildS3Key(folder, fileName))
                .build());

            Path tempFile = Files.createTempFile("revplay-", "-" + fileName);
            Files.write(tempFile, objectBytes.asByteArray());
            tempFile.toFile().deleteOnExit();
            return new UrlResource(tempFile.toUri());
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("File not found");
        } catch (S3Exception | IOException ex) {
            log.error("Failed to load S3 file {}", fileName, ex);
            throw new ResourceNotFoundException("File not found");
        }
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(songsPath);
            Files.createDirectories(podcastsPath);
            Files.createDirectories(imagesPath);
        } catch (IOException ex) {
            log.error("Failed to initialize upload folders", ex);
            throw new BadRequestException("Failed to initialize upload folders");
        }
    }

    private void deleteFile(String fileName, Path localBasePath, String folder) {
        if (isS3Storage()) {
            deleteFromS3(buildS3Key(folder, fileName));
            return;
        }
        deleteLocalFile(localBasePath.resolve(fileName));
    }

    private void deleteLocalFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Failed to delete file {}", path, ex);
        }
    }

    private void uploadToS3(MultipartFile file, String key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(key)
                .contentType(file.getContentType())
                .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException | S3Exception ex) {
            log.error("Failed to upload file to S3 with key {}", key, ex);
            throw new BadRequestException("Failed to store file");
        }
    }

    private void deleteFromS3(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(key)
                .build());
        } catch (S3Exception ex) {
            log.warn("Failed to delete S3 file {}", key, ex);
        }
    }

    private boolean isLocalStorage() {
        return !isS3Storage();
    }

    private boolean isS3Storage() {
        return "s3".equalsIgnoreCase(storageProperties.getType());
    }

    private String buildS3Key(String folder, String fileName) {
        return folder + "/" + fileName;
    }
}
