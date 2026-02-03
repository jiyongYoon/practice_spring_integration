package yoon.jy.practice.repository;

import java.util.List;
import yoon.jy.practice.model.RobotMessage;

public interface LogRepository {

  void save(RobotMessage robotMessage);
  List<RobotMessage> findAllByRobotId(String robotId);

}
