package com.matthewdeanmartin.pathkeeper.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleIntervalTest {

    @Test void startup() {
        var i = ScheduleInterval.parse("startup");
        assertThat(i.kind()).isEqualTo(ScheduleInterval.Kind.STARTUP);
        assertThat(i.toString()).isEqualTo("startup");
    }

    @Test void daily() {
        var i = ScheduleInterval.parse("daily");
        assertThat(i.kind()).isEqualTo(ScheduleInterval.Kind.DAILY);
        assertThat(i.toString()).isEqualTo("daily");
    }

    @ParameterizedTest @CsvSource({"30m,MINUTE,30", "2h,HOUR,2", "60m,MINUTE,60"})
    void minuteAndHour(String raw, ScheduleInterval.Kind kind, int value) {
        var i = ScheduleInterval.parse(raw);
        assertThat(i.kind()).isEqualTo(kind);
        assertThat(i.value()).isEqualTo(value);
    }

    @Test void minutesToSeconds() {
        var i = ScheduleInterval.parse("30m");
        assertThat(i.minutes()).isEqualTo(30);
        assertThat(i.seconds()).isEqualTo(1800);
    }

    @Test void hoursToMinutes() {
        var i = ScheduleInterval.parse("2h");
        assertThat(i.minutes()).isEqualTo(120);
    }

    @Test void nullParsesAsStartup() {
        assertThat(ScheduleInterval.parse(null).kind()).isEqualTo(ScheduleInterval.Kind.STARTUP);
    }

    @Test void invalidThrows() {
        assertThatThrownBy(() -> ScheduleInterval.parse("bogus"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
