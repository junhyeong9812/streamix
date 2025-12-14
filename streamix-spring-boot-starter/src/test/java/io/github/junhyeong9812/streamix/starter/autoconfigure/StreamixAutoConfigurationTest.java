package io.github.junhyeong9812.streamix.starter.autoconfigure;

import io.github.junhyeong9812.streamix.core.application.port.in.*;
import io.github.junhyeong9812.streamix.core.application.port.out.FileMetadataPort;
import io.github.junhyeong9812.streamix.core.application.port.out.FileStoragePort;
import io.github.junhyeong9812.streamix.core.adapter.out.metadata.InMemoryMetadataAdapter;
import io.github.junhyeong9812.streamix.starter.properties.StreamixProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StreamixAutoConfiguration 자동 설정 테스트
 */
@DisplayName("StreamixAutoConfiguration 테스트")
class StreamixAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(StreamixAutoConfiguration.class))
      // InMemory 메타데이터 어댑터 제공 (JPA 없이 테스트)
      .withBean(FileMetadataPort.class, InMemoryMetadataAdapter::new);

  @Test
  @DisplayName("기본 설정으로 핵심 Bean이 등록된다")
  void defaultConfiguration_RegistersCoreBeans() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(StreamixProperties.class);
      assertThat(context).hasSingleBean(UploadFileUseCase.class);
      assertThat(context).hasSingleBean(StreamFileUseCase.class);
      assertThat(context).hasSingleBean(FileStoragePort.class);
      assertThat(context).hasSingleBean(FileMetadataPort.class);
    });
  }

  @Test
  @DisplayName("커스텀 저장 경로가 Properties에 반영된다")
  void customStoragePath_ReflectedInProperties() {
    contextRunner
        .withPropertyValues("streamix.storage.base-path=/custom/path")
        .run(context -> {
          StreamixProperties props = context.getBean(StreamixProperties.class);
          assertThat(props.storage().basePath()).isEqualTo("/custom/path");
        });
  }

  @Test
  @DisplayName("API 경로가 Properties에 반영된다")
  void customApiPath_ReflectedInProperties() {
    contextRunner
        .withPropertyValues("streamix.api.base-path=/custom/api")
        .run(context -> {
          StreamixProperties props = context.getBean(StreamixProperties.class);
          assertThat(props.api().basePath()).isEqualTo("/custom/api");
        });
  }

  @Test
  @DisplayName("대시보드 경로가 Properties에 반영된다")
  void customDashboardPath_ReflectedInProperties() {
    contextRunner
        .withPropertyValues("streamix.dashboard.path=/admin")
        .run(context -> {
          StreamixProperties props = context.getBean(StreamixProperties.class);
          assertThat(props.dashboard().path()).isEqualTo("/admin");
        });
  }
}