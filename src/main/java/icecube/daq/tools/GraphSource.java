package icecube.daq.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;

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
        if (file != null) {
            return new BufferedReader(new FileReader(file));
        }

        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    public String toString()
    {
        if (file != null) {
            return file.getName();
        }

        return url.getFile();
    }
}
