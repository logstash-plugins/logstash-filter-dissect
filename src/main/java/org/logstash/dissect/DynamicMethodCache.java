package org.logstash.dissect;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DynamicMethodCache {
    private static final int DEFAULT_SIZE = 4;
    private static final Map<String, DynamicMethod> DYNAMIC_METHOD_MAP = new ConcurrentHashMap<>(DEFAULT_SIZE);

    private DynamicMethodCache() {
    }

    static DynamicMethod get(final RubyClass rubyPlugin, final String key) {
        return DYNAMIC_METHOD_MAP.computeIfAbsent(key,
                rubyPlugin::searchMethod);
    }
}
