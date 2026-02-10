package yoon.jy.practice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import yoon.jy.practice.entity.Action;

public interface ActionJpaRepository extends JpaRepository<Action, Long> {
  List<Action> findAllByRobotId(String robotId);
}
