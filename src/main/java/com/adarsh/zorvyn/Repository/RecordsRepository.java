package com.adarsh.zorvyn.Repository;

import com.adarsh.zorvyn.Entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import com.adarsh.zorvyn.Entity.Record;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;

public interface RecordsRepository extends JpaRepository<Record, Integer> {

    @Query("SELECT r FROM Record r WHERE " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:category IS NULL OR LOWER(r.category) = LOWER(:category)) AND " +
            "(:from IS NULL OR r.date >= :from) AND " +
            "(:to IS NULL OR r.date <= :to) AND " +
            "(:search IS NULL OR LOWER(r.note) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Record> findWithFilters(
            @Param("type") Type type,
            @Param("category") String category,
            @Param("from") Date from,
            @Param("to") Date to,
            @Param("search") String search,
            Pageable pageable
    );
}
