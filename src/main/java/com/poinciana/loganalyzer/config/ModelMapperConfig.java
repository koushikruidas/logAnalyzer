package com.poinciana.loganalyzer.config;

import com.poinciana.loganalyzer.entity.LogEntry;
import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        /**
         * Custom mapping for ID conversion (Long <-> String)
         * As LogEntry class has 'long id' and LogEntryDocument has 'String id'
         * and in the DTO class we are using 'String id'
         */
        modelMapper.typeMap(LogEntry.class, LogEntryDTO.class)
                .addMappings(mapper -> mapper.map(src -> String.valueOf(src.getId()), LogEntryDTO::setId));

        modelMapper.typeMap(LogEntryDocument.class, LogEntryDTO.class)
                .addMappings(mapper -> mapper.map(LogEntryDocument::getId, LogEntryDTO::setId));

        modelMapper.typeMap(LogEntryDTO.class, LogEntry.class)
                .addMappings(mapper -> mapper.map(src -> src.getId() != null ? Long.valueOf(src.getId()) : null, LogEntry::setId));

        modelMapper.typeMap(LogEntryDTO.class, LogEntryDocument.class)
                .addMappings(mapper -> mapper.map(LogEntryDTO::getId, LogEntryDocument::setId));

        return modelMapper;
    }

}
