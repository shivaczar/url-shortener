package org.bitly.repository;

import org.bitly.entity.RecentShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentShortenedUrlRepository extends JpaRepository<RecentShortenedUrl, Long> {

    @Query("SELECT r FROM RecentShortenedUrl r ORDER BY r.createdAt DESC")
    List<RecentShortenedUrl> findLatestUrls();
}

