package org.uvhnael.fbadsbe2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uvhnael.fbadsbe2.model.entity.Ad;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdsRepository extends JpaRepository<Ad, Long> {

    // Find by ad archive ID (unique identifier from Facebook)
    Optional<Ad> findByAdArchiveId(String adArchiveId);

    // Find by type
    List<Ad> findByTypeAds(String typeAds);

    // Find by status
    List<Ad> findByStatus(String status);

    // Find by page name
    List<Ad> findByPageName(String pageName);

    // Find by date range
    List<Ad> findByTimeCreatedBetween(LocalDate startDate, LocalDate endDate);

    // Find recent ads
    List<Ad> findTop10ByOrderByScrapedAtDesc();

    // Count by type
    @Query("SELECT a.typeAds, COUNT(a) FROM Ad a GROUP BY a.typeAds")
    List<Object[]> countByType();

    // Count by status
    Long countByStatus(String status);

    // Search by caption containing keyword
    List<Ad> findByCaptionContainingIgnoreCase(String keyword);

    // Custom query for statistics
    @Query("SELECT COUNT(a), a.typeAds FROM Ad a WHERE a.timeCreated BETWEEN :startDate AND :endDate GROUP BY a.typeAds")
    List<Object[]> getAdStatisticsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
