package com.evaluation.prototype.cie.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.cie.entity.CieMark;

public interface CieMarkRepository extends JpaRepository<CieMark, UUID> {
    Optional<CieMark> findByComponentIdAndStudentId(UUID componentId, UUID studentId);

    List<CieMark> findByComponentIdIn(List<UUID> componentIds);

    List<CieMark> findByStudentIdAndComponentIdIn(UUID studentId, List<UUID> componentIds);
}
