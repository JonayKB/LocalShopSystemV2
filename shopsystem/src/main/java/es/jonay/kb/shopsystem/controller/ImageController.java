package es.jonay.kb.shopsystem.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageController {
    private static final String ERROR = "Error: ";
    private final Path uploads = Paths.get("uploads");

    private Path getFreePath(String filename, Path filePath) {
        int counter = 1;
        while (Files.exists(filePath)) {
            String newFilename = filename.replaceFirst("(\\.[^.]+$|$)", "_" + counter + "$1");
            filePath = this.uploads.resolve(newFilename);
            counter++;
        }
        return filePath;
    }

    public UrlResource findImage(String imageName) {
        try {
            Path pathForFilename = uploads.resolve(imageName);
            UrlResource resource = new UrlResource(pathForFilename.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException(ERROR + imageName + " not found");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }
    }

    public String save(MultipartFile file) {
        try {
            Files.createDirectories(uploads);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("Filename is null");
        }
        Path filePath = this.uploads.resolve(filename);
        filePath = getFreePath(filename, filePath);
        try {
            Files.copy(file.getInputStream(), filePath);
            return filePath.getFileName().toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    public void delete(String imageName) {
        Path pathForFilename = uploads.resolve(imageName);
        try {
            Files.delete(pathForFilename);
        } catch (IOException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }
    }

    public List<String> getAll() {
        try (var stream = Files.list(uploads)) {
            return stream.map(path -> path.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }

    }

}