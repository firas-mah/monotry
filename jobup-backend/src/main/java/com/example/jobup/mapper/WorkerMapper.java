package com.example.jobup.mapper;

import com.example.jobup.dto.*;
import com.example.jobup.entities.Worker;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WorkerMapper {

    // 🔁 Convertir entity → réponse API
    WorkerResponseDto toResponseDto(Worker worker);

    // 🔁 Convertir DTO de création → entity
    @Mapping(target = "fullName", ignore = true) // Ignoré car défini manuellement
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    Worker toEntity(WorkerCreateDto dto);

    // 🔁 Convertir DTO de mise à jour → entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateWorkerFromDto(WorkerUpdateDto dto, @MappingTarget Worker entity);
}
