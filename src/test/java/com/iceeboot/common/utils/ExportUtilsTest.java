package com.iceeboot.common.utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ExportUtilsTest {
    @Test void excelIsReadableAndKeepsApplicationCorsHeaders() throws Exception {
        var response = new MockHttpServletResponse();
        response.setHeader("Access-Control-Allow-Origin", "https://allowed.example");
        ExportUtils.writeExcel(response, "测试.xlsx", "data", List.of("name"), List.of(List.of("fixture")), "https://untrusted.example");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("fixture");
        }
        assertThat(response.getHeader("Content-Disposition")).contains(".xlsx");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://allowed.example");
    }
    @Test void wordContainsExportedRows() throws Exception {
        var response = new MockHttpServletResponse();
        ExportUtils.writeWord(response, "fixture.docx", "title", List.of("name"), List.of(List.of("fixture")), null);
        try (var document = new XWPFDocument(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(document.getTables().getFirst().getRow(1).getCell(0).getText()).isEqualTo("fixture");
        }
    }
    @Test void pdfContainsExportedText() throws Exception {
        var response = new MockHttpServletResponse();
        ExportUtils.writePdf(response, "fixture.pdf", "title", List.of("name"), List.of(List.of("fixture")), null);
        try (var document = PDDocument.load(response.getContentAsByteArray())) {
            assertThat(new PDFTextStripper().getText(document)).contains("fixture");
        }
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
