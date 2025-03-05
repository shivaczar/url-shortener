package org.bitly.repository;


import org.bitly.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
    void deleteByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = CURRENT_TIMESTAMP WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    List<UrlMapping> findAll(); // Fetch all records

    @Query("SELECT u.originalUrl, COUNT(u) FROM UrlMapping u GROUP BY u.originalUrl ORDER BY COUNT(u) DESC")
    List<Object[]> findTop10MostShortenedUrls(Pageable pageable);

    @Query("SELECT u.shortCode, u.originalUrl, u.clickCount, u.lastAccessedAt " +
            "FROM UrlMapping u ORDER BY u.clickCount DESC, u.lastAccessedAt DESC")
    List<Object[]> findTop10MostClickedUrls(Pageable pageable);

    Optional<UrlMapping> findByShortCodeAndIsDeletedFalse(String shortCode);

    List<UrlMapping> findByUserIdAndIsDeletedFalse(Long userId);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.isDeleted = true WHERE u.shortCode = :shortCode AND u.userId = :userId")
    void softDeleteByShortCode(@Param("shortCode") String shortCode, @Param("userId") Long userId);
}

