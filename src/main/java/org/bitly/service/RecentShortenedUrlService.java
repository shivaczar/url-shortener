package org.bitly.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.bitly.entity.RecentShortenedUrl;
import org.bitly.repository.RecentShortenedUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecentShortenedUrlService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RecentShortenedUrlRepository repository;

    @Transactional
    public void refreshMaterializedView() {
        entityManager.createNativeQuery("DROP TABLE IF EXISTS recent_shortened_urls").executeUpdate();
        entityManager.createNativeQuery(
                "CREATE TABLE recent_shortened_urls AS " +
                        "SELECT * FROM url_shortener ORDER BY created_at DESC LIMIT 10"
        ).executeUpdate();
    }


    public List<RecentShortenedUrl> getRecentUrls() {
        return repository.findLatestUrls();
    }
}

