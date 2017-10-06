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
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.logstash.Event;
import org.logstash.dissect.fields.InvalidFieldException;
import org.logstash.ext.JrubyEventExtLibrary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JavaDissectorLibrary implements Library {

    private static final Logger LOGGER = LogManager.getLogger(Dissector.class);

    @Override
    public final void load(final Ruby runtime, final boolean wrap) {
        final RubyModule module = runtime.defineModule("LogStash");

        final RubyClass clazz = runtime.defineClassUnder("Dissector", runtime.getObject(), JavaDissectorLibrary.RubyDissect::new, module);
        clazz.defineAnnotatedMethods(JavaDissectorLibrary.RubyDissect.class);

        final RubyClass runtimeError = runtime.getRuntimeError();
        module.defineClassUnder("FieldFormatError", runtimeError, runtimeError.getAllocator());
        module.defineClassUnder("ConvertDatatypeFormatError", runtimeError, runtimeError.getAllocator());
    }

    private static final class NativeExceptions {
        static NativeException newFieldFormatError(final Ruby ruby, final Throwable cause) {
            final RubyClass errorClass = ruby.getModule("LogStash").getClass("FieldFormatError");
            return new NativeException(ruby, errorClass, cause);
        }
    }

    public static class RubyDissect extends RubyObject {
        private static final long serialVersionUID = -4417443116118527316L;

        static final String FILTER_MATCHED = "filter_matched";
        static final String INCREMENT_MATCHES_METRIC = "increment_matches_metric";
        static final String INCREMENT_FAILURES_METRIC = "increment_failures_metric";
        static final String TAG_ON_FAILURE = "tag_on_failure";
        static final String[] EMPTY_STRINGS_ARRAY = new String[0];

        private DissectPair[] dissectors;
        private ConvertPair[] conversions;
        private RubyObject plugin;
        private RubyClass pluginMetaClass;
        private boolean runMatched;
        private String[] failureTags;

        public RubyDissect(final Ruby runtime, final RubyClass klass) {
            super(runtime, klass);
            dissectors = DissectPair.EMPTY_ARRAY;
            conversions = ConvertPair.EMPTY_ARRAY;
            runMatched = false;
            failureTags = EMPTY_STRINGS_ARRAY;
        }

        public RubyDissect(final Ruby runtime) {
            this(runtime, runtime.getModule("LogStash").getClass("Dissector"));
        }

        private static Map<String, Map<String, Object>> buildLoggerEventMap(final Event event) {
            final Map<String, Map<String, Object>> map = new HashMap<>(1);
            map.put("event", event.getData());
            return map;
        }

        // def initialize(mapping, plugin, convert?, decorate?)
        @JRubyMethod(name = "initialize", required = 2, optional = 2)
        public IRubyObject rubyInitialize(final ThreadContext ctx, final IRubyObject[] args) {
            Ruby ruby = ctx.runtime;
            try {
                dissectors = DissectPair.createArrayFromHash((RubyHash) args[0]);
            } catch (final InvalidFieldException e) {
                throw new RaiseException(e, JavaDissectorLibrary.NativeExceptions.newFieldFormatError(ruby, e));
            }
            plugin = (RubyObject) args[1];
            pluginMetaClass = plugin.getMetaClass();
            conversions = ConvertPair.createArrayFromHash((RubyHash) args[2]);
            for (final ConvertPair convertPair : conversions) {
                if (convertPair.converter().isInvalid()) {
                    final RubyClass klass = ruby.getModule("LogStash").getClass("ConvertDatatypeFormatError");
                    final String errorMessage = String.format("Dissector datatype conversion, datatype not supported: %s", convertPair.type());
                    throw new RaiseException(ruby, klass, errorMessage, true);
                }
            }
            runMatched = args[3] == null || args[3].isTrue();
            failureTags = fetchFailureTags(ctx);
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
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Event before dissection", buildLoggerEventMap(event));
                }

                if (dissectors.length > 0) {
                    invokeDissection(ctx, rubyEvent, event);
                }
                if (conversions.length > 0) {
                    invokeConversions(event);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Event after dissection", buildLoggerEventMap(event));
                }
            } catch (final Exception ex) {
                invokeFailureTagsAndMetric(ctx, event);
                logException(ex);
            }
            return ctx.nil;
        }

        // def dissect_multi(events, self)
        @JRubyMethod(name = "dissect_multi", required = 1)
        public final IRubyObject dissectMulti(final ThreadContext ctx, final IRubyObject arg1) {
            final RubyArray events = (RubyArray) arg1;
            Arrays.stream(events.toJavaArray()).forEach(event -> dissect(ctx, event));
            return ctx.nil;
        }

        private void invokeDissection(final ThreadContext ctx, final JrubyEventExtLibrary.RubyEvent rubyEvent, final Event event) {
            // this Map is used for logging
            final Map<String, Object> map = new HashMap<>(2);
            map.put("event", (Map)event.getData());
            // as there can be multiple dissect patterns, any success is a positive metric
            for (final DissectPair dissectPair : dissectors) {
                if (dissectPair.isEmpty()) {
                    continue;
                }
                map.put("key", dissectPair.javaKey());
                if (!event.includes(dissectPair.javaKey())) {
                    invokeFailuresMetric(ctx);
                    LOGGER.warn("Dissector mapping, key not found in event", map);
                    continue;
                }
                // use ruby event here because we want the bytelist bytes
                // from the ruby extract without converting to Java
                final RubyString src = rubyEvent.ruby_get_field(ctx, dissectPair.key()).asString();
                if (src.isEmpty()) {
                    LOGGER.warn("Dissector mapping, key found in event but it was empty", map);
                    invokeFailureTagsAndMetric(ctx, event);
                    continue;
                }
                final int result = dissectPair.dissector().dissect(src.getBytes(), event);
                // a good result will be the end of the source string
                if (result == src.strLength()) {
                    if (runMatched) {
                        invokeFilterMatched(ctx, rubyEvent);
                    }
                    invokeMatchesMetric(ctx);
                } else {
                    LOGGER.warn("Dissector mapping, key found in event but it was empty", map);
                    invokeFailureTagsAndMetric(ctx, event);
                }
            }
        }

        private void invokeConversions(final Event event) {
            for (final ConvertPair convertPair : conversions) {
                if (!convertPair.converter().isInvalid()) {
                    try {
                        convertPair.converter().convert(event, convertPair.src());
                    } catch (final NumberFormatException e) {
                        final Object val = event.getField(convertPair.src());
                        if (val == null) {
                            event.tag(String.format("_dataconversionnullvalue_%s_%s", convertPair.src(), convertPair.type()));
                        } else {
                            event.tag(String.format("_dataconversionuncoercible_%s_%s", convertPair.src(), convertPair.type()));
                        }
                        final String msg = String.format(
                                "Dissector datatype conversion, value cannot be coerced, key: %s, value: %s",
                                convertPair.src(),
                                String.valueOf(val)
                        );
                        LOGGER.warn(msg);
                    }
                }
            }
        }

        private void invokeFilterMatched(final ThreadContext ctx, final IRubyObject rubyEvent) {
            final DynamicMethod method = DynamicMethodCache.get(pluginMetaClass, FILTER_MATCHED);
            if (!method.isUndefined()) {
                method.call(ctx, plugin, pluginMetaClass, FILTER_MATCHED, new IRubyObject[]{rubyEvent});
            }
        }

        private void invokeFailureTagsAndMetric(final ThreadContext ctx, final Event event) {
            invokeFailuresMetric(ctx);
            invokeFailureTags(event);
        }

        private String[] fetchFailureTags(final ThreadContext ctx) {
            final DynamicMethod method = DynamicMethodCache.get(pluginMetaClass, TAG_ON_FAILURE);
            String[] result = EMPTY_STRINGS_ARRAY;
            if (!method.isUndefined()) {
                final IRubyObject obj = method.call(ctx, plugin, pluginMetaClass, TAG_ON_FAILURE);
                if (obj instanceof RubyArray) {
                    final RubyArray tags = (RubyArray) obj;
                    result = new String[tags.size()];
                    for(int idx = 0; idx < result.length; idx++) {
                        result[idx] = tags.entry(idx).asJavaString();
                    }
                }
            }
            return result;
        }

        private void invokeFailureTags(final Event event) {
            for (final String tag : failureTags) {
                event.tag(tag);
            }
        }

        private void invokeMatchesMetric(final ThreadContext ctx) {
            final DynamicMethod method = DynamicMethodCache.get(pluginMetaClass, INCREMENT_MATCHES_METRIC);
            if (!method.isUndefined()) {
                method.call(ctx, plugin, pluginMetaClass, INCREMENT_MATCHES_METRIC);
            }
        }

        private void invokeFailuresMetric(final ThreadContext ctx) {
            final DynamicMethod method = DynamicMethodCache.get(pluginMetaClass, INCREMENT_FAILURES_METRIC);
            if (!method.isUndefined()) {
                method.call(ctx, plugin, pluginMetaClass, INCREMENT_FAILURES_METRIC);
            }
        }

        private void logException(final Throwable exc) {
            final Map<String, Object> map = new HashMap<>(2);
            map.put("exception", exc.toString());
            map.put("backtrace", String.join("\n   ", Arrays.stream(exc.getStackTrace()).limit(12L).map(StackTraceElement::toString).toArray(String[]::new)));
            LOGGER.error("Dissect threw an exception", map);
        }
    }
}
