package com.blogplatform.service;

import com.blogplatform.entity.Tag;

import java.util.Set;

public sealed interface TagResolutionResult permits TagResolutionResult.Success, TagResolutionResult.Failure {

    record Success(Set<Tag> tags) implements TagResolutionResult {}
    record Failure(String reason) implements TagResolutionResult {}
}
