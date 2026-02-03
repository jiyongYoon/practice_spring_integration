package yoon.jy.practice.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import yoon.jy.practice.model.RobotMessage;
import yoon.jy.practice.repository.LogRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogSaveHandler {

  private final LogRepository logRepository;

  @ServiceActivator(
      inputChannel = "logChannel",
      poller = @Poller(taskExecutor = "logExecutor")
  )
  public void handleInference(Message<RobotMessage> message) {
    log.info("[Internal] === Log Pipe === 데이터를 저장합니다...");
    logRepository.save(message.getPayload());
    log.info("[Internal] === Log Pipe === 데이터를 저장했습니다. 데이터: {}", message.getPayload().toString());
  }
}
