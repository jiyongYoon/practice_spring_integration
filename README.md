# Spring Integration 학습 정리

[Edge Gateway] <-> [Integration Server] <-> [AI Inference Server] 간의 통신을 효율적으로 주고받으며, 실시간성과 로그성 데이터를 분리하여 처리 및 확장하려는 의도로 Spring Integration을 활용하기 위해 학습한 예제입니다.

---

## 목차
1. [Spring Integration 소개](#1-spring-integration-소개)
2. [핵심 구성요소와 개념](#2-핵심-구성요소와-개념)
3. [현재 프로젝트 구성 분석](#3-현재-프로젝트-구성-분석)
4. [장점과 잠재적 문제점 및 해결방안](#4-장점과-잠재적-문제점-및-해결방안)

---

## 1. Spring Integration 소개

Spring Integration은 **메시지 기반의 엔터프라이즈 통합 패턴(EIP)**을 구현한 프레임워크입니다. 서로 다른 시스템이나 컴포넌트 간의 통신을 **느슨하게 결합(loosely coupled)**된 방식으로 처리할 수 있도록 지원합니다.

### 주요 특징
- **비동기 메시지 처리**: 요청과 응답을 분리하여 시스템 응답성 향상
- **확장 가능한 아키텍처**: 메시지 채널과 핸들러를 독립적으로 확장 가능
- **표준화된 패턴**: 엔터프라이즈 통합 패턴(EIP)을 Spring 방식으로 구현
- **선언적 구성**: 어노테이션 기반으로 간단하게 메시지 흐름 정의

### 사용 사례
- 마이크로서비스 간 통신
- 이벤트 기반 아키텍처
- 배치 처리 시스템
- 로봇/IoT 디바이스 제어 시스템 (본 프로젝트)

---

## 2. 핵심 구성요소와 개념

### 2.1 Message (메시지)
데이터를 담는 컨테이너입니다.
```java
Message<RobotMessage> message = MessageBuilder
    .withPayload(robotMessage)
    .build();
```
- **Payload**: 실제 전송할 데이터
- **Headers**: 메타데이터 (메시지 ID, 타임스탬프 등)

### 2.2 Channel (채널)
메시지가 흐르는 **파이프**입니다. 송신자와 수신자를 분리합니다.

#### DirectChannel (동기 채널)
- 메시지를 보내는 스레드가 직접 처리
- 즉시 실행, 빠른 응답 필요 시 사용
```java
MessageChannel channel = new DirectChannel();
```

#### QueueChannel (비동기 채널)
- 메시지를 큐에 쌓아두고 별도 스레드가 처리
- 버퍼링 가능, 처리 속도 조절 가능
```java
MessageChannel channel = new QueueChannel(100); // 버퍼 크기 100
```

#### ExecutorChannel (비동기 채널)
- 메시지 도착 즉시 스레드풀에서 처리
- 이벤트 기반 처리, Poller 불필요
```java
MessageChannel channel = new ExecutorChannel(executor);
```

### 2.3 Handler (핸들러)
채널에서 메시지를 받아 실제 비즈니스 로직을 실행하는 컴포넌트입니다.

#### @ServiceActivator
가장 기본적인 핸들러 타입으로, 메시지를 받아 처리합니다.
```java
@ServiceActivator(inputChannel = "myChannel")
public void handle(Message<Data> message) {
    // 비즈니스 로직
}
```

#### @Router
메시지 타입이나 조건에 따라 다른 채널로 분기합니다.
```java
@Router(inputChannel = "inputChannel")
public String route(Message<RobotMessage> message) {
    return message.getPayload().getType() == CONTROL 
        ? "controlChannel" 
        : "logChannel";
}
```

### 2.4 Poller (폴러)
**QueueChannel**에서 메시지를 꺼내는 방식과 주기를 정의합니다.

#### 주요 설정
```java
@Poller(
    fixedDelay = "100",          // 이전 작업 완료 후 100ms 대기하고 다음 polling
    maxMessagesPerPoll = "10",   // 한 번에 최대 10개 메시지 처리
    taskExecutor = "myExecutor"  // 사용할 스레드풀 지정
)
```

#### Poller의 동작 원리
- `ScheduledExecutorService`를 사용하여 주기적으로 큐 체크
- `Thread.sleep()`이 아닌 **OS 타이머 기반**으로 동작
- 대기 중에는 `TIMED_WAITING` 상태로 CPU 거의 사용 안 함 (< 1%)

#### fixedDelay vs fixedRate
```java
// fixedDelay: 이전 작업 완료 후 대기
@Poller(fixedDelay = "100")
// 처리(2초) → 대기(100ms) → 처리(2초) → 대기(100ms)

// fixedRate: 시작 시점 기준
@Poller(fixedRate = "100")
// 시작 → 100ms → 시작 → 100ms (처리 시간 무시)
// ⚠️ 처리가 느리면 스레드 고갈 위험!
```

### 2.5 TaskExecutor (스레드풀)
메시지 처리에 사용할 스레드를 관리합니다.

```java
@Bean
public Executor myExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);       // 기본 스레드 5개
    executor.setMaxPoolSize(10);       // 최대 10개까지 증가 가능
    executor.setQueueCapacity(100);    // 대기 큐 크기
    executor.setThreadNamePrefix("my-thread-");
    executor.initialize();
    return executor;
}
```

### 2.6 설정 우선순위
```
1순위: @Poller에 명시한 값
2순위: 기본 Poller 설정 (DEFAULT_POLLER)
3순위: Spring Integration 내장 기본값 (fixedDelay=10ms, maxMessagesPerPoll=1)
```

---

## 3. 현재 프로젝트 구성 분석

### 3.1 전체 아키텍처

```
[외부: Edge Gateway] 
         ↓ HTTP 요청
[InternalController]
         ↓
[RobotMessageSender] @Async
    ├─→ inferenceChannel (QueueChannel, 버퍼 50)
    │       ↓ Poller (10ms마다, 1개씩, inference-1 스레드)
    │   [InferenceHandler]
    │       ├─→ AI 서버 호출
    │       ├─→ Edge Gateway로 결과 전송
    │       └─→ 로그 저장 요청 (다시 logChannel로!)
    │
    └─→ logChannel (QueueChannel, 버퍼 100)
            ↓ Poller (기본 설정, log-db-1~3 스레드)
        [LogSaveHandler]
            └─→ DB 저장
```

### 3.2 IntegrationConfig 분석

```java
@Configuration
public class IntegrationConfig {

  // 추론용 채널 - QueueChannel 사용
  @Bean
  public MessageChannel inferenceChannel() {
    return new QueueChannel(50);  // 최대 50개 메시지 버퍼링
  }

  // 로그용 채널 - QueueChannel 사용
  @Bean
  public MessageChannel logChannel() {
    return new QueueChannel(100);  // 최대 100개 메시지 버퍼링
  }

  // 추론 처리용 스레드풀 - 단일 스레드로 순서 보장
  @Bean
  public Executor inferenceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);      // 스레드 1개
    executor.setMaxPoolSize(1);       // 최대 1개
    executor.setQueueCapacity(50);    // 내부 큐 50개
    executor.setThreadNamePrefix("inference-");
    executor.initialize();
    return executor;
  }

  // 로그 처리용 스레드풀 - 3개 스레드로 병렬 처리
  @Bean
  public Executor logExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);      // 동시에 3개 처리
    executor.setMaxPoolSize(3);
    executor.setThreadNamePrefix("log-db-");
    executor.initialize();
    return executor;
  }

  // 기본 Poller 설정 (명시하지 않은 Handler에 적용)
  @Bean(name = PollerMetadata.DEFAULT_POLLER)
  public PollerMetadata defaultPoller() {
    PollerMetadata poller = new PollerMetadata();
    poller.setMaxMessagesPerPoll(3);  // 한 번에 3개씩
    poller.setTrigger(new PeriodicTrigger(Duration.ofMillis(500)));  // 500ms마다
    return poller;
  }
}
```

#### 설계 의도
1. **inferenceChannel**: 제어 메시지는 순서가 중요하므로 단일 스레드로 순차 처리
2. **logChannel**: 로그는 순서가 덜 중요하므로 3개 스레드로 병렬 처리
3. **QueueChannel 사용**: 메시지 버퍼링으로 급격한 부하 완충
4. **기본 Poller**: 명시하지 않은 Handler도 동작 보장

### 3.3 InferenceHandler 분석

```java
@ServiceActivator(
    inputChannel = "inferenceChannel",
    poller = @Poller(
        fixedDelay = "10",               // 10ms마다 체크 (거의 실시간)
        maxMessagesPerPoll = "1",        // 순서 보장을 위해 1개씩
        taskExecutor = "inferenceExecutor"  // 단일 스레드 사용
    )
)
public void handleInference(Message<RobotMessage> message) {
    // 1. AI 추론 수행
    CoordinatesDto coordinatesDto = aiModelDummyService.inference();
    
    // 2. Edge Gateway로 결과 전송
    edgeGatewayDummyService.sendInferenceData(coordinatesDto);
    
    // 3. 로그 저장 요청 (체이닝 패턴)
    robotMessageSender.processMessage(RobotMessage.builder()
        .type(MessageType.LOG)
        .payload(coordinatesDto)
        .build());
}
```

#### 동작 흐름
```
1. Poller가 10ms마다 inferenceChannel 체크
2. 메시지 1개 발견 시 inference-1 스레드로 전달
3. AI 추론 → Edge Gateway 전송 → 로그 채널에 메시지 전송
4. 처리 완료 후 10ms 대기하고 다음 메시지 체크
```

#### 핵심 특징
- **fixedDelay=10ms**: 평균 5ms 지연으로 거의 실시간 처리
- **maxMessagesPerPoll=1**: 한 번에 1개만 꺼내서 순서 보장
- **단일 스레드**: 로봇 제어 명령의 순서 유지 (A → B → C 순서 보장)
- **체이닝 패턴**: 추론 결과를 다시 로그 채널로 전송하여 비동기 저장

### 3.4 LogSaveHandler 분석

```java
@ServiceActivator(
    inputChannel = "logChannel",
    poller = @Poller(taskExecutor = "logExecutor")
    // fixedDelay, maxMessagesPerPoll은 기본 설정 사용 (500ms, 3개)
)
public void handleInference(Message<RobotMessage> message) {
    log.info("[Internal] === Log Pipe === 데이터를 저장합니다...");
    logRepository.save(message.getPayload());
    log.info("[Internal] === Log Pipe === 데이터를 저장했습니다.");
}
```

#### 동작 흐름
```
1. Poller가 500ms마다 logChannel 체크 (기본 설정)
2. 메시지 최대 3개 발견 시 log-db-1~3 스레드로 병렬 처리
3. DB에 저장
4. 처리 완료 후 500ms 대기
```

#### 핵심 특징
- **기본 Poller 사용**: fixedDelay=500ms, maxMessagesPerPoll=3
- **3개 스레드**: 로그는 순서가 덜 중요하므로 병렬 처리로 성능 향상
- **느린 주기**: 로그는 실시간성이 덜 중요하므로 500ms 주기로 처리

### 3.5 메시지 흐름 상세 분석

#### 시나리오: 제어 명령 5개 연속 수신
```
0ms:    msg1 도착 → inferenceChannel (버퍼)
10ms:   Poller 체크 → msg1 꺼냄 → inference-1 처리 시작
10ms:   msg2 도착 → inferenceChannel (버퍼)
20ms:   msg3 도착 → inferenceChannel (버퍼)
...
2000ms: msg1 처리 완료 (AI 추론 + 전송)
        └─→ LOG 메시지 → logChannel
2010ms: Poller 체크 → msg2 꺼냄 → inference-1 처리 시작
4010ms: msg2 처리 완료
        └─→ LOG 메시지 → logChannel
...
```

**처리 순서 보장**: msg1 → msg2 → msg3 → msg4 → msg5 ✅

#### 스레드 상태
```
inference-1: RUNNABLE (AI 추론 중) 또는 TIMED_WAITING (대기)
log-db-1:    RUNNABLE (DB 저장 중) 또는 TIMED_WAITING (대기)
log-db-2:    RUNNABLE (DB 저장 중) 또는 TIMED_WAITING (대기)
log-db-3:    RUNNABLE (DB 저장 중) 또는 TIMED_WAITING (대기)
task-scheduler: TIMED_WAITING (CPU 거의 사용 안 함)
```

---

## 4. 장점과 잠재적 문제점 및 해결방안

### 4.1 현재 설계의 장점

#### ✅ 1. 비동기 처리로 응답성 향상
```java
@Async
public void processMessage(RobotMessage message) {
    inferenceChannel.send(message);  // 즉시 리턴
}
```
- Controller 스레드가 블로킹되지 않음
- HTTP 요청이 빠르게 응답 가능

#### ✅ 2. 메시지 버퍼링으로 부하 완충
```java
new QueueChannel(50);  // 최대 50개 버퍼링
```
- 급격한 요청 증가 시 큐에 쌓아두고 순차 처리
- 시스템 안정성 향상

#### ✅ 3. 순서 보장 (제어 명령)
```java
executor.setCorePoolSize(1);  // 단일 스레드
@Poller(maxMessagesPerPoll = "1")  // 1개씩 처리
```
- 로봇 제어 명령의 순서 유지 (A → B → C)
- 데이터 정합성 보장

#### ✅ 4. 효율적인 CPU 사용
- **Poller는 blocking 방식이 아님!**
- `ScheduledExecutorService` 기반으로 OS 타이머 활용
- 대기 중 CPU 사용률 < 1%
- 스레드는 `TIMED_WAITING` 상태로 대기

#### ✅ 5. 관심사 분리
- 추론 처리와 로그 저장이 독립적으로 동작
- 한쪽 장애가 다른 쪽에 영향 없음

### 4.2 잠재적 문제점

#### ⚠️ 1. 단일 스레드 병목 (가장 큰 문제)

**문제 상황:**
```
로봇 1대: 2초마다 1개 처리 → 처리량 30개/분
로봇 10대: 2초마다 1개 처리 → 처리량 30개/분 (동일!)
로봇 100대: 2초마다 1개 처리 → 처리량 30개/분 (동일!)

→ 로봇이 늘어나도 처리 속도가 늘어나지 않음!
→ 큐에 메시지가 계속 쌓임 → 지연 시간 증가
```

**예시:**
```
로봇 100대가 동시에 요청
→ inferenceChannel에 100개 메시지 대기
→ 단일 스레드가 2초씩 처리
→ 마지막 메시지는 200초 후에 처리! 😱
```

#### ⚠️ 2. Poller 주기로 인한 약간의 지연
```java
@Poller(fixedDelay = "10")  // 10ms 주기
```
- 평균 5ms, 최대 10ms 지연 발생
- 대부분의 경우 문제없지만, 초고속 응답이 필요하면 아쉬움

#### ⚠️ 3. 큐 오버플로우 가능성
```java
new QueueChannel(50);  // 최대 50개
```
- 50개 초과 시 메시지 손실 또는 Blocking
- 로봇이 많아지면 버퍼 크기 부족 가능

### 4.3 해결방안

#### 🎯 해결방안 1: Hash-based Partitioning (추천! ⭐⭐⭐⭐⭐)

**핵심 아이디어**: 로봇 ID를 해시해서 특정 파티션(채널)에 배정

```java
@Configuration
public class IntegrationConfig {
    
    private static final int PARTITION_COUNT = 5;  // 파티션 5개
    
    // 파티션별 채널 생성
    @Bean
    public List<QueueChannel> inferenceChannelPartitions() {
        List<QueueChannel> partitions = new ArrayList<>();
        for (int i = 0; i < PARTITION_COUNT; i++) {
            partitions.add(new QueueChannel(50));
        }
        return partitions;
    }
    
    // 스레드 풀도 5개로 증가
    @Bean
    public Executor inferenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // 5개 스레드
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("inference-");
        executor.initialize();
        return executor;
    }
}
```

```java
@Service
public class RobotMessageSender {
    
    private static final int PARTITION_COUNT = 5;
    private final List<QueueChannel> inferenceChannelPartitions;
    
    @Async
    public void processMessage(RobotMessage message) {
        if (message.getType() == CONTROL) {
            // 로봇 ID 해시로 파티션 결정
            int partition = Math.abs(message.getRobotId().hashCode()) % PARTITION_COUNT;
            QueueChannel targetChannel = inferenceChannelPartitions.get(partition);
            
            log.info("Robot {} → Partition {}", message.getRobotId(), partition);
            targetChannel.send(MessageBuilder.withPayload(message).build());
        }
    }
}
```

```java
@Component
public class InferenceHandler {
    
    // 각 파티션별 Handler 등록 (반복 코드 개선 가능)
    @ServiceActivator(
        inputChannel = "inferenceChannelPartitions[0]",
        poller = @Poller(fixedDelay = "10", maxMessagesPerPoll = "1", 
                         taskExecutor = "inferenceExecutor")
    )
    public void handlePartition0(Message<RobotMessage> message) {
        handleInference(message, 0);
    }
    
    @ServiceActivator(
        inputChannel = "inferenceChannelPartitions[1]",
        poller = @Poller(fixedDelay = "10", maxMessagesPerPoll = "1", 
                         taskExecutor = "inferenceExecutor")
    )
    public void handlePartition1(Message<RobotMessage> message) {
        handleInference(message, 1);
    }
    
    // ... 나머지 파티션도 동일
    
    private void handleInference(Message<RobotMessage> message, int partition) {
        log.info("[Partition {}] 처리: {}", partition, message.getPayload().getRobotId());
        // 기존 로직
    }
}
```

**효과:**
```
로봇 100대, 5개 파티션

Robot-A (hash % 5 = 0) → Partition 0 → inference-1 스레드
  └─ A의 모든 메시지는 순서 보장 ✅
  
Robot-B (hash % 5 = 2) → Partition 2 → inference-3 스레드
  └─ B의 모든 메시지는 순서 보장 ✅
  
Robot-C (hash % 5 = 0) → Partition 0 → inference-1 스레드
  └─ A와 C는 같은 파티션이지만 순차 처리로 순서 OK ✅

처리량: 30개/분 → 150개/분 (5배 증가!) 🚀
```

**장점:**
- ✅ 같은 로봇 메시지는 순서 보장
- ✅ 다른 로봇은 병렬 처리
- ✅ 로봇 수가 늘어나도 파티션 수는 고정 (메모리 효율적)
- ✅ 확장 용이 (파티션만 늘리면 됨)

**단점:**
- Handler 코드 중복 (Java DSL로 개선 가능)
- 파티션 불균형 가능 (해시 충돌)

---

#### 🎯 해결방안 2: ExecutorChannel로 전환 (이벤트 기반)

**지연 시간 최소화가 가장 중요한 경우**

```java
@Bean
public MessageChannel inferenceChannel() {
    // QueueChannel → ExecutorChannel로 변경
    return new ExecutorChannel(inferenceExecutor());
}

@Bean
public Executor inferenceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);  // 여전히 1개로 순서 보장 시도
    executor.setMaxPoolSize(1);
    executor.setThreadNamePrefix("inference-");
    executor.initialize();
    return executor;
}
```

```java
@Component
public class InferenceHandler {
    
    // Poller 불필요!
    @ServiceActivator(inputChannel = "inferenceChannel")
    public void handleInference(Message<RobotMessage> message) {
        // 메시지 도착 즉시 처리!
    }
}
```

**장점:**
- ✅ 지연 시간 0ms (메시지 도착 즉시 처리)
- ✅ Poller 오버헤드 없음
- ✅ 코드 단순

**단점:**
- ⚠️ 순서 보장이 약함 (스레드 1개여도 완벽하지 않음)
- ⚠️ 병목은 여전히 존재 (단일 스레드)

---

#### 🎯 해결방안 3: Blocking Queue Take (완벽한 이벤트 기반)

**지연 시간 0ms + 순서 보장 모두 필요한 경우**

```java
@Component
public class InferenceWorker implements InitializingBean {
    
    @Autowired
    private BlockingQueue<Message<?>> inferenceQueue;
    
    @Autowired
    private Executor inferenceExecutor;
    
    @Override
    public void afterPropertiesSet() {
        inferenceExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 블로킹 대기! (메시지 올 때까지 CPU 안 씀)
                    Message<?> message = inferenceQueue.take();
                    
                    // 즉시 처리!
                    handleInference(message);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}
```

**장점:**
- ✅ 지연 시간 0ms
- ✅ 완벽한 순서 보장
- ✅ CPU 오버헤드 없음 (대기 중 WAITING 상태)

**단점:**
- ⚠️ Spring Integration 표준 방식이 아님
- ⚠️ 코드 복잡도 증가
- ⚠️ 병목은 여전히 존재 (단일 스레드)

---

#### 🎯 해결방안 4: Poller 주기 단축 (간단한 개선)

**현재 10ms → 1ms로 단축**

```java
@Poller(
    fixedDelay = "1",  // 10ms → 1ms
    maxMessagesPerPoll = "1",
    taskExecutor = "inferenceExecutor"
)
```

**효과:**
- 평균 지연: 5ms → 0.5ms
- CPU 오버헤드: < 1% → ~2%

**장점:**
- ✅ 코드 변경 최소
- ✅ 지연 시간 크게 개선

**단점:**
- ⚠️ CPU 사용량 약간 증가
- ⚠️ 병목은 여전히 존재

---

### 4.4 권장 전략

#### 현재 상황 (로봇 1-10대)
```java
// 현재 설정 유지 (충분히 좋음)
@Poller(fixedDelay = "10", maxMessagesPerPoll = "1")
executor.setCorePoolSize(1);
```
- 처리량: 30개/분
- 충분히 빠르고 안정적

#### 로봇 10-50대
```java
// Poller 주기 단축
@Poller(fixedDelay = "1", maxMessagesPerPoll = "1")
executor.setCorePoolSize(1);
```
- 처리량: 30개/분 (동일)
- 응답성 10배 향상

#### 로봇 50-200대
```java
// Hash-based Partitioning (5개 파티션)
PARTITION_COUNT = 5;
executor.setCorePoolSize(5);
```
- 처리량: 150개/분
- 순서 보장 + 병렬 처리

#### 로봇 200대 이상
```java
// Hash-based Partitioning (10-20개 파티션)
PARTITION_COUNT = 20;
executor.setCorePoolSize(20);
```
- 처리량: 600개/분
- 확장 가능한 아키텍처

---

## 5. 참고 자료

### 성능 지표
```
현재 구성:
- 처리량: 30개/분 (단일 스레드)
- 평균 지연: 5ms (Poller 10ms)
- CPU 사용: < 1%
- 메모리: 안정적

Hash-based Partitioning (5개):
- 처리량: 150개/분 (5배)
- 평균 지연: 5ms (동일)
- CPU 사용: < 5%
- 메모리: 안정적

ExecutorChannel:
- 처리량: 30개/분 (동일)
- 평균 지연: 0.1ms (50배 빠름)
- CPU 사용: < 0.5%
- 순서 보장: 약함
```

### 트레이드오프 정리

| 방식 | 처리량 | 지연시간 | 순서보장 | 복잡도 | 추천도 |
|------|--------|----------|----------|--------|--------|
| 현재 (단일 스레드) | 낮음 | 5ms | ✅ | 낮음 | ⭐⭐⭐ |
| Poller 1ms | 낮음 | 0.5ms | ✅ | 낮음 | ⭐⭐⭐⭐ |
| Hash Partitioning | 높음 | 5ms | ✅ | 중간 | ⭐⭐⭐⭐⭐ |
| ExecutorChannel | 낮음 | 0.1ms | ⚠️ | 낮음 | ⭐⭐ |
| Blocking Take | 낮음 | 0.1ms | ✅ | 높음 | ⭐⭐⭐⭐ |

---

## 결론

현재 프로젝트는 Spring Integration의 핵심 개념을 잘 활용하여 **순서 보장**과 **비동기 처리**를 동시에 달성했습니다.

**현재 강점:**
- ✅ 깔끔한 아키텍처
- ✅ 순서 보장
- ✅ 효율적인 CPU 사용 (Poller는 blocking 방식이 아님!)
- ✅ 메시지 버퍼링으로 안정성

**향후 개선 방향:**
- 로봇 수가 증가하면 **Hash-based Partitioning** 도입
- 초저지연이 필요하면 Poller 주기를 1ms로 단축
- 완벽한 이벤트 기반이 필요하면 Blocking Queue 방식 고려

이 학습을 통해 Spring Integration의 강력함과 유연성을 이해할 수 있었고, 실제 프로덕션 환경에서 발생할 수 있는 병목과 해결 방안까지 고민해볼 수 있었습니다.