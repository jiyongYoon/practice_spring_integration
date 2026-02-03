package yoon.jy.practice.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yoon.jy.practice.model.RobotMessage;

@Component
@Slf4j
public class LogMemoryRepository implements LogRepository {

  private final List<RobotMessage> robotMessageList = new LinkedList<>();


  @Override
  public void save(RobotMessage robotMessage) {
    // DB 저장 병목을 가정
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      log.warn("Thread sleep 중 인터럽트가 발생했습니다.");
      Thread.currentThread().interrupt();
    }

    boolean sendToEdgeGateway = false;
    for (RobotMessage message : robotMessageList) {
      if (message.getTraceId().equals(robotMessage.getTraceId())) {
        log.info("[Internal] === Log Pipe === 이미 있는 데이터입니다. 메시지에 좌표를 업데이트합니다. robotId={}, coordinates={}, sendAt={}",
            robotMessage.getRobotId(), robotMessage.getPayload().toString(), robotMessage.getSendAt());
        message.updateCoordinates(robotMessage.getPayload(), robotMessage.getSendAt());
        sendToEdgeGateway = true;
        break;
      }
    }

    if (!sendToEdgeGateway) {
      log.info("[Internal] === Log Pipe === 새로운 데이터입니다. DB에 데이터를 추가합니다. message={}",
          robotMessage.toString());
      robotMessage.updateSaveAt(LocalDateTime.now());
      robotMessageList.add(robotMessage);
    }
  }

  @Override
  public List<RobotMessage> findAllByRobotId(String robotId) {
    return robotMessageList.stream()
        .filter(robotMessage -> robotMessage.getRobotId().equals(robotId))
        .sorted(Comparator.comparingInt(RobotMessage::getTraceId))
        .toList();
  }
}
