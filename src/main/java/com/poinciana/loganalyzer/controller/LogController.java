package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.entity.LogEntry;
import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.model.LogSearchResponseDTO;
import com.poinciana.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/ingest")
    public ResponseEntity<LogEntryDTO> ingestDocumentLog(@RequestBody String rawLog, @RequestParam(required = false) Long patternId) {
        return new ResponseEntity<>(logService.ingestLog(rawLog, patternId), HttpStatus.OK);
    }

    @GetMapping
    public List<LogEntry> getAllLogs() {
        return logService.getAllLogs();
    }

    @GetMapping("/fileterdLogs")
    public List<LogEntry> getFilteredLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return logService.getFilteredLogs(level, serviceName, startDate, endDate);
    }

    @GetMapping("/search")
    public ResponseEntity<LogSearchResponseDTO> searchLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(logService.searchLogs(level, serviceName, keyword, startDate, endDate, page, size));
    }

    @PostMapping("/ingest-file")
    public ResponseEntity<List<LogEntryDTO>> ingestLogFile(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(required = false) Long patternId) {
        return new ResponseEntity<>(logService.ingestLogFile(file, patternId), HttpStatus.OK);
    }

}

