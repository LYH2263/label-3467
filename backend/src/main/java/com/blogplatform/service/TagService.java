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

    private final TagRepository tagRepository;

    @Transactional
    public TagResolutionResult resolveTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new TagResolutionResult.Empty();
        }

        Set<String> normalized = rawTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .limit(8)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) {
            return new TagResolutionResult.Empty();
        }

        List<Tag> existing = tagRepository.findByNameIn(normalized);
        Map<String, Tag> existingMap = existing.stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        Set<Tag> result = new HashSet<>(existing);
        for (String tagName : normalized) {
            if (!existingMap.containsKey(tagName)) {
                Tag tag = new Tag();
                tag.setName(tagName);
                result.add(tagRepository.save(tag));
            }
        }

        return new TagResolutionResult.Success(result);
    }
}
