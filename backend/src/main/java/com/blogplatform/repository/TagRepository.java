package com.blogplatform.repository;

import com.blogplatform.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    List<Tag> findByNameIn(Collection<String> names);

    List<Tag> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
}
