package com.poinciana.loganalyzer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LogEntryDTO {
    private String level;
    private String serviceName;
    private String message;
    private LocalDateTime timestamp;
}
