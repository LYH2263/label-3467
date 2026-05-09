package com.blogplatform.service;

import com.blogplatform.entity.Tag;

import java.util.Set;

public sealed interface TagResolutionResult permits TagResolutionResult.Success, TagResolutionResult.Empty {
    record Success(Set<Tag> tags) implements TagResolutionResult {
    }

    record Empty() implements TagResolutionResult {
    }
}
