package org.logstash.dissect;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 6, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Threads(1)
@Fork(2)
public class DissectorBenchmark {
    @Benchmark
    public void aDissector(Blackhole bh) {
        bh.consume(
                DissectState.DISSECTOR.dissect(DissectState.SRC.getBytes(), DissectState.map())
        );

    }

    @State(Scope.Thread)
    public static class DissectState {
        public static final String SRC = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        public static final String MPP = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{reason!} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{polrt!} %{polrt!} %{from_zone} %{to_zone} %{rest}";

        public static final Dissector DISSECTOR = new Dissector(MPP);

        public static Map<String, Object> map() {
            return new HashMap<String, Object>();
        }
    }

	/*
	 * It is better to run the benchmark from command-line instead of IDE.
	 *
	 * To run, in command-line: $ ./gradlew clean jmh
	 */

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(DissectorBenchmark.class.getSimpleName())
                .build();

        new Runner(options).run();
    }
}