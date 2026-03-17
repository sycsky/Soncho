package com.example.aikef.tool.internal.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.example.aikef.dto.UploadedFileDto;
import com.example.aikef.storage.FileUploadService;
import com.example.aikef.tool.annotation.AutoInjectTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@AutoInjectTool
public class FileOperationTool {

    private final FileUploadService fileUploadService;

    public FileOperationTool(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Tool("Read the content of an attached file. Supports PDF, Excel, Word, Markdown, and Text. Provide the file ID or URL.")
    public String readAttachment(
            @P(value = "File ID (UUID) or URL of the attachment", required = true) String fileIdentifier
    ) {
        try {
            UUID fileId = extractFileId(fileIdentifier);
            if (fileId == null) {
                return "Error: Could not extract a valid File ID from the input.";
            }

            UploadedFileDto fileInfo = fileUploadService.getFile(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

            try (InputStream inputStream = fileUploadService.downloadFile(fileId)) {
                String extension = fileInfo.extension().toLowerCase();
                
                if (extension.contains("pdf")) {
                    return readPdf(inputStream);
                } else if (extension.contains("xls") || extension.contains("xlsx")) {
                    return readExcel(inputStream);
                } else if (extension.contains("doc") || extension.contains("docx")) {
                    return readWord(inputStream);
                } else {
                    // Default to text
                    return IoUtil.read(inputStream, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.error("Error reading attachment: {}", fileIdentifier, e);
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool("Generate an Excel file from JSON data. Returns the download URL.")
    public String generateExcel(
            @P(value = "File name (e.g. report.xlsx)", required = true) String fileName,
            @P(value = "JSON array of objects (e.g. [{'name':'A', 'val':1}, ...])", required = true) String jsonData
    ) {
        try {
            if (!fileName.endsWith(".xlsx")) fileName += ".xlsx";
            
            JSONArray jsonArray = JSONUtil.parseArray(jsonData);
            List<Map<String, Object>> data = new ArrayList<>();
            for (Object obj : jsonArray) {
                if (obj instanceof Map) {
                    data.add((Map<String, Object>) obj);
                }
            }

            if (data.isEmpty()) return "Error: JSON data is empty or invalid.";

            // Use Hutool to write Excel
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.write(data, true);
            writer.flush(out);
            writer.close();

            return uploadGeneratedFile(fileName, out.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        } catch (Exception e) {
            log.error("Error generating Excel", e);
            return "Error generating Excel: " + e.getMessage();
        }
    }

    @Tool("Edit an existing Excel file by applying specific changes. Preserves original formatting. Returns the new file URL.")
    public String editExcel(
            @P(value = "Original File ID or URL", required = true) String fileIdentifier,
            @P(value = "JSON list of operations. Example: [{'type':'UPDATE_CELL', 'sheetIndex':0, 'rowIndex':1, 'colIndex':2, 'value':'NewVal'}, {'type':'APPEND_ROW', 'sheetIndex':0, 'data':['A','B']}]", required = true) String operationsJson
    ) {
        try {
            UUID fileId = extractFileId(fileIdentifier);
            if (fileId == null) return "Error: Invalid File ID.";

            UploadedFileDto fileInfo = fileUploadService.getFile(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));

            String extension = fileInfo.extension().toLowerCase();
            if (!extension.contains("xls")) {
                return "Error: Only Excel files supported for editing.";
            }

            // 1. Load original file
            try (InputStream is = fileUploadService.downloadFile(fileId);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                // Use Hutool's ExcelUtil.getReader to read first, then we need a writer that wraps the POI Workbook
                // But Hutool's ExcelWriter(InputStream) is deprecated or works differently? 
                // Let's check Hutool docs: ExcelUtil.getWriter(File) or ExcelUtil.getWriter(DestFile, templateFile)
                // For streams: ExcelReader reader = ExcelUtil.getReader(is); Workbook wb = reader.getWorkbook();
                // Then wrap wb in Writer.
                
                cn.hutool.poi.excel.ExcelReader reader = ExcelUtil.getReader(is);
                ExcelWriter writer = ExcelUtil.getWriter(reader.getWorkbook());
                
                JSONArray ops = JSONUtil.parseArray(operationsJson);
                for (Object item : ops) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> op = (Map<String, Object>) item;
                    
                    String type = (String) op.get("type");
                    Integer sheetIndex = op.get("sheetIndex");
                    if (sheetIndex == null) sheetIndex = 0;
                    
                    writer.setSheet(sheetIndex);
                    
                    if ("UPDATE_CELL".equalsIgnoreCase(type)) {
                        Integer row = (Integer) op.get("rowIndex");
                        Integer col = (Integer) op.get("colIndex");
                        Object val = op.get("value");
                        if (row != null && col != null) {
                            writer.writeCellValue(col, row, val);
                        }
                    } else if ("APPEND_ROW".equalsIgnoreCase(type)) {
                        Object dataObj = op.get("data");
                        if (dataObj instanceof List) {
                            List<?> rowData = (List<?>) dataObj;
                            writer.writeRow(rowData);
                        }
                    }
                }
                
                writer.flush(out);
                writer.close();

                return uploadGeneratedFile("modified_" + fileInfo.originalName(), out.toByteArray(), fileInfo.contentType());
            }

        } catch (Exception e) {
            log.error("Error editing Excel", e);
            return "Error editing Excel: " + e.getMessage();
        }
    }

    private void addTextToPdf(PDDocument document, PDPage page, String content) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Try to load a Chinese font (Windows specific)
            org.apache.pdfbox.pdmodel.font.PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            boolean isChineseSupported = false;
            
            try {
                File fontFile = new File("C:/Windows/Fonts/simhei.ttf");
                if (fontFile.exists()) {
                    font = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, fontFile);
                    isChineseSupported = true;
                } else {
                     fontFile = new File("C:/Windows/Fonts/msyh.ttf"); // Microsoft YaHei
                     if (fontFile.exists()) {
                         font = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, fontFile);
                         isChineseSupported = true;
                     }
                }
            } catch (Exception e) {
                log.warn("Failed to load Chinese font, falling back to Helvetica", e);
            }

            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(25, 700);
            
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (!isChineseSupported) {
                    // Replace non-ASCII if we don't have a supporting font
                    line = line.replaceAll("[^\\x00-\\x7F]", "?");
                }
                
                // Basic check to avoid writing outside page
                // (This is a very simple implementation, proper wrapping requires calculation)
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15);
            }
            contentStream.endText();
        }
    }

    @Tool("Generate a PDF file from text content. Returns the download URL.")
    public String generatePDF(
            @P(value = "File name (e.g. document.pdf)", required = true) String fileName,
            @P(value = "Text content for the PDF", required = true) String content
    ) {
        try {
            if (!fileName.endsWith(".pdf")) fileName += ".pdf";

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                addTextToPdf(document, page, content);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.save(out);
                
                return uploadGeneratedFile(fileName, out.toByteArray(), "application/pdf");
            }

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    @Tool("Generate a Word document from text content. Returns the download URL.")
    public String generateWord(
            @P(value = "File name (e.g. document.docx)", required = true) String fileName,
            @P(value = "Text content for the Word doc", required = true) String content
    ) {
        try {
            if (!fileName.endsWith(".docx")) fileName += ".docx";

            try (XWPFDocument document = new XWPFDocument()) {
                // Split by double newline for paragraphs
                String[] paragraphs = content.split("\n\n");
                
                for (String paraText : paragraphs) {
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(paraText.replace("\n", " ")); // Replace single newlines with space within paragraph
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.write(out);

                return uploadGeneratedFile(fileName, out.toByteArray(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }

        } catch (Exception e) {
            log.error("Error generating Word doc", e);
            return "Error generating Word doc: " + e.getMessage();
        }
    }

    @Tool("Generate a Markdown file from text content. Returns the download URL.")
    public String generateMarkdown(
            @P(value = "File name (e.g. notes.md)", required = true) String fileName,
            @P(value = "Markdown content", required = true) String content
    ) {
        try {
            if (!fileName.endsWith(".md")) fileName += ".md";
            
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return uploadGeneratedFile(fileName, bytes, "text/markdown");

        } catch (Exception e) {
            log.error("Error generating Markdown", e);
            return "Error generating Markdown: " + e.getMessage();
        }
    }

    // --- Helper Methods ---

    private String uploadGeneratedFile(String fileName, byte[] content, String contentType) throws IOException {
        MultipartFile multipartFile = new InMemoryMultipartFile(fileName, fileName, contentType, content);
        UploadedFileDto result = fileUploadService.uploadFile(
                multipartFile, 
                null, "SYSTEM", // Uploader
                null, null, // Reference
                true // isPublic
        );
        return "File generated successfully: " + result.url() + " (ID: " + result.id() + ")";
    }

    private UUID extractFileId(String input) {
        try {
            // Try direct UUID parsing
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            // Try extracting from URL
            // Pattern: .../files/{uuid} or .../files/{uuid}/download
            Pattern pattern = Pattern.compile("([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})");
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return UUID.fromString(matcher.group(1));
            }
        }
        return null;
    }

    private String readPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String readExcel(InputStream is) {
        // Use Hutool ExcelReader
        // Read first 50 rows to avoid context overflow
        List<Map<String, Object>> rows = ExcelUtil.getReader(is).read(0, 0, 50);
        String json = JSONUtil.toJsonStr(rows);
        if (rows.size() >= 50) {
            json += "\n...(Truncated: showing first 50 rows)";
        }
        return json;
    }

    private String readWord(InputStream is) throws IOException {
        // Supports .docx (XWPF)
        try (XWPFDocument doc = new XWPFDocument(is)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
             // Fallback or specific error for .doc
             return "Error reading Word file (Note: only .docx is supported): " + e.getMessage();
        }
    }

    // --- Inner Class for MultipartFile ---
    
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }
        @Override
        public String getOriginalFilename() { return originalFilename; }
        @Override
        public String getContentType() { return contentType; }
        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }
        @Override
        public long getSize() { return content.length; }
        @Override
        public byte[] getBytes() throws IOException { return content; }
        @Override
        public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
        
        // Needed for Spring 6 / Boot 3 compatibility if interface changed, but for now standard methods:
        // Note: transferTo(Path) might be default in newer Spring versions.
        // If compilation fails, I will add it.
    }
}
