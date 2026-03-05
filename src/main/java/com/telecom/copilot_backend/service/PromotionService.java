package com.telecom.copilot_backend.service;

import com.telecom.copilot_backend.dto.PromotionDto;
import com.telecom.copilot_backend.entity.Promotion;
import com.telecom.copilot_backend.exception.ResourceNotFoundException;
import com.telecom.copilot_backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public List<PromotionDto> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Returns active promotions that the customer qualifies for based on tenure.
     */
    public List<PromotionDto> getEligiblePromotions(Integer tenureMonths) {
        int tenure = (tenureMonths != null) ? tenureMonths : 0;
        return promotionRepository
                .findByIsActiveTrueAndMinTenureMonthsLessThanEqual(tenure)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PromotionDto getPromotionById(Integer promoId) {
        return toDto(findEntityById(promoId));
    }

    @Transactional
    public PromotionDto createPromotion(PromotionDto dto) {
        return toDto(promotionRepository.save(toEntity(dto)));
    }

    @Transactional
    public PromotionDto updatePromotion(Integer promoId, PromotionDto dto) {
        Promotion existing = findEntityById(promoId);
        existing.setPromoName(dto.getPromoName());
        existing.setDiscountPercentage(dto.getDiscountPercentage());
        existing.setMinTenureMonths(dto.getMinTenureMonths());
        existing.setIsActive(dto.getIsActive());
        return toDto(promotionRepository.save(existing));
    }

    @Transactional
    public void deletePromotion(Integer promoId) {
        promotionRepository.delete(findEntityById(promoId));
    }

    // -------------------------------------------------------------------------
    // Package-internal helper
    // -------------------------------------------------------------------------

    public Promotion findEntityById(Integer promoId) {
        return promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "promoId", String.valueOf(promoId)));
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public PromotionDto toDto(Promotion p) {
        return PromotionDto.builder()
                .promoId(p.getPromoId())
                .promoName(p.getPromoName())
                .discountPercentage(p.getDiscountPercentage())
                .minTenureMonths(p.getMinTenureMonths())
                .isActive(p.getIsActive())
                .build();
    }

    private Promotion toEntity(PromotionDto dto) {
        return Promotion.builder()
                .promoName(dto.getPromoName())
                .discountPercentage(dto.getDiscountPercentage())
                .minTenureMonths(dto.getMinTenureMonths())
                .isActive(dto.getIsActive())
                .build();
    }
}

