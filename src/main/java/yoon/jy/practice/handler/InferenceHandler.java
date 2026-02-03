package yoon.jy.practice.handler;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import yoon.jy.practice.service.RobotMessageSender;
import yoon.jy.practice.dto.CoordinatesDto;
import yoon.jy.practice.model.MessageType;
import yoon.jy.practice.model.RobotMessage;
import yoon.jy.practice.service.AiModelDummyService;
import yoon.jy.practice.service.EdgeGatewayDummyService;

@Slf4j
@Component
@RequiredArgsConstructor
public class InferenceHandler {

  private final AiModelDummyService aiModelDummyService;
  private final EdgeGatewayDummyService edgeGatewayDummyService;
  private final RobotMessageSender robotMessageSender;

  @ServiceActivator(
      inputChannel = "inferenceChannel",
      poller = @Poller(
          fixedDelay = "10",
          maxMessagesPerPoll = "1",
          taskExecutor = "inferenceExecutor"
      )
  )
  public void handleInference(Message<RobotMessage> message) {
    CoordinatesDto coordinatesDto = aiModelDummyService.inference();
    log.info("[Internal] === Inference Pipe === 추론 결과 수신 완료!");
    edgeGatewayDummyService.sendInferenceData(coordinatesDto);
    log.info("[Internal] === Inference Pipe === 추론 결과 송신 완료!");

    log.info("[Internal] === Log Pipe === 추론 결과 저장 요청!");
    robotMessageSender.processMessage(RobotMessage.builder()
        .traceId(message.getPayload().getTraceId())
        .type(MessageType.LOG)
        .robotId(message.getPayload().getRobotId())
        .payload(coordinatesDto)
        .sendAt(LocalDateTime.now())
        .build());
    log.info("[Internal] === Log Pipe === 추론 결과 저장 요청 완료!");
  }

}
