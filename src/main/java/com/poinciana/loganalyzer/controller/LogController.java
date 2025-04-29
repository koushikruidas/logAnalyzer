package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/ingest-file")
    public ResponseEntity<List<LogEntryDTO>> ingestLogFile(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(required = false) Long patternId, @RequestParam String  indexName) {
        return new ResponseEntity<>(logService.ingestLogFile(file, patternId, indexName), HttpStatus.OK);
    }

}

