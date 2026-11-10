package com.iceeboot.common.utils;

import com.iceeboot.common.exception.IceeBootException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExportUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DATE_TIME_FORMATTER);
    }

    public static String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static void writeExcel(HttpServletResponse response,
                                  String fileName,
                                  String sheetName,
                                  List<String> headers,
                                  List<List<String>> rows,
                                  String origin) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setWrapText(true);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> data = rows.get(i);
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(j < data.size() ? data.get(j) : "");
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            writeResponse(response,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileName,
                outputStream.toByteArray(),
                origin);
        } catch (IOException e) {
            throw new IceeBootException("导出Excel失败: " + e.getMessage());
        }
    }

    public static void writeWord(HttpServletResponse response,
                                 String fileName,
                                 String title,
                                 List<String> headers,
                                 List<List<String>> rows,
                                 String origin) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText(title);

            XWPFTable table = document.createTable(rows.size() + 1, headers.size());
            for (int i = 0; i < headers.size(); i++) {
                setCellText(table.getRow(0).getCell(i), headers.get(i), true);
            }

            for (int i = 0; i < rows.size(); i++) {
                List<String> data = rows.get(i);
                for (int j = 0; j < headers.size(); j++) {
                    String value = j < data.size() ? data.get(j) : "";
                    setCellText(table.getRow(i + 1).getCell(j), value, false);
                }
            }

            document.write(outputStream);
            writeResponse(response,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                fileName,
                outputStream.toByteArray(),
                origin);
        } catch (IOException e) {
            throw new IceeBootException("导出Word失败: " + e.getMessage());
        }
    }

    public static void writePdf(HttpServletResponse response,
                                String fileName,
                                String title,
                                List<String> headers,
                                List<List<String>> rows,
                                String origin) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDRectangle pageSize = page.getMediaBox();
            float margin = 40;
            float yPosition = pageSize.getHeight() - margin;
            float leading = 16;
            PDFont pdfFont = resolvePdfFont(document);
            boolean unicodeFont = pdfFont instanceof PDType0Font;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(pdfFont, 16);
            yPosition = writePdfLine(contentStream, title, margin, yPosition, leading, unicodeFont);
            yPosition -= 6;

            for (int i = 0; i < rows.size(); i++) {
                if (yPosition < margin + leading * 2) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }
                contentStream.setFont(pdfFont, 12);
                yPosition = writePdfLine(contentStream, "记录 " + (i + 1), margin, yPosition, leading, unicodeFont);
                contentStream.setFont(pdfFont, 11);
                List<String> data = rows.get(i);
                for (int j = 0; j < headers.size(); j++) {
                    String value = j < data.size() ? data.get(j) : "";
                    String line = headers.get(j) + "：" + value;
                    List<String> wrapped = wrapText(line, 95);
                    for (String text : wrapped) {
                        if (yPosition < margin + leading) {
                            contentStream.close();
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            yPosition = page.getMediaBox().getHeight() - margin;
                            contentStream.setFont(pdfFont, 11);
                        }
                        yPosition = writePdfLine(contentStream, text, margin, yPosition, leading, unicodeFont);
                    }
                }
                yPosition -= 6;
            }

            contentStream.close();
            document.save(outputStream);
            writeResponse(response, "application/pdf", fileName, outputStream.toByteArray(), origin);
        } catch (IOException e) {
            throw new IceeBootException("导出PDF失败: " + e.getMessage());
        }
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(text);
    }

    private static float writePdfLine(PDPageContentStream contentStream,
                                      String text,
                                      float x,
                                      float y,
                                      float leading,
                                      boolean unicodeFont) throws IOException {
        String safeText = sanitizePdfText(text, unicodeFont);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(safeText);
        contentStream.endText();
        return y - leading;
    }

    private static PDFont resolvePdfFont(PDDocument document) throws IOException {
        String configuredPath = System.getProperty("iceeboot.export.pdf.font-path");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("ICEEBOOT_PDF_FONT_PATH");
        }
        List<String> candidates = new ArrayList<>();
        if (configuredPath != null && !configuredPath.isBlank()) {
            candidates.add(configuredPath);
        }
        candidates.add("/System/Library/Fonts/Supplemental/Arial Unicode.ttf");
        candidates.add("/System/Library/Fonts/PingFang.ttc");
        candidates.add("/System/Library/Fonts/STHeiti Light.ttc");
        candidates.add("/System/Library/Fonts/STHeiti Medium.ttc");
        candidates.add("/System/Library/Fonts/Supplemental/Songti.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        candidates.add("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf");
        candidates.add("/usr/share/fonts/truetype/arphic/ukai.ttc");
        candidates.add("/usr/share/fonts/truetype/arphic/uming.ttc");
        candidates.add("/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc");
        candidates.add("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
        candidates.add("C:\\Windows\\Fonts\\simsun.ttc");
        candidates.add("C:\\Windows\\Fonts\\simhei.ttf");
        candidates.add("C:\\Windows\\Fonts\\msyh.ttc");

        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                try {
                    return PDType0Font.load(document, path.toFile());
                } catch (Exception ignored) {
                }
            }
        }
        return PDType1Font.HELVETICA;
    }

    private static String sanitizePdfText(String text, boolean unicodeFont) {
        if (text == null) {
            return "";
        }
        if (unicodeFont) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch <= 0x7F) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }
        return builder.toString();
    }

    private static List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            lines.add("");
            return lines;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines;
    }

    private static void writeResponse(HttpServletResponse response,
                                      String contentType,
                                      String fileName,
                                      byte[] content,
                                      String origin) throws IOException {
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName(fileName) + "\"");
        response.getOutputStream().write(content);
        response.flushBuffer();
    }

    private static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }
}
