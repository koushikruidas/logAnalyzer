package com.poinciana.loganalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LogSearchResponseDTO {
    private List<LogEntryDTO> logs;
    private long totalHits;
}
