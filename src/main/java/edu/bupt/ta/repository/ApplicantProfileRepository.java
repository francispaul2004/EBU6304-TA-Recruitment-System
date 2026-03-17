package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.ApplicantProfile;

import java.util.Optional;

public class ApplicantProfileRepository extends AbstractJsonRepository<ApplicantProfile, String> {

    public ApplicantProfileRepository() {
        super(AppPaths.applicantProfilesJson(), ApplicantProfile.class);
    }

    public Optional<ApplicantProfile> findByUserId(String userId) {
        return findAll().stream()
                .filter(profile -> profile.getUserId() != null && profile.getUserId().equals(userId))
                .findFirst();
    }
}
