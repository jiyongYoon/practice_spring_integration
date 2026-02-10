package yoon.jy.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yoon.jy.practice.dto.InferenceResDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiModelDummyService {

  private final RestTemplate restTemplate;

  public InferenceResDto inference() {
    log.info("[Internal] === Inference Pipe === 추론 서버에 추론을 요청합니다...");
    return restTemplate.getForEntity("http://localhost:8080/ai/coordinates", InferenceResDto.class).getBody();
  }

}
