package yoon.jy.practice.service;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yoon.jy.practice.dto.TimeIntervalDto;
import yoon.jy.practice.model.RobotMessage;
import yoon.jy.practice.repository.LogRepository;

@Service
@RequiredArgsConstructor
public class InternalService {

  private final LogRepository logRepository;

  public List<RobotMessage> findAllLogs(String robotId) {
    return logRepository.findAllByRobotId(robotId);
  }

  public TimeIntervalDto getTimeInterval(String robotId) {
    List<RobotMessage> allActionList = logRepository.findAllByRobotId(robotId);

    // 1. SendInterval 평균 (sendAt - receiveAt)
    double avgSendInterval = allActionList.stream()
        .filter(m -> m.getSendAt() != null && m.getReceiveAt() != null)
        .mapToLong(m -> Duration.between(m.getReceiveAt(), m.getSendAt()).toMillis())
        .average()
        .orElse(0.0);

    // 2. SaveInterval 평균 (saveAt - receiveAt)
    double avgSaveInterval = allActionList.stream()
        .filter(m -> m.getSaveAt() != null && m.getReceiveAt() != null)
        .mapToLong(m -> Duration.between(m.getReceiveAt(), m.getSaveAt()).toMillis())
        .average()
        .orElse(0.0);

    return TimeIntervalDto.builder()
        .sendInterval(avgSendInterval)
        .saveInterval(avgSaveInterval)
        .build();
  }
}
