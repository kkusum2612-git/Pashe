
package com.pashe.app.medicine.prescription;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MedicinePrescriptionService {
    private static final long MAX_BYTES = 4 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private final MedicinePrescriptionRepository repository;

    public MedicinePrescriptionService(MedicinePrescriptionRepository repository) {
        this.repository = repository;
    }

    public List<MedicinePrescription> list(UUID userId) {
        return repository.findByUserId(userId);
    }

    public MedicinePrescription create(UUID userId, String doctorName, LocalDate date, MultipartFile file)
            throws IOException {
        String name = doctorName.strip();
        if (name.isEmpty() || name.length() > 150 || date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor name and prescription date are required");
        }

        MedicinePrescriptionRepository.Image image = file == null ? null : validateImage(file);
        MedicinePrescription prescription = new MedicinePrescription(UUID.randomUUID(), name, date, image != null);
        repository.insert(userId, prescription, image);
        return prescription;
    }

    public void requireOwned(UUID userId, UUID id) {
        if (id != null && !repository.exists(userId, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found");
        }
    }

    public MedicinePrescriptionRepository.Image getImage(UUID userId, UUID id) {
        return repository.findImage(userId, id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription image not found")
        );
    }

    private MedicinePrescriptionRepository.Image validateImage(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Prescription image exceeds 4 MB");
        }

        byte[] bytes = file.getBytes();
        try (var stream = new MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (readers.hasNext()) {
                var reader = readers.next();
                try {
                    reader.setInput(stream);
                    String type = switch (reader.getFormatName().toLowerCase(java.util.Locale.ROOT)) {
                        case "jpeg" -> "image/jpeg";
                        case "png" -> "image/png";
                        default -> null;
                    };
                    if (type != null && (long) reader.getWidth(0) * reader.getHeight(0) <= MAX_PIXELS
                            && reader.read(0) != null) {
                        return new MedicinePrescriptionRepository.Image(bytes, type);
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prescription image could not be read", ex);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a JPEG or PNG prescription image");
    }
}
