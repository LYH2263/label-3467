package com.blogplatform.util;

public enum ArticleMetricsCalculator {
    INSTANCE;

    private static final int LIKES_WEIGHT = 4;
    private static final int COMMENTS_WEIGHT = 2;

    public long calculateHotScore(int views, long likes, long comments) {
        return views + likes * LIKES_WEIGHT + comments * COMMENTS_WEIGHT;
    }
}
