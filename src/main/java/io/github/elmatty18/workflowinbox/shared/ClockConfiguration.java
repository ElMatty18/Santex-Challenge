package io.github.elmatty18.workflowinbox.shared;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of "now", so time-dependent code can be tested with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

	/**
	 * Ticks in microseconds, the precision of the timestamp columns, so a value returned right
	 * after a change is identical to the one read back from the database later.
	 */
	@Bean
	Clock clock() {
		return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
	}

}
