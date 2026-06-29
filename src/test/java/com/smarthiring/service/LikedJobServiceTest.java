package com.smarthiring.service;

import com.smarthiring.model.LikedJob;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.LikedJobRepository;
import com.smarthiring.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LikedJobServiceTest {

    private final LikedJobRepository likedJobRepository = mock(LikedJobRepository.class);
    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final LikedJobService likedJobService = new LikedJobService(likedJobRepository, shiftRepository);

    @Test
    void returnsOnlyCurrentWorkersLikedJobs() {
        User worker = worker(1L);
        LikedJob likedJob = likedJob(worker, shift(10L));
        when(likedJobRepository.findAllByWorkerIdOrderByCreatedAtDesc(worker.getId())).thenReturn(List.of(likedJob));

        List<LikedJob> likedJobs = likedJobService.getLikedJobs(worker);

        assertThat(likedJobs).containsExactly(likedJob);
        verify(likedJobRepository).findAllByWorkerIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void likesShiftForActiveWorker() {
        User worker = worker(1L);
        Shift shift = shift(10L);
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(likedJobRepository.findByWorkerIdAndShiftId(worker.getId(), shift.getId())).thenReturn(Optional.empty());
        when(likedJobRepository.save(any(LikedJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikedJob likedJob = likedJobService.likeShift(shift.getId(), worker);

        assertThat(likedJob.getWorker()).isEqualTo(worker);
        assertThat(likedJob.getShift()).isEqualTo(shift);
        verify(likedJobRepository).save(any(LikedJob.class));
    }

    @Test
    void duplicateLikeReturnsExistingRecord() {
        User worker = worker(1L);
        Shift shift = shift(10L);
        LikedJob existing = likedJob(worker, shift);
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(likedJobRepository.findByWorkerIdAndShiftId(worker.getId(), shift.getId())).thenReturn(Optional.of(existing));

        LikedJob likedJob = likedJobService.likeShift(shift.getId(), worker);

        assertThat(likedJob).isEqualTo(existing);
        verify(likedJobRepository, never()).save(any());
    }

    @Test
    void unlikesOnlyCurrentWorkersShift() {
        User worker = worker(1L);

        likedJobService.unlikeShift(10L, worker);

        verify(likedJobRepository).deleteByWorkerIdAndShiftId(1L, 10L);
    }

    @Test
    void rejectsNonWorkerAccess() {
        User manager = new User();
        manager.setId(2L);
        manager.setRole("MANAGER");
        manager.setStatus("ACTIVE");

        assertThatThrownBy(() -> likedJobService.getLikedJobs(manager))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    private User worker(Long id) {
        User worker = new User();
        worker.setId(id);
        worker.setRole("WORKER");
        worker.setStatus("ACTIVE");
        return worker;
    }

    private Shift shift(Long id) {
        Shift shift = new Shift();
        shift.setId(id);
        shift.setStatus("OPEN");
        return shift;
    }

    private LikedJob likedJob(User worker, Shift shift) {
        LikedJob likedJob = new LikedJob();
        likedJob.setId(100L);
        likedJob.setWorker(worker);
        likedJob.setShift(shift);
        return likedJob;
    }
}
