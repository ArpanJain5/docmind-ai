package com.docmind.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(@Value("${app.storage.dir}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the storage folder " + root, e);
        }
    }

    // Saves the bytes under a name WE invent. The person's file name never touches the disk.
    public String save(byte[] content) throws IOException {
        String storedName = UUID.randomUUID() + ".pdf";
        Files.write(root.resolve(storedName), content);
        return storedName;
    }

    // Best effort: a leftover file is harmless, a crash while deleting is not
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", storedName, e);
        }
    }
}