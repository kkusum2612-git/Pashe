package com.pashe.app.ambulance.profile;

import com.pashe.app.ambulance.profile.AmbulanceProfileDtos.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AmbulanceProfileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final AmbulanceProfileRepository profileRepository;
    private final AmbulanceEquipmentRepository equipmentRepository;
    private final AmbulanceLicenseDocumentRepository documentRepository;

    public AmbulanceProfileService(
            AmbulanceProfileRepository profileRepository,
            AmbulanceEquipmentRepository equipmentRepository,
            AmbulanceLicenseDocumentRepository documentRepository
    ) {
        this.profileRepository = profileRepository;
        this.equipmentRepository = equipmentRepository;
        this.documentRepository = documentRepository;
    }

    // Register ambulance
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse register(
            UUID operatorId,
            RegistrationRequest request
    ) {
        String registrationNumber = normalizeRegistration(
                request.registrationNumber()
        );

        if (profileRepository.existsByRegistrationNumberIgnoreCase(
                registrationNumber)) {
            throw error(HttpStatus.CONFLICT,
                    "Registration number already exists");
        }

        AmbulanceProfile profile = new AmbulanceProfile(
                operatorId,
                registrationNumber,
                request.vehicleName().strip(),
                request.vehicleType().strip()
        );

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // View operator's ambulances
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public List<ProfileResponse> getMyAmbulances(UUID operatorId) {
        return profileRepository
                .findByOperatorIdOrderByCreatedAtDesc(operatorId)
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    // Update vehicle details
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse updateDetails(
            UUID operatorId,
            UUID ambulanceId,
            RegistrationRequest request
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        String number = normalizeRegistration(
                request.registrationNumber()
        );

        if (!profile.getRegistrationNumber().equalsIgnoreCase(number)
                && profileRepository
                .existsByRegistrationNumberIgnoreCase(number)) {

            throw error(HttpStatus.CONFLICT,
                    "Registration number already exists");
        }

        profile.updateDetails(
                number,
                request.vehicleName().strip(),
                request.vehicleType().strip()
        );

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // Add equipment
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse addEquipment(
            UUID operatorId,
            UUID ambulanceId,
            EquipmentRequest request
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        String name = request.equipmentName().strip();

        if (equipmentRepository
                .existsByAmbulanceIdAndEquipmentNameIgnoreCase(
                        ambulanceId, name)) {

            throw error(HttpStatus.CONFLICT,
                    "Equipment already exists");
        }

        equipmentRepository.save(
                new AmbulanceEquipment(
                        ambulanceId,
                        name,
                        request.quantity()
                )
        );

        profile.resetVerification();

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // Remove equipment
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse removeEquipment(
            UUID operatorId,
            UUID ambulanceId,
            UUID equipmentId
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        AmbulanceEquipment equipment = equipmentRepository
                .findByIdAndAmbulanceId(equipmentId, ambulanceId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Equipment not found"
                ));

        equipmentRepository.delete(equipment);
        profile.resetVerification();

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // Upload license document
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse uploadDocument(
            UUID operatorId,
            UUID ambulanceId,
            MultipartFile file
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Document must be between 1 byte and 5 MB");
        }

        byte[] data;

        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Could not read document");
        }

        if (data.length == 0 || data.length > MAX_FILE_SIZE) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Invalid document size");
        }

        String contentType = detectContentType(data);
        String fileName = sanitizeFileName(
                file.getOriginalFilename()
        );

        documentRepository.save(
                new AmbulanceLicenseDocument(
                        ambulanceId,
                        fileName,
                        contentType,
                        data
                )
        );

        profile.resetVerification();

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // Remove license document
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse removeDocument(
            UUID operatorId,
            UUID ambulanceId,
            UUID documentId
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        documentRepository.delete(
                findDocument(ambulanceId, documentId)
        );

        profile.resetVerification();

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // Set ambulance availability
    @Transactional
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public ProfileResponse updateAvailability(
            UUID operatorId,
            UUID ambulanceId,
            AvailabilityRequest request
    ) {
        AmbulanceProfile profile = lockOwnedProfile(
                operatorId, ambulanceId
        );

        if (request.available()
                && profile.getVerificationStatus()
                != VerificationStatus.APPROVED) {

            throw error(HttpStatus.CONFLICT,
                    "Ambulance must be approved first");
        }

        profile.setAvailable(request.available());

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // View pending applications
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProfileResponse> getPendingAmbulances() {
        return profileRepository
                .findByVerificationStatusOrderByCreatedAtDesc(
                        VerificationStatus.PENDING
                )
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    // Approve or reject ambulance
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileResponse reviewAmbulance(
            UUID adminId,
            UUID ambulanceId,
            ReviewRequest request
    ) {
        AmbulanceProfile profile = lockProfile(ambulanceId);

        if (profile.getVerificationStatus()
                != VerificationStatus.PENDING) {

            throw error(HttpStatus.CONFLICT,
                    "Ambulance is not pending verification");
        }

        if (request.verificationStatus()
                == VerificationStatus.APPROVED) {

            if (!documentRepository.existsByAmbulanceId(
                    ambulanceId)) {

                throw error(HttpStatus.CONFLICT,
                        "License document is required");
            }

            if (equipmentRepository
                    .findByAmbulanceIdOrderByEquipmentNameAsc(
                            ambulanceId
                    ).isEmpty()) {

                throw error(HttpStatus.CONFLICT,
                        "Equipment list is required");
            }

            profile.approve(adminId, request.notes());

        } else if (request.verificationStatus()
                == VerificationStatus.REJECTED) {

            profile.reject(adminId, request.notes());

        } else {
            throw error(HttpStatus.BAD_REQUEST,
                    "Invalid review decision");
        }

        return toProfileResponse(
                profileRepository.saveAndFlush(profile)
        );
    }

    // View verified ambulances
    @PreAuthorize("isAuthenticated()")
    public List<PublicAmbulanceResponse> getVerifiedAmbulances() {
        return profileRepository
                .findByVerificationStatusOrderByCreatedAtDesc(
                        VerificationStatus.APPROVED
                )
                .stream()
                .map(profile -> new PublicAmbulanceResponse(
                        profile.getId(),
                        profile.getRegistrationNumber(),
                        profile.getVehicleName(),
                        profile.getVehicleType(),
                        profile.isAvailable(),
                        getEquipment(profile.getId())
                ))
                .toList();
    }

    // Download private document as operator
    @PreAuthorize("hasRole('AMBULANCE_OPERATOR')")
    public DocumentDownload downloadMyDocument(
            UUID operatorId,
            UUID ambulanceId,
            UUID documentId
    ) {
        AmbulanceProfile profile = profileRepository
                .findById(ambulanceId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Ambulance not found"
                ));

        checkOwnership(profile, operatorId);

        return toDownload(
                findDocument(ambulanceId, documentId)
        );
    }

    // Download private document as administrator
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDownload downloadDocumentForReview(
            UUID ambulanceId,
            UUID documentId
    ) {
        return toDownload(
                findDocument(ambulanceId, documentId)
        );
    }

    public record DocumentDownload(
            String fileName,
            String contentType,
            byte[] data
    ) {}

    // Private helper methods

    private AmbulanceProfile lockProfile(UUID ambulanceId) {
        return profileRepository
                .findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Ambulance not found"
                ));
    }

    private AmbulanceProfile lockOwnedProfile(
            UUID operatorId,
            UUID ambulanceId
    ) {
        AmbulanceProfile profile = lockProfile(ambulanceId);

        checkOwnership(profile, operatorId);

        return profile;
    }

    private void checkOwnership(
            AmbulanceProfile profile,
            UUID operatorId
    ) {
        if (!profile.getOperatorId().equals(operatorId)) {
            throw error(HttpStatus.NOT_FOUND,
                    "Ambulance not found");
        }
    }

    private AmbulanceLicenseDocument findDocument(
            UUID ambulanceId,
            UUID documentId
    ) {
        return documentRepository
                .findByIdAndAmbulanceId(
                        documentId, ambulanceId
                )
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Document not found"
                ));
    }

    private DocumentDownload toDownload(
            AmbulanceLicenseDocument document
    ) {
        return new DocumentDownload(
                document.getFileName(),
                document.getContentType(),
                document.getFileData()
        );
    }

    private List<EquipmentResponse> getEquipment(
            UUID ambulanceId
    ) {
        return equipmentRepository
                .findByAmbulanceIdOrderByEquipmentNameAsc(
                        ambulanceId
                )
                .stream()
                .map(item -> new EquipmentResponse(
                        item.getId(),
                        item.getEquipmentName(),
                        item.getQuantity()
                ))
                .toList();
    }

    private ProfileResponse toProfileResponse(
            AmbulanceProfile profile
    ) {
        List<DocumentResponse> documents = documentRepository
                .findAllByAmbulanceIdOrderByUploadedAtDesc(
                        profile.getId()
                )
                .stream()
                .map(document -> new DocumentResponse(
                        document.getId(),
                        document.getFileName(),
                        document.getContentType(),
                        document.getUploadedAt()
                ))
                .toList();

        return new ProfileResponse(
                profile.getId(),
                profile.getOperatorId(),
                profile.getRegistrationNumber(),
                profile.getVehicleName(),
                profile.getVehicleType(),
                profile.isAvailable(),
                profile.getVerificationStatus(),
                profile.getReviewedBy(),
                profile.getReviewedAt(),
                profile.getReviewNotes(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                getEquipment(profile.getId()),
                documents
        );
    }

    private String normalizeRegistration(String number) {
        return number.strip().toUpperCase(Locale.ROOT);
    }

    private String sanitizeFileName(String original) {
        if (original == null) {
            return "license-document";
        }

        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();

        if (name.isBlank()
                || name.length() > 255
                || name.chars().anyMatch(Character::isISOControl)) {

            throw error(HttpStatus.BAD_REQUEST,
                    "Invalid document filename");
        }

        return name;
    }

    private String detectContentType(byte[] data) {
        if (startsWith(data, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return "application/pdf";
        }

        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }

        if (startsWith(data,
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A)) {

            return "image/png";
        }

        throw error(HttpStatus.BAD_REQUEST,
                "Only PDF, JPEG and PNG documents are allowed");
    }

    private boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (Byte.toUnsignedInt(data[i]) != signature[i]) {
                return false;
            }
        }

        return true;
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }
}