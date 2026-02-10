package yoon.jy.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import yoon.jy.practice.dto.CoordinatesDto;
import yoon.jy.practice.dto.InferenceResDto;
import yoon.jy.practice.dto.InferenceType;

@Service
@RequiredArgsConstructor
@Slf4j
public class EdgeGatewayDummyService {

  private final RestTemplate restTemplate;

  public void sendInferenceData(InferenceResDto inferenceResDto) {
    log.info("[Internal] === Inference Pipe === 추론 완료 데이터를 Edge Gateway로 전송합니다.");

    String response = restTemplate.postForObject("http://localhost:8080/edge/coordinates", inferenceResDto, String.class);
    log.info("[Internal] === Inference Pipe === 추론 완료 데이터 전달 완료. EdgeGateway's response: {}", response);
  }

}
