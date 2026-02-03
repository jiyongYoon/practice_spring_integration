package yoon.jy.practice.dto;

import lombok.Builder;

@Builder
public record InferenceResDto(
    InferenceType status,
    CoordinatesDto coordinatesDto
) {

}
