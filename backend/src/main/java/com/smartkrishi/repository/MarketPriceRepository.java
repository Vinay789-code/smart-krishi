package com.smartkrishi.repository;

import com.smartkrishi.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {
    List<MarketPrice> findByCropNameIgnoreCase(String cropName);

    @Query("SELECT m FROM MarketPrice m WHERE " +
           "(:crop IS NULL OR LOWER(m.cropName) LIKE LOWER(CONCAT('%', :crop, '%'))) AND " +
           "(:state IS NULL OR LOWER(m.state) LIKE LOWER(CONCAT('%', :state, '%'))) AND " +
           "(:district IS NULL OR LOWER(m.district) LIKE LOWER(CONCAT('%', :district, '%')))")
    List<MarketPrice> searchPrices(@Param("crop") String crop,
                                   @Param("state") String state,
                                   @Param("district") String district);

    List<MarketPrice> findTop6ByOrderByPriceDateDesc();
}
