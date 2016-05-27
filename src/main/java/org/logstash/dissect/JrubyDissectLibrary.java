package org.logstash.dissect;

import com.logstash.Event;
import com.logstash.ext.JrubyEventExtLibrary.RubyEvent;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import java.io.IOException;
import java.util.Map;


public class JrubyDissectLibrary implements Library {

    @Override
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule module = runtime.defineModule("LogStash");

        RubyClass clazz = runtime.defineClassUnder("Dissect", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass rubyClass) {
                return new RubyDissect(runtime, rubyClass);
            }
        }, module);

        clazz.defineAnnotatedMethods(RubyDissect.class);
    }

//    @JRubyClass(name = "Dissect", parent = "Object")
    public static class RubyDissect extends RubyObject {
        private Dissect dissector;
        private RubyEvent event;

        public RubyDissect(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
        }

        public RubyDissect(Ruby runtime) {
            this(runtime, runtime.getModule("LogStash").getClass("Dissect"));
        }

        // def initialize(event, source, mapping)
        @JRubyMethod(name = "initialize", required = 3)
        public IRubyObject ruby_initialize(ThreadContext ctx, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
//            args = Arity.scanArgs(ctx.runtime, args, 3, 0);
            event = (RubyEvent) arg1;
            String source = arg2.asJavaString();
            String mapping = arg3.asJavaString();
            dissector = new Dissect(source, mapping);
            return ctx.nil;
        }

        @JRubyMethod(name = "dissect")
        public IRubyObject dissect(ThreadContext ctx) {
            Event e = event.getEvent();
            Map<String, Object> map = e.toMap();
            dissector.dissect(map);
            e.append(new Event(map));
            return ctx.nil;
        }
    }

}
