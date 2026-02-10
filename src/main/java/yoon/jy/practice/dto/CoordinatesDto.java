package yoon.jy.practice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CoordinatesDto {

  private String x;
  private String y;
  private String z;
  private String rx;
  private String ry;
  private String rz;

}
