# 헥사고날 아키텍처

Streamix에 적용된 헥사고날 아키텍처(Ports & Adapters)에 대한 설명입니다.

---

## 1. 헥사고날 아키텍처란?

Alistair Cockburn이 제안한 아키텍처 패턴으로, **Ports and Adapters** 패턴이라고도 합니다.

핵심 비즈니스 로직(도메인)을 외부 기술(DB, Web, 메시지 큐 등)로부터 분리하여 테스트와 유지보수를 용이하게 합니다.

### 전통적인 레이어드 아키텍처

```
Controller → Service → Repository → DB
    ↓           ↓          ↓
  (Web)    (Business)  (Database)
```

문제점:
- 상위 레이어가 하위 레이어에 직접 의존
- DB 변경 시 Service까지 영향
- 비즈니스 로직 테스트에 DB 필요

### 헥사고날 아키텍처

```
          ┌─────────────────────────────────────┐
          │           Application               │
          │  ┌─────────────────────────────┐    │
Driving   │  │         Domain              │    │  Driven
Adapters ─┼─▶│    (Business Logic)         │◀───┼─ Adapters
(Input)   │  │                             │    │  (Output)
          │  └─────────────────────────────┘    │
          │         ▲              ▲            │
          │    Input Ports    Output Ports      │
          └─────────────────────────────────────┘
```

특징:
- 도메인이 중심, 외부 기술은 어댑터로 연결
- 의존성이 항상 안쪽(도메인)을 향함
- 인터페이스(Port)를 통해 느슨한 결합

---

## 2. 핵심 개념

### Port (포트)

애플리케이션의 경계를 정의하는 **인터페이스**입니다.

**Input Port (Driving Port)**
- 외부에서 애플리케이션을 호출하는 인터페이스
- Use Case를 정의
- 예: `UploadFileUseCase`, `StreamFileUseCase`

**Output Port (Driven Port)**
- 애플리케이션이 외부 시스템을 호출하는 인터페이스
- SPI(Service Provider Interface) 역할
- 예: `FileStoragePort`, `FileMetadataPort`

### Adapter (어댑터)

Port 인터페이스의 **구현체**입니다.

**Driving Adapter (Primary Adapter)**
- 외부 요청을 받아 Input Port 호출
- 예: REST Controller, CLI, Message Consumer

**Driven Adapter (Secondary Adapter)**
- Output Port를 구현하여 외부 시스템과 통신
- 예: JPA Repository, S3 Client, Redis Client

---

## 3. Streamix 적용 구조

```
streamix-core/
├── domain/                    # 💎 도메인 (중심)
│   ├── model/
│   │   ├── FileMetadata.java
│   │   ├── FileType.java
│   │   └── StreamableFile.java
│   └── exception/
│
├── application/               # 🎯 애플리케이션
│   ├── port/
│   │   ├── in/               # Input Ports
│   │   │   ├── UploadFileUseCase.java
│   │   │   └── StreamFileUseCase.java
│   │   └── out/              # Output Ports
│   │       ├── FileStoragePort.java
│   │       └── FileMetadataPort.java
│   └── service/              # Use Case 구현
│       ├── FileUploadService.java
│       └── FileStreamService.java
│
└── adapter/out/              # 🔌 기본 Driven Adapters
    ├── storage/
    │   └── LocalFileStorageAdapter.java
    └── metadata/
        └── InMemoryMetadataAdapter.java

streamix-spring-boot-starter/
├── adapter/
│   ├── in/web/               # 🔌 Driving Adapter (Web)
│   │   └── StreamixController.java
│   └── out/persistence/      # 🔌 Driven Adapter (JPA)
│       └── JpaFileMetadataAdapter.java
```

---

## 4. 의존성 규칙

**의존성은 항상 안쪽(도메인)을 향해야 합니다.**

```
Adapter → Application → Domain
   ↓           ↓           ↓
(구현체)    (Use Case)   (모델)
```

### 올바른 의존성

```java
// ✅ Service가 Port(인터페이스)에 의존
public class FileUploadService implements UploadFileUseCase {
    private final FileStoragePort storage;      // 인터페이스
    private final FileMetadataPort metadata;    // 인터페이스
}

// ✅ Adapter가 Port를 구현
public class JpaFileMetadataAdapter implements FileMetadataPort {
    private final FileMetadataRepository repository;
}
```

### 잘못된 의존성

```java
// ❌ Service가 구현체에 직접 의존
public class FileUploadService {
    private final JpaFileMetadataAdapter adapter;  // 구현체 직접 참조
}

// ❌ Domain이 외부 기술에 의존
public class FileMetadata {
    @Entity  // JPA 어노테이션이 도메인에 있으면 안됨
}
```

---

## 5. 코드 예시

### Input Port 정의

```java
// application/port/in/UploadFileUseCase.java
public interface UploadFileUseCase {
    
    UploadResult upload(UploadCommand command);
    
    record UploadCommand(
        String originalName,
        String contentType,
        long size,
        InputStream inputStream
    ) {}
}
```

### Output Port 정의

```java
// application/port/out/FileStoragePort.java
public interface FileStoragePort {
    
    String save(String fileName, InputStream inputStream, long size);
    
    InputStream load(String storagePath);
    
    InputStream loadPartial(String storagePath, long start, long end);
    
    void delete(String storagePath);
}
```

### Use Case 구현

```java
// application/service/FileUploadService.java
public class FileUploadService implements UploadFileUseCase {
    
    private final FileStoragePort storage;
    private final FileMetadataPort metadataRepo;
    
    @Override
    public UploadResult upload(UploadCommand command) {
        // 1. 파일 저장
        String path = storage.save(
            generateFileName(command),
            command.inputStream(),
            command.size()
        );
        
        // 2. 메타데이터 저장
        FileMetadata metadata = createMetadata(command, path);
        metadataRepo.save(metadata);
        
        return new UploadResult(metadata);
    }
}
```

### Driven Adapter 구현

```java
// adapter/out/storage/LocalFileStorageAdapter.java
public class LocalFileStorageAdapter implements FileStoragePort {
    
    private final Path basePath;
    
    @Override
    public String save(String fileName, InputStream inputStream, long size) {
        Path filePath = basePath.resolve(fileName);
        Files.copy(inputStream, filePath);
        return filePath.toString();
    }
    
    @Override
    public InputStream load(String storagePath) {
        return Files.newInputStream(Path.of(storagePath));
    }
}
```

---

## 6. 장점

### 테스트 용이성

```java
// Port의 Mock 구현으로 단위 테스트 가능
@Test
void uploadFile_success() {
    // given
    FileStoragePort mockStorage = mock(FileStoragePort.class);
    FileMetadataPort mockMetadata = mock(FileMetadataPort.class);
    
    when(mockStorage.save(any(), any(), anyLong())).thenReturn("/path/file.jpg");
    
    FileUploadService service = new FileUploadService(mockStorage, mockMetadata);
    
    // when
    UploadResult result = service.upload(command);
    
    // then
    assertThat(result).isNotNull();
}
```

### 기술 교체 용이성

```java
// 로컬 저장소 → S3로 교체
// FileStoragePort 구현체만 교체하면 됨

// Before
@Bean
public FileStoragePort fileStorage() {
    return new LocalFileStorageAdapter(basePath);
}

// After
@Bean
public FileStoragePort fileStorage() {
    return new S3FileStorageAdapter(s3Client, bucketName);
}
```

---

## 7. 참고 자료

- [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Get Your Hands Dirty on Clean Architecture (Tom Hombergs)](https://github.com/thombergs/buckpal)
- [Netflix - Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)