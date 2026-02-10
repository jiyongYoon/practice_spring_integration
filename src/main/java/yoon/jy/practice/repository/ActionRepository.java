package yoon.jy.practice.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import yoon.jy.practice.dto.CoordinatesDto;
import yoon.jy.practice.dto.InferenceResDto;
import yoon.jy.practice.dto.InferenceType;
import yoon.jy.practice.entity.Action;
import yoon.jy.practice.entity.Action.ActionBuilder;
import yoon.jy.practice.model.MessageType;
import yoon.jy.practice.model.RobotMessage;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ActionRepository implements LogRepository {

  private final ActionJpaRepository actionJpaRepository;

  @Override
  public void save(RobotMessage robotMessage) {
    // DB 저장 병목을 가정
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      log.warn("Thread sleep 중 인터럽트가 발생했습니다.");
      Thread.currentThread().interrupt();
    }

    // traceId로 기존 데이터 찾기
    Optional<Action> existingAction = actionJpaRepository.findById(robotMessage.getTraceId());

    if (existingAction.isPresent()) {
      // 이미 있는 데이터 - 좌표 업데이트
      Action action = existingAction.get();

      if (robotMessage.getPayload() != null) {
        log.info("[Internal] === Log Pipe === 이미 있는 데이터입니다. 메시지에 좌표를 업데이트합니다. robotId={}, coordinates={}, sendAt={}",
            robotMessage.getRobotId(), robotMessage.getPayload().toString(), robotMessage.getSendAt());

        boolean isExistsCoordinates = false;
        if (InferenceType.DONE.equals(robotMessage.getPayload().status())) {
          isExistsCoordinates = true;
        }
        action.updateCoordinates(
            robotMessage.getPayload().status(),
            isExistsCoordinates ? Double.valueOf(robotMessage.getPayload().coordinatesDto().getX()) : null,
            isExistsCoordinates ? Double.valueOf(robotMessage.getPayload().coordinatesDto().getY()) : null,
            isExistsCoordinates ? Double.valueOf(robotMessage.getPayload().coordinatesDto().getZ()) : null,
            robotMessage.getSendAt()
        );

        actionJpaRepository.save(action);
      }
    } else {
      // 새로운 데이터 - 추가
      log.info("[Internal] === Log Pipe === 새로운 데이터입니다. DB에 데이터를 추가합니다. message={}",
          robotMessage.toString());

      Action.ActionBuilder actionBuilder = Action.builder()
          .id(robotMessage.getTraceId())
          .robotId(robotMessage.getRobotId())
          .receiveAt(robotMessage.getReceiveAt())
          .sendAt(robotMessage.getSendAt())
          .saveAt(LocalDateTime.now());

      if (robotMessage.getPayload() != null) {
        actionBuilder.status(robotMessage.getPayload().status())
            .x(Double.valueOf(robotMessage.getPayload().coordinatesDto().getX()))
            .y(Double.valueOf(robotMessage.getPayload().coordinatesDto().getY()))
            .z(Double.valueOf(robotMessage.getPayload().coordinatesDto().getZ()));
      }

      actionJpaRepository.save(actionBuilder.build());
    }
  }

  @Override
  public List<RobotMessage> findAllByRobotId(String robotId) {
    return actionJpaRepository.findAllByRobotId(robotId).stream()
        .sorted(Comparator.comparingLong(Action::getId))
        .map(action -> RobotMessage.builder()
            .type(MessageType.LOG)
            .robotId(action.getRobotId())
            .traceId(action.getId())
            .payload(InferenceResDto.builder()
                .coordinatesDto(action.getX() != null 
                    ? CoordinatesDto.builder()
                      .x(action.getX().toString())
                      .y(action.getY().toString())
                      .z(action.getZ().toString())
                      .build()
                    : null)
                .status(action.getStatus())
                .build())
            .receiveAt(action.getReceiveAt())
            .sendAt(action.getSendAt())
            .saveAt(action.getSaveAt())
            .build())
        .toList();
  }
}
