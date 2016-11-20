package br.com.kodeless.minimojs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Created by eduardo on 11/11/16.
 */
public interface ResourceLoader {
    InputStream get(String path) throws IOException;
    Set<String> getPaths(String path) throws IOException;
    String getRealPath(String path) throws IOException;
}
