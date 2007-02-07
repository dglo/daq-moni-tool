package icecube.daq.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;

public class DumpMoni
{
    private ArrayList fileList = new ArrayList();

    DumpMoni(String[] args)
    {
        processArgs(args);

        StatData statData = new StatData();

        Iterator fileIter = fileList.iterator();
        while (fileIter.hasNext()) {
            File f = (File) fileIter.next();

            try {
                statData.addData(new GraphSource(f));
            } catch (IOException ioe) {
                System.err.println("Couldn't load \"" + f + "\":");
                ioe.printStackTrace();
                continue;
            }

            System.out.println(f + ":");
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
        Iterator sIter = data.getSections().iterator();
        while (sIter.hasNext()) {
            String section = (String) sIter.next();

            Iterator nIter = data.getSectionNames(section).iterator();
            while (nIter.hasNext()) {
                String name = (String) nIter.next();

                StatParent stats = data.getStatistics(section, name);
                out.println(section + " " + name + " " + stats);
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
