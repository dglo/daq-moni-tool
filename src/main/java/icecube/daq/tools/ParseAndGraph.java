package icecube.daq.tools;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class ParseAndGraph
{
    private ArrayList fileList = new ArrayList();
    private boolean saveToFile;
    private boolean showPoints;
    private ArrayList includeSection = new ArrayList();
    private ArrayList excludeSection = new ArrayList();

    ParseAndGraph(String[] args)
    {
        processArgs(args);

        if (saveToFile) {
            try {
                System.setProperty("java.awt.headless", "true");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
        }

        ChartChoices choices =
            new ChartChoices(statData, includeSection, excludeSection);
        choices.setShowPoints(showPoints);
        choices.setFilterBoring(true);

        ChartGenerator chartGen = new ChartGenerator(statData, choices);

        if (chartGen.isEmpty()) {
            System.err.println("No data found!");
        } else if (saveToFile) {
            BufferedImage img = chartGen.getImage();
            saveToFile(img);
        } else {
            ApplicationFrame appFrame = new ApplicationFrame("moni-graphs");
            appFrame.setContentPane(chartGen.layout());

            appFrame.pack();
            RefineryUtilities.centerFrameOnScreen(appFrame);
            appFrame.setVisible(true);
        }

        if (saveToFile) {
            System.exit(0);
        }
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

    private void processArgs(String[] args)
    {
        boolean usage = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].length() > 1 && args[i].charAt(0) == '-') {
                if (args[i].charAt(1) == 'i') {
                    i++;

                    if (i >= args.length) {
                        System.err.println("No argument found for" +
                                           " '-i(ncludeSection)'");
                        usage = true;
                        break;
                    }

                    includeSection.add(args[i]);
                } else if (args[i].charAt(1) == 'p') {
                    showPoints = true;
                } else if (args[i].charAt(1) == 's') {
                    saveToFile = true;
                } else if (args[i].charAt(1) == 'x') {
                    i++;

                    if (i >= args.length) {
                        System.err.println("No argument found for" +
                                           " '-x(cludeSection)'");
                        usage = true;
                        break;
                    }

                    excludeSection.add(args[i]);
                } else {
                    System.err.println("Unknown option '" + args[i] + "'");
                    usage = true;
                }
            } else if (!addFile(args[i])) {
                System.err.println("Bad file '" + args[i] + "'");
                usage = true;
            }
        }

        if (fileList.size() == 0) {
            System.err.println("No files specified!");
            usage = true;
        }

        if (includeSection.size() > 0 && excludeSection.size() > 0) {
            System.err.println("Cannot specify both -i(ncludeSection)" +
                               " and -x(cludeSection)");
            usage = true;
        }

        if (usage) {
            System.err.println("java " + getClass().getName() +
                               " -i(ncludeSection)" +
                               " -p(lotPoints)" +
                               " -s(aveToImageFile)" +
                               " -x(cludeSection)" +
                               " file [file ...]" +
                               "");
            System.exit(1);
        }
    }

    private void saveToFile(BufferedImage img)
    {
        final String imgType = "png";

        String name = "moni-charts." + imgType;

        try {
            ImageIO.write(img, imgType, new File(name));
            System.out.println("Saved to " + name);
        } catch (Exception ex) {
            System.err.println("Couldn't save to \"" + name + "\":");
            ex.printStackTrace();
        }
    }

    public static final void main(String[] args)
    {
        new ParseAndGraph(args);
    }
}
