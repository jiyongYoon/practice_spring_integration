package yoon.jy.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InferenceResDto(
    InferenceType status,
    @JsonProperty("coordinates")
    CoordinatesDto coordinatesDto
) {

}
