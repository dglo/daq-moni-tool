package icecube.daq.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

class GraphSource
{
    private File file;
    private URL url;

    GraphSource(File file)
    {
        this.file = file;
    }

    GraphSource(URL url)
    {
        this.url = url;
    }

    BufferedReader getReader()
        throws IOException
    {
        Reader rdr;
        if (file == null) {
            rdr = new InputStreamReader(url.openStream());
        } else if (!file.getName().endsWith(".gz")) {
            rdr = new FileReader(file);
        } else {
            GZIPInputStream gin =
                new GZIPInputStream(new FileInputStream(file));
            rdr = new InputStreamReader(gin);
        }

        return new BufferedReader(rdr);
    }

    public String toString()
    {
        if (file != null) {
            return file.getName();
        }

        return url.getFile();
    }
}
