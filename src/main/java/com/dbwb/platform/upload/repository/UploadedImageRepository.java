package com.dbwb.platform.upload.repository;

import com.dbwb.platform.upload.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, UUID> {

    /**
     * How many bytes this account is storing. COALESCE because an owner who
     * has never uploaded anything has no rows, and SUM over none is null - the
     * quota check would then compare against null rather than zero and let the
     * first upload through a limit of zero.
     */
    @Query("SELECT COALESCE(SUM(u.byteSize), 0) FROM UploadedImage u WHERE u.accountId = :accountId")
    long totalBytesForAccount(@Param("accountId") UUID accountId);
}
