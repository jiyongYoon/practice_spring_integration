package yoon.jy.practice.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import yoon.jy.practice.dto.CoordinatesDto;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RobotMessage {

  private MessageType type;
  private String robotId;
  private Integer traceId;
  private CoordinatesDto payload;
  private LocalDateTime receiveAt;
  private LocalDateTime sendAt;
  private LocalDateTime saveAt;

  public void updateCoordinates(CoordinatesDto coordinatesDto, LocalDateTime sendAt) {
    this.payload = coordinatesDto;
    this.sendAt = sendAt;
  }

  public void updateSaveAt(LocalDateTime saveAt) {
    this.saveAt = saveAt;
  }

}
