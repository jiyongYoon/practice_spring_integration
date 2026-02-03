package yoon.jy.practice.config;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.support.PeriodicTrigger;

@Configuration
public class IntegrationConfig {

  // 추론용 채널 (이벤트 기반 - 빠른 응답 필요)
  @Bean
  public MessageChannel inferenceChannel() {
    return new QueueChannel(50);
  }

  // 로그 데이터용 채널 (비동기 - 천천히 처리해도 됨)
  @Bean
  public MessageChannel logChannel() {
    return new QueueChannel(100);
  }

  // 추론 처리용 스레드풀 - 제어 처리 순서 보장을 위해 싱글스레드로 처리
  @Bean
  public Executor inferenceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("inference-");
    executor.initialize();
    return executor;
  }

  // 로그 처리용 스레드풀
  @Bean
  public Executor logExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("log-db-");
    executor.initialize();
    return executor;
  }

  // 기본 Poller 설정
  @Bean(name = PollerMetadata.DEFAULT_POLLER)
  public PollerMetadata defaultPoller() {
    PollerMetadata poller = new PollerMetadata();
    poller.setMaxMessagesPerPoll(3);  // 기본: 10개
    poller.setTrigger(new PeriodicTrigger(Duration.ofMillis(1000)));  // 기본: 1초
    return poller;
  }
}
