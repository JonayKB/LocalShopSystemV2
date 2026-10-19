package es.jonay.kb.shopsystem.controller;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageController {
    private static final String ERROR = "Error: ";
    /** Anchos de miniatura permitidos: se ajusta al siguiente para no generar infinitas variantes. */
    private static final int[] THUMB_WIDTHS = { 160, 320, 640, 1024 };
    private static final String THUMBS_DIR = ".thumbs";

    private final Logger logger = Logger.getLogger(ImageController.class.getName());
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

    /** Resuelve el nombre dentro de uploads y rechaza rutas que se salgan de la carpeta. */
    private Path safeResolve(String imageName) {
        Path base = uploads.toAbsolutePath().normalize();
        Path resolved = base.resolve(imageName).normalize();
        if (!resolved.startsWith(base) || !Files.isRegularFile(resolved)) {
            throw new RuntimeException(ERROR + imageName + " not found");
        }
        return resolved;
    }

    public UrlResource findImage(String imageName) {
        return findImage(imageName, null);
    }

    /**
     * Devuelve la imagen original o, si se pide un ancho, una miniatura cacheada en disco.
     * Si la imagen no se puede reducir (formato no soportado) o ya es más pequeña, devuelve la original.
     */
    public UrlResource findImage(String imageName, Integer width) {
        Path original = safeResolve(imageName);
        Path target = original;
        if (width != null && width > 0) {
            target = thumbnailOf(original, imageName, snapWidth(width));
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }
    }

    /** Tipo MIME según la extensión del fichero servido. */
    public String contentTypeOf(UrlResource resource) {
        String name = resource.getFilename() == null ? "" : resource.getFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static int snapWidth(int requested) {
        for (int w : THUMB_WIDTHS) {
            if (requested <= w) {
                return w;
            }
        }
        return THUMB_WIDTHS[THUMB_WIDTHS.length - 1];
    }

    private Path thumbnailOf(Path original, String imageName, int width) {
        String lower = imageName.toLowerCase(Locale.ROOT);
        boolean keepsAlpha = lower.endsWith(".png") || lower.endsWith(".gif");
        String format = keepsAlpha ? "png" : "jpg";
        Path thumbDir = uploads.toAbsolutePath().normalize().resolve(THUMBS_DIR).resolve(String.valueOf(width));
        Path thumb = thumbDir.resolve(original.getFileName().toString() + "." + format);
        try {
            if (Files.isRegularFile(thumb)
                    && Files.getLastModifiedTime(thumb).compareTo(Files.getLastModifiedTime(original)) >= 0) {
                return thumb;
            }
            BufferedImage source = ImageIO.read(original.toFile());
            if (source == null || source.getWidth() <= width) {
                return original; // formato no soportado o ya es pequeña
            }
            int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
            int type = keepsAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage scaled = new BufferedImage(width, height, type);
            Graphics2D g = scaled.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!keepsAlpha) {
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, width, height);
                }
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            Files.createDirectories(thumbDir);
            Path tmp = Files.createTempFile(thumbDir, "thumb", ".tmp");
            if (!ImageIO.write(scaled, format, tmp.toFile())) {
                Files.deleteIfExists(tmp);
                return original;
            }
            Files.move(tmp, thumb, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return thumb;
        } catch (IOException | RuntimeException e) {
            logger.warning("No se pudo crear la miniatura de " + imageName + ": " + e.getMessage());
            return original;
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
        // Solo el nombre del fichero: nunca rutas que vengan del cliente.
        filename = Paths.get(filename).getFileName().toString();
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
        Path pathForFilename = safeResolve(imageName);
        try {
            Files.delete(pathForFilename);
            for (int w : THUMB_WIDTHS) {
                for (String ext : new String[] { "png", "jpg" }) {
                    Files.deleteIfExists(uploads.toAbsolutePath().normalize().resolve(THUMBS_DIR)
                            .resolve(String.valueOf(w)).resolve(pathForFilename.getFileName() + "." + ext));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }
    }

    public List<String> getAll() {
        try (var stream = Files.list(uploads)) {
            return stream.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new RuntimeException(ERROR + e.getMessage());
        }

    }

}