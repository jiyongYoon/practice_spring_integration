package yoon.jy.practice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import yoon.jy.practice.dto.InferenceType;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter(AccessLevel.PROTECTED)
public class Action {

  @Id
  private Long id;

  private String robotId;

  @Enumerated(EnumType.STRING)
  private InferenceType status;

  private Double x;
  private Double y;
  private Double z;

  private LocalDateTime receiveAt;
  private LocalDateTime sendAt;
  private LocalDateTime saveAt;

  public void updateCoordinates(InferenceType status, Double x, Double y, Double z, LocalDateTime sendAt) {
    this.status = status;
    this.x = x;
    this.y = y;
    this.z = z;
    this.sendAt = sendAt;
  }

}
