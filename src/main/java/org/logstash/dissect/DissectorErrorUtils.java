package org.logstash.dissect;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyString;

public class DissectorErrorUtils {
    public static RubyString message(Ruby ruby, Throwable cause) {
        return RubyString.newString(ruby, cause.toString());
    }

    public static RubyArray backtrace(Ruby ruby, Throwable cause) {
        StackTraceElement[] elements = cause.getStackTrace();
        RubyArray arr = RubyArray.newArray(ruby, elements.length);
        // add first two lines anyway
        arr.add(RubyString.newString(ruby, elements[0].toString()));
        arr.add(RubyString.newString(ruby, elements[1].toString()));
        for (int i = 2; i < elements.length; i++) {
            String line = elements[i].toString();
            // only add lines referring to our code
            if (line.contains("logstash")) {
                arr.add(RubyString.newString(ruby, line));
            }
        }
        return arr;
    }
}
