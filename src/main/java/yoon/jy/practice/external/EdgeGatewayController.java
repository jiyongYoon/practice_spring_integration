package yoon.jy.practice.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yoon.jy.practice.dto.InferenceResDto;

@RestController
@RequestMapping("/edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeGatewayController {

  @PostMapping("/coordinates")
  public String receiveInferenceResDto(@RequestBody InferenceResDto inferenceResDto) {
    String message = String.format("[Edge Gateway] 수신 완료! status=%s, coordinates=%s",
        inferenceResDto.status().toString(), inferenceResDto.coordinatesDto().toString());
    log.info("                                               {}", message);
    return message;
  }
}
