package yoon.jy.practice.external;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yoon.jy.practice.dto.CoordinatesDto;
import yoon.jy.practice.dto.InferenceResDto;
import yoon.jy.practice.dto.InferenceType;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiModelServerController {

  private final Random random = new Random();

  @GetMapping("/coordinates")
  public InferenceResDto inference() {
    log.info("                                               [AI] 추론 요청 수신!");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.warn("Thread sleep 중 인터럽트가 발생했습니다.");
      Thread.currentThread().interrupt();
    }
    log.info("                                               [AI] 추론이 완료되었습니다.");

    return InferenceResDto.builder()
        .status(InferenceType.DONE)
        .coordinatesDto(CoordinatesDto.builder()
          .x(getDouble())
          .y(getDouble())
          .z(getDouble())
          .rx(getDouble())
          .ry(getDouble())
          .rz(getDouble())
          .build())
        .build();
  }

  private String getDouble() {
    return String.valueOf(Math.round(random.nextDouble() * 100) / 100.0);
  }

}
