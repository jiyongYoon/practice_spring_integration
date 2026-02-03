package yoon.jy.practice.repository;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class IdMemoryRepository {

  private final AtomicInteger traceId = new AtomicInteger(0);

  public Integer generateTraceId() {
    return traceId.incrementAndGet();
  }
}
