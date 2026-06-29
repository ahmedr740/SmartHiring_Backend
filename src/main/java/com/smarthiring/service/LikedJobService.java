package com.smarthiring.service;

import com.smarthiring.model.LikedJob;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.LikedJobRepository;
import com.smarthiring.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class LikedJobService {

    private final LikedJobRepository likedJobRepository;
    private final ShiftRepository shiftRepository;

    public LikedJobService(LikedJobRepository likedJobRepository, ShiftRepository shiftRepository) {
        this.likedJobRepository = likedJobRepository;
        this.shiftRepository = shiftRepository;
    }

    public List<LikedJob> getLikedJobs(User currentUser) {
        requireActiveWorker(currentUser);
        return likedJobRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
    }

    public LikedJob likeShift(Long shiftId, User currentUser) {
        requireActiveWorker(currentUser);
        if (shiftId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "shiftId is required");
        }

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        return likedJobRepository.findByWorkerIdAndShiftId(currentUser.getId(), shift.getId())
                .orElseGet(() -> {
                    LikedJob likedJob = new LikedJob();
                    likedJob.setWorker(currentUser);
                    likedJob.setShift(shift);
                    return likedJobRepository.save(likedJob);
                });
    }

    @Transactional
    public void unlikeShift(Long shiftId, User currentUser) {
        requireActiveWorker(currentUser);
        if (shiftId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "shiftId is required");
        }
        likedJobRepository.deleteByWorkerIdAndShiftId(currentUser.getId(), shiftId);
    }

    private void requireActiveWorker(User user) {
        if (user == null || !"WORKER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only workers can save jobs");
        }

        if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active workers can save jobs");
        }
    }
}
