package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.entity.LogPattern;
import com.poinciana.loganalyzer.model.LogPatternDTO;
import com.poinciana.loganalyzer.repository.LogPatternRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/log-patterns")
@RequiredArgsConstructor
public class LogPatternController {

    private final LogPatternRepository logPatternRepository;
    private final ModelMapper modelMapper;

    // ✅ Add New Log Pattern
    @PostMapping
    public ResponseEntity<LogPatternDTO> createLogPattern(@RequestBody LogPatternDTO logPatternDTO) {
        LogPattern logPattern = modelMapper.map(logPatternDTO, LogPattern.class);
        LogPattern savedPattern = logPatternRepository.save(logPattern);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(savedPattern, LogPatternDTO.class));
    }

    // ✅ Get All Log Patterns
    @GetMapping
    public ResponseEntity<List<LogPatternDTO>> getAllPatterns() {
        List<LogPattern> patterns = logPatternRepository.findAll();
        List<LogPatternDTO> dtos = patterns.stream()
                .map(pattern -> modelMapper.map(pattern, LogPatternDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ✅ Get a Single Log Pattern by ID
    @GetMapping("/{id}")
    public ResponseEntity<LogPatternDTO> getPatternById(@PathVariable Long id) {
        return logPatternRepository.findById(id)
                .map(pattern -> modelMapper.map(pattern, LogPatternDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Update Log Pattern
    @PutMapping("/{id}")
    public ResponseEntity<LogPatternDTO> updateLogPattern(@PathVariable Long id, @RequestBody LogPatternDTO logPatternDTO) {
        return logPatternRepository.findById(id).map(existingPattern -> {
            modelMapper.map(logPatternDTO, existingPattern);
            existingPattern.setId(id); // Ensure ID remains unchanged
            LogPattern updatedPattern = logPatternRepository.save(existingPattern);
            return ResponseEntity.ok(modelMapper.map(updatedPattern, LogPatternDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ✅ Delete Log Pattern
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLogPattern(@PathVariable Long id) {
        if (logPatternRepository.existsById(id)) {
            logPatternRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
