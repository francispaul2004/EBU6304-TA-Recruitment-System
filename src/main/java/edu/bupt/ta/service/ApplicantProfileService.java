package edu.bupt.ta.service;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.User;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.IdGenerator;
import edu.bupt.ta.util.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ApplicantProfileService {

    private final ApplicantProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ApplicantProfileService(ApplicantProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public ApplicantProfile getOrCreateProfile(String userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            String id = IdGenerator.next("A", profileRepository.findAll().stream().map(ApplicantProfile::getApplicantId).toList(), 3);
            ApplicantProfile profile = new ApplicantProfile();
            profile.setApplicantId(id);
            profile.setUserId(userId);
            profile.setYear(1);
            profile.setLastUpdated(DateTimeUtils.now());
            userRepository.findById(userId).map(User::getDisplayName).ifPresent(profile::setFullName);
            profileRepository.save(profile);
            return profile;
        });
    }

    public ValidationResult saveProfile(ApplicantProfile profile) {
        List<String> errors = new ArrayList<>();
        if (profile.getFullName() == null || profile.getFullName().isBlank()) {
            errors.add("fullName is required");
        }
        if (profile.getStudentId() == null || profile.getStudentId().isBlank()) {
            errors.add("studentId is required");
        }
        if (profile.getEmail() == null || !profile.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            errors.add("email format is invalid");
        }
        if (profile.getYear() <= 0 || profile.getYear() > 8) {
            errors.add("year must be between 1 and 8");
        }
        if (profile.getPhone() != null && !profile.getPhone().isBlank() && !profile.getPhone().matches("^[0-9+\\-]{6,20}$")) {
            errors.add("phone format is invalid");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.fail(errors);
        }

        profile.setLastUpdated(DateTimeUtils.now());
        profileRepository.save(profile);
        return ValidationResult.ok();
    }

    public int calculateProfileCompletion(String applicantId) {
        return profileRepository.findById(applicantId)
                .map(profile -> {
                    int total = 6;
                    int complete = 0;
                    if (notBlank(profile.getFullName())) complete++;
                    if (notBlank(profile.getStudentId())) complete++;
                    if (notBlank(profile.getProgramme())) complete++;
                    if (profile.getYear() > 0) complete++;
                    if (notBlank(profile.getEmail())) complete++;
                    if (notBlank(profile.getPhone())) complete++;
                    return complete * 100 / total;
                })
                .orElse(0);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
