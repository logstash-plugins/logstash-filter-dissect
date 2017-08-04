package org.logstash.dissect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.logstash.Event;
import org.logstash.dissect.fields.InvalidFieldException;
import org.logstash.ext.JrubyEventExtLibrary;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JavaDissectorLibrary implements Library {

    private static final Logger LOGGER = LogManager.getLogger(Dissector.class);

    @Override
    final public void load(final Ruby runtime, final boolean wrap) throws IOException {
        final RubyModule module = runtime.defineModule("LogStash");

        final RubyClass clazz = runtime.defineClassUnder("Dissector", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(final Ruby runtime, final RubyClass rubyClass) {
                return new JavaDissectorLibrary.RubyDissect(runtime, rubyClass);
            }
        }, module);
        clazz.defineAnnotatedMethods(JavaDissectorLibrary.RubyDissect.class);

        final RubyClass runtimeError = runtime.getRuntimeError();
        module.defineClassUnder("FieldFormatError", runtimeError, runtimeError.getAllocator());
    }

    private static class NativeExceptions {
        public static NativeException newFieldFormatError(final Ruby ruby, final Throwable cause) {
            final RubyClass errorClass = ruby.getModule("LogStash").getClass("FieldFormatError");
            return new NativeException(ruby, errorClass, cause);
        }
    }

    public static class RubyDissect extends RubyObject {
        private final Map<RubyString, Dissector> dissectors = new HashMap<>();
        private DynamicMethod matchesMetricMethod;
        private DynamicMethod failuresMetricMethod;
        private boolean run_matched;
        private RubyHash conversions;
        private RubyObject plugin;
        private IRubyObject decorator;
        private IRubyObject metric;

        public RubyDissect(final Ruby runtime, final RubyClass klass) {
            super(runtime, klass);
            run_matched = false;
        }

        public RubyDissect(final Ruby runtime) {
            this(runtime, runtime.getModule("LogStash").getClass("Dissector"));
        }

        // def initialize(mapping, convert?, decorate?)
        @JRubyMethod(name = "initialize", required = 2, optional = 2)
        public IRubyObject ruby_initialize(final ThreadContext ctx, final IRubyObject[] args) {
            final RubyHash maps = (RubyHash) args[0];
            this.plugin = (RubyObject) args[1];
            this.conversions = (RubyHash)args[2];
            this.run_matched = args[3] == null || args[2].isTrue();
            matchesMetricMethod = getMethod(plugin, "increment_matches_metric");
            failuresMetricMethod = getMethod(plugin, "increment_failures_metric");

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> iter = maps.entrySet().iterator();
            while ( iter.hasNext() ) {
                Map.Entry entry = iter.next();
                final RubyString k = RubyString.newUTF8String(ctx.runtime, entry.getKey().toString());
                final String v = entry.getValue().toString();
                try {
                    if(!v.isEmpty()) {
                        final Dissector d = Dissector.create(v);
                        dissectors.put(k, d);
                    }
                } catch  (final InvalidFieldException e) {
                    throw new RaiseException(e, JavaDissectorLibrary.NativeExceptions.newFieldFormatError(ctx.runtime, e));
                }
            }
            return ctx.nil;
        }

        // def dissect(event, self)
        @JRubyMethod(name = "dissect", required = 1)
        public final IRubyObject dissect(final ThreadContext ctx, final IRubyObject arg1) {
            final JrubyEventExtLibrary.RubyEvent rubyEvent = (JrubyEventExtLibrary.RubyEvent) arg1;
            if (rubyEvent.ruby_cancelled(ctx).isTrue()) {
                return ctx.nil;
            }
            final Event event = rubyEvent.getEvent();
            final Map<String, Object> map = new HashMap<>(2);
            map.put("event", event.getData());
            try {
                LOGGER.debug("Event before dissection", buildLoggerEventMap(event));
                // there can be multiple dissect patterns, any success is a positive metric
                for (final Map.Entry<RubyString, Dissector> entry : dissectors.entrySet()) {
                    final RubyString key = entry.getKey();
                    if (event.includes(key.toString())) {
                        // use ruby event here because we want the bytelist bytes
                        // from the ruby extract without converting to Java
                        final RubyString src = rubyEvent.ruby_get_field(ctx, key).asString();
                        if (src.isEmpty()) {
                            map.put("key", key.toString());
                            LOGGER.warn("Dissector mapping, key found in event but it was empty", map);
                            invoke_failure_tags_and_metric(ctx, event);
                        } else {
                            final int result = entry.getValue().dissect(src.getBytes(), event);
                            // a good result will be the end of the source string
                            if (result == src.strLength()) {
                                invoke_matches_metric(ctx);
                            } else {
                                LOGGER.warn("Dissector mapping, key found in event but it was empty", map);
                                invoke_failure_tags_and_metric(ctx, event);
                            }
                        }
                    } else {
                        map.put("key", key.toString());
                        invoke_failures_metric(ctx);
                        LOGGER.warn("Dissector mapping, key not found in event", map);
                    }
                }
                if (!conversions.isEmpty()) {
                    invoke_conversions(ctx, event);
                }
                if (run_matched) {
                    invoke_filter_matched(ctx, getMethod(plugin, "filter_matched"), plugin, rubyEvent);
                }
                LOGGER.debug("Event after dissection", buildLoggerEventMap(event));
            } catch (final Exception ex) {
                invoke_failure_tags_and_metric(ctx, event);
                logException(ex);
            }
            return ctx.nil;
        }

        // def dissect_multi(events, self)
        @JRubyMethod(name = "dissect_multi", required = 1)
        public final IRubyObject dissectMulti(final ThreadContext ctx, final IRubyObject arg1) {
            final RubyArray events = (RubyArray) arg1;
            for (final IRubyObject event : events.toJavaArray()) {
                dissect(ctx, event);
            }
            return ctx.nil;
        }

        @JRubyMethod(name = "with_decorator", required = 1)
        public final IRubyObject withDecorator(final ThreadContext ctx, final IRubyObject decorator) {
            this.decorator = decorator;
            return this;
        }

        @JRubyMethod(name = "with_metric", required = 1)
        public final IRubyObject withMetric(final ThreadContext ctx, final IRubyObject metric) {
            this.metric = metric;
            return this;
        }

        private DynamicMethod getMethod(final RubyObject target, final String name) {
            return target.getMetaClass().searchMethod(name);
        }

        private IRubyObject invoke_conversions(final ThreadContext ctx, final Event javaEvent) {

            @SuppressWarnings("unchecked")
            final Iterator<Map.Entry> iter = conversions.entrySet().iterator();
            while ( iter.hasNext() ) {
                Map.Entry entry = iter.next();
                final String src = entry.getKey().toString();
                final String newType = entry.getValue().toString();
                try {
                    Converters.select(newType).convert(javaEvent, src);
                } catch (final NumberFormatException e) {
                    final Object val = javaEvent.getField(src);
                    if (val == null) {
                        javaEvent.tag(String.format("_dataconversionnullvalue_%s_%s", src, newType));
                    } else {
                        javaEvent.tag(String.format("_dataconversionuncoercible_%s_%s", src, newType));
                    }
                    final String msg = String.format(
                            "Dissector datatype conversion, value cannot be coerced, key: %s, value: %s",
                            src,
                            String.valueOf(val)
                    );
                    LOGGER.warn(msg);
                } catch (final IllegalArgumentException e) {
                    javaEvent.tag(String.format("_dataconversionmissing_%s_%s", src, newType));
                    LOGGER.warn("Dissector datatype conversion, datatype not supported: " + newType);
                }
            }
            return ctx.nil;
        }

        private IRubyObject invoke_filter_matched(final ThreadContext ctx, final DynamicMethod m, final RubyObject plugin, final JrubyEventExtLibrary.RubyEvent event) {
            if (!m.isUndefined()) {
                return m.call(ctx, plugin, plugin.getMetaClass(), "filter_matched", new IRubyObject[]{event});
            }
            return ctx.nil;
        }

        private IRubyObject invoke_failure_tags_and_metric(final ThreadContext ctx, final Event event) {
            invoke_failures_metric(ctx);
            invoke_failure_tags(ctx, event);
            return ctx.nil;
        }

        private IRubyObject invoke_failure_tags(final ThreadContext ctx, final Event event) {

            final DynamicMethod m = getMethod(plugin, "tag_on_failure");
            if (m.isUndefined()) {
                return ctx.nil;
            }
            final IRubyObject obj = m.call(ctx, plugin, plugin.getMetaClass(), "tag_on_failure");
            if (obj instanceof RubyArray) {
                final RubyArray tags = (RubyArray) obj;
                for (final IRubyObject t : tags.toJavaArray()) {
                    event.tag(t.toString());
                }
            }
            return ctx.nil;
        }

        private IRubyObject invoke_matches_metric(final ThreadContext ctx) {
            if (!matchesMetricMethod.isUndefined()) {
                return matchesMetricMethod.call(ctx, plugin, plugin.getMetaClass(), "increment_matches_metric");
            }
            return ctx.nil;
        }

        private IRubyObject invoke_failures_metric(final ThreadContext ctx) {
            if (!failuresMetricMethod.isUndefined()) {
                return failuresMetricMethod.call(ctx, plugin, plugin.getMetaClass(), "increment_failures_metric");
            }
            return ctx.nil;
        }

        private void logException(final Throwable ex) {
            final Map<String, Object> map = new HashMap<>(2);
            map.put("exception", ex.toString());
            map.put("backtrace", String.join("\n   ", Arrays.stream(ex.getStackTrace()).limit(12).map(StackTraceElement::toString).toArray(String[]::new)));
            LOGGER.error("Dissect threw an exception", map);
        }

        private Map<String, Map<String, Object>> buildLoggerEventMap(final Event event) {
            final Map<String, Map<String, Object>> map = new HashMap<>(1);
            map.put("event", event.getData());
            return map;
        }
    }
}
