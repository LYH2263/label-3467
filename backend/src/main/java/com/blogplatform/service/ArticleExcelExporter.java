package com.blogplatform.service;

import com.blogplatform.entity.Article;
import com.blogplatform.exception.BusinessException;
import com.blogplatform.repository.CommentRepository;
import com.blogplatform.repository.FavoriteRepository;
import com.blogplatform.util.ArticleMetricsCalculator;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleExcelExporter {

    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final ArticleMetricsCalculator metricsCalculator = ArticleMetricsCalculator.INSTANCE;

    public byte[] exportArticles(List<Article> articles) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, Long> favoriteCounts = loadFavoriteCounts(articleIds);
        Map<Long, Long> commentCounts = loadCommentCounts(articleIds);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("PsycheGame-Analytics");

            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"ID", "标题", "分类", "状态", "浏览量", "点赞量", "评论数", "热度评分", "最后更新"};
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Article article : articles) {
                long likes = favoriteCounts.getOrDefault(article.getId(), 0L);
                long comments = commentCounts.getOrDefault(article.getId(), 0L);
                int views = article.getViewCount() == null ? 0 : article.getViewCount();
                long score = metricsCalculator.calculateHotScore(views, likes, comments);

                var row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(article.getId());
                row.createCell(1).setCellValue(article.getTitle());
                row.createCell(2).setCellValue(article.getCategory().getName());
                row.createCell(3).setCellValue(article.getStatus().name());
                row.createCell(4).setCellValue(views);
                row.createCell(5).setCellValue(likes);
                row.createCell(6).setCellValue(comments);
                row.createCell(7).setCellValue(score);
                row.createCell(8).setCellValue(formatDateTime(article.getUpdatedAt(), formatter));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出作者数据失败");
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private String formatDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime == null ? "-" : dateTime.format(formatter);
    }

    private Map<Long, Long> loadFavoriteCounts(List<Long> articleIds) {
        return favoriteRepository.countByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("articleId"),
                        row -> (Long) row.get("favoriteCount")
                ));
    }

    private Map<Long, Long> loadCommentCounts(List<Long> articleIds) {
        return commentRepository.countByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("articleId"),
                        row -> (Long) row.get("commentCount")
                ));
    }
}
