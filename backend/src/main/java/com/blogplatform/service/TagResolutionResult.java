package com.blogplatform.service;

import com.blogplatform.entity.Tag;

import java.util.Set;

public sealed interface TagResolutionResult permits TagResolutionSuccess, TagResolutionFailure {
}

record TagResolutionSuccess(Set<Tag> tags) implements TagResolutionResult {
}

record TagResolutionFailure(String message) implements TagResolutionResult {
}
