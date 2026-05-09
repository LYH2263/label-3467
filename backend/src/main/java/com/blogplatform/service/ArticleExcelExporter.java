package com.blogplatform.service;

import com.blogplatform.dto.ArticleWithMetrics;
import com.blogplatform.entity.Article;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleExcelExporter {

    private final FavoriteRepository favoriteRepo;
    private final CommentRepository commentRepo;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] doExport(List<Article> articles) {
        Map<Long, Long> favoriteCounts = doBatchFavoriteCounts(articles);
        Map<Long, Long> commentCounts = doBatchCommentCounts(articles);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("PsycheGame-Analytics");
            doApplyHeaderStyle(workbook, sheet);

            int rowIndex = 1;
            for (Article article : articles) {
                long likes = favoriteCounts.getOrDefault(article.getId(), 0L);
                long comments = commentCounts.getOrDefault(article.getId(), 0L);
                int views = article.getViewCount() == null ? 0 : article.getViewCount();
                long score = ArticleMetricsCalculator.INSTANCE.calculateHotScore(views, likes, comments);

                var row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(article.getId());
                row.createCell(1).setCellValue(article.getTitle());
                row.createCell(2).setCellValue(article.getCategory().getName());
                row.createCell(3).setCellValue(article.getStatus().name());
                row.createCell(4).setCellValue(views);
                row.createCell(5).setCellValue(likes);
                row.createCell(6).setCellValue(comments);
                row.createCell(7).setCellValue(score);
                row.createCell(8).setCellValue(article.getUpdatedAt() == null ? "-" : article.getUpdatedAt().format(FORMATTER));
            }

            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出作者数据失败");
        }
    }

    private Map<Long, Long> doBatchFavoriteCounts(List<Article> articles) {
        if (articles.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = articles.stream().map(Article::getId).toList();
        return favoriteRepo.countByArticleIds(ids).stream()
                .collect(Collectors.toMap(ArticleWithMetrics::articleId, ArticleWithMetrics::favoriteCount));
    }

    private Map<Long, Long> doBatchCommentCounts(List<Article> articles) {
        if (articles.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = articles.stream().map(Article::getId).toList();
        return commentRepo.countByArticleIds(ids).stream()
                .collect(Collectors.toMap(ArticleWithMetrics::articleId, ArticleWithMetrics::commentCount));
    }

    private void doApplyHeaderStyle(XSSFWorkbook workbook, XSSFSheet sheet) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"ID", "标题", "分类", "状态", "浏览量", "点赞量", "评论数", "热度评分", "最后更新"};
        var headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
}
