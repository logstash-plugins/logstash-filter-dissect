
import org.logstash.dissect.JrubyDissectLibrary;
import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

public class JrubyDissectService implements BasicLibraryService {
    public boolean basicLoad(final Ruby runtime)
            throws IOException
    {
        new JrubyDissectLibrary().load(runtime, false);
        return true;
    }
}
