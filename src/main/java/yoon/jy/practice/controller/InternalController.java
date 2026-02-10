package yoon.jy.practice.controller;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yoon.jy.practice.dto.TimeIntervalDto;
import yoon.jy.practice.model.MessageType;
import yoon.jy.practice.model.RobotMessage;
import yoon.jy.practice.repository.IdMemoryRepository;
import yoon.jy.practice.service.InternalService;
import yoon.jy.practice.service.RobotMessageSender;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

  private final RobotMessageSender robotMessageSender;
  private final IdMemoryRepository idMemoryRepository;
  private final InternalService internalService;

  @GetMapping("/coordinates")
  public void requestCoordinates(@RequestParam String robotId) {
    LocalDateTime receiveAt = LocalDateTime.now();
    Long traceId = idMemoryRepository.generateTraceId();
    log.info("[Internal] 좌표 요청 명령 전달 수신: robotId={}, traceId={}, receiveAt={}",
        robotId, traceId, receiveAt);

    for (MessageType messageType : MessageType.values()) {
      RobotMessage robotMessage = RobotMessage.builder()
          .traceId(traceId)
          .robotId(robotId)
          .receiveAt(receiveAt)
          .type(messageType)
          .build();
      robotMessageSender.processMessage(robotMessage);
    }

    log.info("[Internal] 좌표 요청 명령 전달 완료!");
  }

  @GetMapping("/logs")
  public List<RobotMessage> getAllLogs(@RequestParam String robotId) {
    return internalService.findAllLogs(robotId);
  }

  @GetMapping("/logs/timeInterval")
  public TimeIntervalDto getTimeInterval(@RequestParam String robotId) {
    return internalService.getTimeInterval(robotId);
  }
}
