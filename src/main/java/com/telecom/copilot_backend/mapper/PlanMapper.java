package com.telecom.copilot_backend.mapper;

import com.telecom.copilot_backend.dto.PlanCatalogDto;
import com.telecom.copilot_backend.entity.PlanCatalog;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between PlanCatalog entity and PlanCatalogDto.
 * Generates mapper implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface PlanMapper {

    /**
     * Convert PlanCatalog entity to PlanCatalogDto.
     *
     * @param planCatalog the plan catalog entity
     * @return plan catalog DTO
     */
    PlanCatalogDto toDto(PlanCatalog planCatalog);

    /**
     * Convert PlanCatalogDto to PlanCatalog entity.
     *
     * @param dto the plan catalog DTO
     * @return plan catalog entity
     */
    PlanCatalog toEntity(PlanCatalogDto dto);
}

