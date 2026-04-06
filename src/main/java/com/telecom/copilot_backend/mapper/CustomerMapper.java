package com.telecom.copilot_backend.mapper;

import com.telecom.copilot_backend.dto.CustomerDto;
import com.telecom.copilot_backend.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Customer entity and CustomerDto.
 * Generates mapper implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    /**
     * Convert Customer entity to CustomerDto.
     * Excludes customerId and password from DTO.
     *
     * @param customer the customer entity
     * @return customer DTO
     */
    @Mapping(target = "currentPlanName", expression = "java(customer.getCurrentPlan() != null ? customer.getCurrentPlan().getPlanName() : null)")
    @Mapping(target = "currentPlanId", expression = "java(customer.getCurrentPlan() != null ? customer.getCurrentPlan().getPlanId() : null)")
    CustomerDto toDto(Customer customer);

    /**
     * Convert CustomerDto to Customer entity.
     * Note: This is a partial mapping. Plan and password encoding must be handled separately in service layer.
     *
     * @param dto the customer DTO
     * @return customer entity (without plan and password)
     */
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "currentPlan", ignore = true)
    @Mapping(target = "password", ignore = true)
    Customer toEntity(CustomerDto dto);
}

