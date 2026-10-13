package com.hesoy9.guesthouse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Saves uploaded room photos to a folder on disk (app.upload-dir) - not inside the jar,
// where files can't be written. WebConfig serves that folder at /uploads/**.
@Service
public class PhotoStorageService {

    public static final String URL_PREFIX = "/uploads/";
    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);

    private final Path root;

    public PhotoStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    // Returns the public URL of the stored file, e.g. "/uploads/rooms/3/9f2c....jpg".
    public String storeRoomPhoto(Long roomId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo to upload");
        }
        // Decide the type from the file's first bytes, not its name or the browser's claim,
        // so nothing but a real image can ever be stored and served back.
        String extension = detectImageExtension(file);
        if (extension == null) {
            throw new IllegalArgumentException("Photos must be JPG, PNG, WebP or GIF images");
        }

        Path dir = root.resolve("rooms").resolve(String.valueOf(roomId));
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not save the photo", ex);
        }
        return URL_PREFIX + "rooms/" + roomId + "/" + filename;
    }

    // Deletes the file behind an uploaded photo's URL. External URLs are simply ignored.
    public void deleteIfStored(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        Path file = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(root)) return; // never step outside the upload folder
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Could not delete photo file {}", file, ex);
        }
    }

    private static String detectImageExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException ex) {
            return null;
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) return ".jpg";
        if (read >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') return ".png";
        if (read >= 4 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') return ".gif";
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') return ".webp";
        return null;
    }
}
