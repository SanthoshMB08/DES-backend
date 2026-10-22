package com.evaluation.prototype.booklet.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.booklet.entity.BookletRecord;

public interface BookletRecordRepository extends JpaRepository<BookletRecord, UUID> {
    Optional<BookletRecord> findByBookletNumber(String bookletNumber);

    Optional<BookletRecord> findByBookletNumberIgnoreCase(String bookletNumber);

}
