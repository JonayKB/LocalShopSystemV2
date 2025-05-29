package es.jonay.kb.shopsystem.services;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.jonay.kb.shopsystem.controller.ImageController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/images")
@CrossOrigin
@Tag(name = "IMAGES", description = "Images management")
public class ImageService {

    private final ImageController imageService;

    Logger logger = Logger.getLogger(ImageService.class.getName());

    public ImageService(ImageController imageService) {
        this.imageService = imageService;
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }
            String contentType = file.getContentType();
            if (contentType != null) {
                if (!contentType.startsWith("image/")) {
                    throw new RuntimeException("File is not an image");
                }
                String filename = imageService.save(file);
                logger.log(Level.INFO, "Image uploaded: {0}", filename);
                return ResponseEntity.ok(filename);
            }
            logger.warning("Error uploading image: Content type is null");
            throw new RuntimeException("File is empty");
        } catch (RuntimeException e) {
            logger.warning("Error uploading image: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(value = "/{imageName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<UrlResource> getImage(@PathVariable("imageName") String imageName) {
        try {
            UrlResource uri = imageService.findImage(imageName);
            logger.log(Level.INFO, "GetImage /images/{0} OK", imageName);
            return ResponseEntity.ok(uri);
        } catch (RuntimeException e) {
            logger.warning("Error getting image: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

}