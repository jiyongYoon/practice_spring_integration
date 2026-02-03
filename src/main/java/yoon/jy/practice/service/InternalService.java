package yoon.jy.practice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yoon.jy.practice.model.RobotMessage;
import yoon.jy.practice.repository.LogRepository;

@Service
@RequiredArgsConstructor
public class InternalService {

  private final LogRepository logRepository;

  public List<RobotMessage> findAllLogs(String robotId) {
    return logRepository.findAllByRobotId(robotId);
  }

}
