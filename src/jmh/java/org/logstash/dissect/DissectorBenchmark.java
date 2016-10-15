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
@Warmup(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Threads(2)
@Fork(2)
public class DissectorBenchmark {
    @Benchmark
    public void aWarmupVLong(Blackhole bh) {
        bh.consume(
                DissectVLongDelims.DISSECTOR.dissect(DissectVLongDelims.SRC.getBytes(), DissectMap.map())
        );
    }

    @Benchmark
    public void dOneDelim(Blackhole bh) {
        bh.consume(
                DissectOneDelim.DISSECTOR.dissect(DissectOneDelim.SRC.getBytes(), DissectMap.map())
        );
    }

    @Benchmark
    public void eTwoDelim(Blackhole bh) {
        bh.consume(
                DissectTwoDelims.DISSECTOR.dissect(DissectTwoDelims.SRC.getBytes(), DissectMap.map())
        );
    }

    @Benchmark
    public void cLongDelims(Blackhole bh) {
        bh.consume(
                DissectLongDelims.DISSECTOR.dissect(DissectLongDelims.SRC.getBytes(), DissectMap.map())
        );
    }

    @Benchmark
    public void bVLongDelims(Blackhole bh) {
        bh.consume(
                DissectVLongDelims.DISSECTOR.dissect(DissectVLongDelims.SRC.getBytes(), DissectMap.map())
        );
    }


    @State(Scope.Thread)
    public static class DissectOneDelim {
        public static final String SRC = Source.buildSrc(Source.delims1, 10);
        public static final Dissector DISSECTOR = Dissector.create(Source.buildMpp(Source.delims1, 10));
    }

    @State(Scope.Thread)
    public static class DissectTwoDelims {
        public static final String SRC = Source.buildSrc(Source.delims2, 10);
        public static final Dissector DISSECTOR = Dissector.create(Source.buildMpp(Source.delims2, 10));
    }

    @State(Scope.Thread)
    public static class DissectLongDelims {
        public static final String SRC = Source.buildSrc(Source.delims3, 10);
        public static final Dissector DISSECTOR = Dissector.create(Source.buildMpp(Source.delims3, 10));
    }

    @State(Scope.Thread)
    public static class DissectVLongDelims {
        public static final String SRC = Source.buildSrc(Source.delimsX, 10);
        public static final Dissector DISSECTOR = Dissector.create(Source.buildMpp(Source.delimsX, 10));
    }

    @State(Scope.Thread)
    public static class DissectMap {
        public static Map<String, Object> map() {
            return new HashMap<>();
        }
    }

    private static class Source {
        private static String src = "QQQQQwwwww‰‰‰‰‰rrrrrtttttYYYYYuuuuuIIIIIøøøøøpppppåååååsssssdddddfffffgggggHHHHHjjjjjkkkkkLLLLLzzzzz";
        private static String mpp = "%{a}%{b}%{c}%{d}%{e}%{f}%{g}%{h}%{i}%{j}";

        public static String[] delims1 = new String[]{" ", ".", ",", "/", "?", "|", "!", "$"};
        public static String[] delims2 = new String[]{" *", ". ", ",.", "/,", "?/", "|?", "!|", "$!"};
        public static String[] delims3 = new String[]{" *^", ". *", ",. ", "/,.", "?/,", "|?/", "!|?", "$!|"};
        public static String[] delimsX = new String[]{" *..........^", ". ..........*", ",........... ", "/,...........", "?/..........,", "|?........../", "!|..........?", "$!..........|"};

        public static String buildSrc(String[] delims, int count) {
            int d = delims.length;
            StringBuilder sb = new StringBuilder();
            int k = 0;
            for(int i = 0; i < count; i++) {
                k = (i * 5) % src.length();
                sb.append(src.substring(k, k + 5));
                sb.append(delims[i % d]);
            }
            sb.append("MMMMM");
            return sb.toString();
        }

        public static String buildMpp(String[] delims, int count) {
            int d = delims.length;
            StringBuilder sb = new StringBuilder();
            int k = 0;
            for(int i = 0; i < count; i++) {
                k = (i * 4) % mpp.length();
                sb.append(mpp.substring(k, k + 4));
                sb.append(delims[i % d]);
            }
            sb.append("%{k}");
            return sb.toString();
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