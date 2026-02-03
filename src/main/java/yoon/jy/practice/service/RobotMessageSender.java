package yoon.jy.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import yoon.jy.practice.model.RobotMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotMessageSender {

  private final MessageChannel inferenceChannel;
  private final MessageChannel logChannel;

  @Async
  public void processMessage(RobotMessage robotMessage) {
    switch (robotMessage.getType()) {
      case CONTROL -> {
        log.info("[Internal] 추론 요청 메시지를 inferenceChannel로 전송. robotId={}", robotMessage.getRobotId());
        inferenceChannel.send(MessageBuilder.withPayload(robotMessage).build());
      }
      case LOG -> {
        log.info("[Internal] 로그 메시지를 logChannel로 전송. robotId={}", robotMessage.getRobotId());
        logChannel.send(MessageBuilder.withPayload(robotMessage).build());
      }
      default -> throw new IllegalArgumentException("[Internal] 정의되지 않은 메시지 타입: " + robotMessage.getType());
    }
  }

}
