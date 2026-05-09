package com.blogplatform.service;

public enum ArticleMetricsCalculator {

    INSTANCE;

    public long calculateHotScore(int views, long likes, long comments) {
        return views + likes * 4 + comments * 2;
    }
}
