# Spring Boot Starter 만들기

`@EnableStreamix`로 활성화되는 Spring Boot Starter를 만드는 방법입니다.

---

## 1. Spring Boot Auto-Configuration

### 개념

Spring Boot의 핵심 기능으로, 클래스패스에 있는 라이브러리를 기반으로 자동으로 Bean을 등록합니다.

`spring-boot-autoconfigure` 모듈이 이 기능을 담당합니다.

### 동작 원리

1. `@SpringBootApplication`에 포함된 `@EnableAutoConfiguration` 활성화
2. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일 읽기
3. 나열된 설정 클래스들을 조건부로 로드

---

## 2. 자동 설정 클래스 작성

### 기본 구조

```java
@AutoConfiguration
@EnableConfigurationProperties(StreamixProperties.class)
@ConditionalOnClass(StreamixClient.class)
public class StreamixAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public FileStoragePort fileStoragePort(StreamixProperties props) {
        return new LocalFileStorageAdapter(props.getStorage().getBasePath());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public UploadFileUseCase uploadFileUseCase(
            FileStoragePort storage,
            FileMetadataPort metadata) {
        return new FileUploadService(storage, metadata);
    }
}
```

### 주요 어노테이션

| 어노테이션 | 설명 |
|------------|------|
| `@AutoConfiguration` | 자동 설정 클래스임을 표시 (Spring Boot 2.7+) |
| `@EnableConfigurationProperties` | Properties 클래스 활성화 |
| `@ConditionalOnClass` | 특정 클래스가 있을 때만 설정 |
| `@ConditionalOnMissingBean` | 해당 Bean이 없을 때만 등록 |
| `@ConditionalOnProperty` | 특정 프로퍼티 값에 따라 설정 |

### 조건부 설정 예시

```java
@Configuration
@ConditionalOnProperty(
    prefix = "streamix.api", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true  // 기본값 true
)
public class StreamixWebConfiguration {
    
    @Bean
    public StreamixController streamixController(...) {
        return new StreamixController(...);
    }
}

@Configuration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@ConditionalOnProperty(prefix = "streamix.metadata", name = "type", havingValue = "jpa")
public class StreamixJpaConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public FileMetadataPort fileMetadataPort(FileMetadataRepository repository) {
        return new JpaFileMetadataAdapter(repository);
    }
}
```

---

## 3. @Enable 어노테이션 만들기

### 기본 방식: @Import 사용

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(StreamixAutoConfiguration.class)
public @interface EnableStreamix {
}
```

### 선택적 기능 활성화

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(StreamixConfigurationSelector.class)
public @interface EnableStreamix {
    
    boolean enableApi() default true;
    
    boolean enableThumbnail() default true;
}
```

```java
public class StreamixConfigurationSelector implements ImportSelector {
    
    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        Map<String, Object> attrs = metadata
            .getAnnotationAttributes(EnableStreamix.class.getName());
        
        List<String> imports = new ArrayList<>();
        imports.add(StreamixCoreConfiguration.class.getName());
        
        if ((boolean) attrs.get("enableApi")) {
            imports.add(StreamixWebConfiguration.class.getName());
        }
        
        if ((boolean) attrs.get("enableThumbnail")) {
            imports.add(StreamixThumbnailConfiguration.class.getName());
        }
        
        return imports.toArray(new String[0]);
    }
}
```

---

## 4. Properties 클래스

### @ConfigurationProperties 사용

```java
@ConfigurationProperties(prefix = "streamix")
public class StreamixProperties {
    
    private Storage storage = new Storage();
    private Thumbnail thumbnail = new Thumbnail();
    private Api api = new Api();
    
    public static class Storage {
        private String type = "local";
        private String basePath = "./data/streamix";
        
        // getters, setters
    }
    
    public static class Thumbnail {
        private boolean enabled = true;
        private int width = 320;
        private int height = 180;
        
        // getters, setters
    }
    
    public static class Api {
        private boolean enabled = true;
        private String basePath = "/api/streamix";
        
        // getters, setters
    }
    
    // getters, setters
}
```

### Record 사용 (Spring Boot 3.0+)

```java
@ConfigurationProperties(prefix = "streamix")
public record StreamixProperties(
    Storage storage,
    Thumbnail thumbnail,
    Api api
) {
    public StreamixProperties {
        storage = storage != null ? storage : new Storage("local", "./data");
        thumbnail = thumbnail != null ? thumbnail : new Thumbnail(true, 320, 180);
        api = api != null ? api : new Api(true, "/api/streamix");
    }
    
    public record Storage(String type, String basePath) {}
    public record Thumbnail(boolean enabled, int width, int height) {}
    public record Api(boolean enabled, String basePath) {}
}
```

---

## 5. AutoConfiguration.imports 파일

### 위치

```
src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 내용

```
io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixAutoConfiguration
```

### 여러 설정 클래스 등록

```
io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixAutoConfiguration
io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixWebConfiguration
io.github.junhyeong9812.streamix.starter.autoconfigure.StreamixJpaConfiguration
```

---

## 6. Configuration Processor

### 역할

- IDE에서 application.yml 자동완성 지원
- `spring-configuration-metadata.json` 생성

### 의존성 추가

```groovy
annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
```

### 생성 결과

빌드 시 `META-INF/spring-configuration-metadata.json` 자동 생성:

```json
{
  "properties": [
    {
      "name": "streamix.storage.type",
      "type": "java.lang.String",
      "defaultValue": "local"
    },
    {
      "name": "streamix.storage.base-path",
      "type": "java.lang.String",
      "defaultValue": "./data/streamix"
    }
  ]
}
```

---

## 7. 전체 구조 정리

```
streamix-spring-boot-starter/
├── src/main/java/.../starter/
│   ├── annotation/
│   │   └── EnableStreamix.java
│   │
│   ├── autoconfigure/
│   │   ├── StreamixAutoConfiguration.java
│   │   ├── StreamixWebConfiguration.java
│   │   └── StreamixJpaConfiguration.java
│   │
│   └── properties/
│       └── StreamixProperties.java
│
└── src/main/resources/
    └── META-INF/
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 8. 참고 자료

- [Spring Boot - Creating Your Own Auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)
- [Spring Boot - @Conditional](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.condition-annotations)
- [Baeldung - Create a Custom Auto-Configuration](https://www.baeldung.com/spring-boot-custom-auto-configuration)