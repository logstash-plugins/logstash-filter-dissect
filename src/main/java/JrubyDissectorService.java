import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;
import org.logstash.dissect.JavaDissectorLibrary;

import java.io.IOException;

public class JrubyDissectorService implements BasicLibraryService {
    @Override
    public boolean basicLoad(final Ruby runtime)
            throws IOException {
        new JavaDissectorLibrary().load(runtime, false);
        return true;
    }
}
