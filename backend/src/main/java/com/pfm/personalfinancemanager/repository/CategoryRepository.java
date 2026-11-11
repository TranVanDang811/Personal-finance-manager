package com.pfm.personalfinancemanager.repository;

import com.pfm.personalfinancemanager.entity.Category;
import com.pfm.personalfinancemanager.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    @Query("""
            SELECT c FROM Category c
            WHERE c.isDeleted = false
              AND (c.isSystem = true OR c.user.id = :userId)
              AND (:type IS NULL OR c.categoryType = :type)
            ORDER BY c.isSystem DESC, c.name ASC
            """)
    List<Category> findAllAvailableForUser(
            @Param("userId") String userId,
            @Param("type") CategoryType type
    );

    @Query("""
            SELECT c FROM Category c
            WHERE c.id = :id
              AND c.isDeleted = false
              AND (c.isSystem = true OR c.user.id = :userId)
            """)
    Optional<Category> findAvailableByIdForUser(
            @Param("id") String id,
            @Param("userId") String userId
    );

    Optional<Category> findByIdAndIsDeletedFalse(String id);

    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE c.isDeleted = false
              AND c.categoryType = :type
              AND LOWER(c.name) = LOWER(:name)
              AND (c.isSystem = true OR c.user.id = :userId)
            """)
    boolean existsDuplicateNameForUser(
            @Param("userId") String userId,
            @Param("type") CategoryType type,
            @Param("name") String name
    );

    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE c.isDeleted = false
              AND c.categoryType = :type
              AND LOWER(c.name) = LOWER(:name)
              AND c.id <> :excludeId
              AND (c.isSystem = true OR c.user.id = :userId)
            """)
    boolean existsDuplicateNameForUserExcludingId(
            @Param("userId") String userId,
            @Param("type") CategoryType type,
            @Param("name") String name,
            @Param("excludeId") String excludeId
    );

    @Modifying
    @Query("DELETE FROM Category c WHERE c.isSystem = false")
    void deleteAllCustomCategories();
}

