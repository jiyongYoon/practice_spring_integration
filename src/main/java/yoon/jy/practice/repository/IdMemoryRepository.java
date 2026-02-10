package yoon.jy.practice.repository;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class IdMemoryRepository {

  private final AtomicLong traceId = new AtomicLong(0);

  public Long generateTraceId() {
    return traceId.incrementAndGet();
  }
}
