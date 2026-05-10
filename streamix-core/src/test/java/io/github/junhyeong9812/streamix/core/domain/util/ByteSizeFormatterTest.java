package io.github.junhyeong9812.streamix.core.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ByteSizeFormatter 테스트")
class ByteSizeFormatterTest {

  @Nested
  @DisplayName("format 테스트")
  class FormatTest {

    @Test
    @DisplayName("음수는 0 B를 반환한다")
    void negativeIsZero() {
      assertThat(ByteSizeFormatter.format(-1L)).isEqualTo("0 B");
      assertThat(ByteSizeFormatter.format(-1024L)).isEqualTo("0 B");
    }

    @Test
    @DisplayName("바이트 단위로 표시한다")
    void bytes() {
      assertThat(ByteSizeFormatter.format(0L)).isEqualTo("0 B");
      assertThat(ByteSizeFormatter.format(1L)).isEqualTo("1 B");
      assertThat(ByteSizeFormatter.format(500L)).isEqualTo("500 B");
      assertThat(ByteSizeFormatter.format(1023L)).isEqualTo("1023 B");
    }

    @Test
    @DisplayName("KB 단위로 표시한다")
    void kilobytes() {
      assertThat(ByteSizeFormatter.format(1024L)).isEqualTo("1.0 KB");
      assertThat(ByteSizeFormatter.format(2048L)).isEqualTo("2.0 KB");
      assertThat(ByteSizeFormatter.format(1024L * 1023)).isEqualTo("1023.0 KB");
    }

    @Test
    @DisplayName("MB 단위로 표시한다")
    void megabytes() {
      assertThat(ByteSizeFormatter.format(1572864L)).isEqualTo("1.5 MB");
      assertThat(ByteSizeFormatter.format(15_728_640L)).isEqualTo("15.0 MB");
    }

    @Test
    @DisplayName("GB 단위로 표시한다")
    void gigabytes() {
      assertThat(ByteSizeFormatter.format(2_147_483_648L)).isEqualTo("2.0 GB");
    }

    @Test
    @DisplayName("TB 단위로 표시한다")
    void terabytes() {
      assertThat(ByteSizeFormatter.format(1_099_511_627_776L)).isEqualTo("1.0 TB");
      assertThat(ByteSizeFormatter.format(2L * 1_099_511_627_776L)).isEqualTo("2.0 TB");
    }
  }

  @Nested
  @DisplayName("인스턴스화 차단 테스트")
  class InstantiationTest {

    @Test
    @DisplayName("Reflection으로도 인스턴스 생성 불가")
    void cannotInstantiate() throws NoSuchMethodException {
      Constructor<ByteSizeFormatter> c = ByteSizeFormatter.class.getDeclaredConstructor();
      c.setAccessible(true);
      assertThatThrownBy(c::newInstance)
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(AssertionError.class);
    }
  }
}
