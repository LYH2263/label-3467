package com.blogplatform.service;

import com.blogplatform.entity.Tag;
import com.blogplatform.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final int MAX_TAGS = 8;

    private final TagRepository tagRepo;

    @Transactional
    public TagResolutionResult resolveTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new TagResolutionSuccess(new HashSet<>());
        }

        Set<String> normalized = rawTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .limit(MAX_TAGS)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Tag> existing = tagRepo.findByNameIn(normalized);
        Map<String, Tag> existingMap = existing.stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        Set<Tag> result = new HashSet<>(existing);
        for (String tagName : normalized) {
            if (!existingMap.containsKey(tagName)) {
                Tag tag = new Tag();
                tag.setName(tagName);
                result.add(tagRepo.save(tag));
            }
        }

        return new TagResolutionSuccess(result);
    }
}
