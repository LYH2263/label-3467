package com.blogplatform.service;

import com.blogplatform.entity.ArticleStatus;
import com.blogplatform.repository.ArticleRepository;
import com.blogplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeStatsPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 20000)
    public void pushCommunityStats() {
        Map<String, Object> payload = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "publishedArticles", articleRepository.countByStatus(ArticleStatus.PUBLISHED),
                "registeredUsers", userRepository.count()
        );

        messagingTemplate.convertAndSend("/topic/community-stats", payload);
        log.info("WebSocket stats push: {}", payload);
    }
}
