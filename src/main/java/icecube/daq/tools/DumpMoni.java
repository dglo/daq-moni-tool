package icecube.daq.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class DumpMoni
{
    private ArrayList<File> fileList = new ArrayList<File>();

    DumpMoni(String[] args)
    {
        processArgs(args);

        StatData statData = new StatData();

        for (File f : fileList) {
            System.out.println(f + ":");
            statData.loadFile(f, false, false);
        }

        dump(statData, System.out);
    }

    private boolean addFile(String fileName)
    {
        File f = new File(fileName);
        if (!f.exists()) {
            return false;
        }

        fileList.add(f);
        return true;
    }

    private void dump(StatData data, PrintStream out)
    {
        for (SectionKey key : data.getSectionKeys()) {
            for (String name : data.getSectionNames(key)) {
                StatParent stats = data.getStatistics(key, name);
                out.println(key + " " + name + " " + stats);
            }
        }
    }

    private void processArgs(String[] args)
    {
        boolean usage = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].length() > 1 && args[i].charAt(0) == '-') {
                System.err.println("Unknown option '" + args[i] + "'");
                usage = true;
            } else if (!addFile(args[i])) {
                System.err.println("Bad file '" + args[i] + "'");
                usage = true;
            }
        }

        if (fileList.size() == 0) {
            System.err.println("No files specified!");
            usage = true;
        }

        if (usage) {
            System.err.println("java " + getClass().getName() +
                               " file [file ...]" +
                               "");
            System.exit(1);
        }
    }

    public static final void main(String[] args)
    {
        new DumpMoni(args);
    }
}
