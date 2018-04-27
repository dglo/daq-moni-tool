package icecube.daq.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

abstract class ListData
    extends BaseData
{
    ListData(ChartTime time)
    {
        super(time);
    }

    abstract String getDataString();
    abstract int getNumEntries();

    StatParent createParent()
    {
        return new ListStat(getNumEntries());
    }

    abstract double getRawValue(int i);
}

class DoubleListData
    extends ListData
{
    private double[] vals;

    DoubleListData(ChartTime time, double[] vals)
    {
        super(time);

        this.vals = vals;
    }

    public String getDataString()
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) {
                buf.append(' ');
            }

            buf.append(vals[i]);
        }
        return buf.toString();
    }

    double getEntry(int i)
    {
        return vals[i];
    }

    int getNumEntries()
    {
        return vals.length;
    }

    double getRawValue(int i)
    {
        return vals[i];
    }

    boolean isEmpty()
    {
        return vals == null || (vals.length == 1 && vals[0] == 0.0);
    }
}

class LongListData
    extends ListData
{
    private long[] vals;

    LongListData(ChartTime time, long[] vals)
    {
        super(time);

        this.vals = vals;
    }

    public String getDataString()
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) {
                buf.append(' ');
            }

            buf.append(vals[i]);
        }
        return buf.toString();
    }

    long getEntry(int i)
    {
        return vals[i];
    }

    int getNumEntries()
    {
        return vals.length;
    }

    double getRawValue(int i)
    {
        return (double) vals[i];
    }

    boolean isEmpty()
    {
        if (vals == null || vals.length == 0) {
            return true;
        }

        boolean allZero = true;
        for (int i = 0; i < vals.length; i++) {
            if (vals[0] != 0L) {
                allZero = false;
                break;
            }
        }

        return allZero;
    }
}

class StringListData
    extends ListData
{
    private String[] vals;

    StringListData(ChartTime time, String[] vals)
    {
        super(time);

        this.vals = vals;
    }

    public String getDataString()
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) {
                buf.append(' ');
            }

            buf.append(vals[i]);
        }
        return buf.toString();
    }

    String getEntry(int i)
    {
        return vals[i];
    }

    int getNumEntries()
    {
        return vals.length;
    }

    double getRawValue(int i)
    {
        return 0.0;
    }

    boolean isEmpty()
    {
        return vals == null || (vals.length == 1 && vals[0] == null);
    }
}

class ListStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s+([^\\s:]+):?\\s+\\[(.*)\\]\\s*$");

    private int numEntries;

    ListStat(int numEntries)
    {
        this.numEntries = numEntries;
    }

    void add(BaseData data)
    {
        ListData sData = (ListData) data;
        if (sData.getNumEntries() != numEntries) {
            throw new Error("Expected " + numEntries + " entries, not " +
                            sData.getNumEntries());
        }

        super.add(data);
    }

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof ListData)) {
            throw new ClassCastException("Expected ListData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries[] series = new TimeSeries[numEntries];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "List " + i, Second.class);
            coll.addSeries(series[i]);
        }

        for (BaseData bd : iterator()) {
            ListData data = (ListData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numEntries; i++) {
                series[i].add(seconds, data.getRawValue(i));
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries[] series = new TimeSeries[numEntries];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "List " + i, Second.class);
            coll.addSeries(series[i]);
        }

        double[] prevVal = new double[numEntries];
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            ListData data = (ListData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    System.err.println("Cannot extract seconds from " + data);
                    continue;
                }

                for (int i = 0; i < numEntries; i++) {
                    series[i].add(seconds, data.getRawValue(i) - prevVal[i]);
                }
            }

            for (int i = 0; i < numEntries; i++) {
                prevVal[i] = data.getRawValue(i);
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries[] series = new TimeSeries[numEntries];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "List " + i, Second.class);
            coll.addSeries(series[i]);
        }

        double minVal = Double.POSITIVE_INFINITY;
        double maxVal = Double.NEGATIVE_INFINITY;

        for (BaseData bd : iterator()) {
            ListData data = (ListData) bd;

            for (int i = 0; i < numEntries; i++) {
                double entry = data.getRawValue(i);

                if (entry < minVal) {
                    minVal = entry;
                }
                if (entry > maxVal) {
                    maxVal = entry;
                }
            }
        }

        double div = maxVal - minVal;

        for (BaseData bd : iterator()) {
            ListData data = (ListData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numEntries; i++) {
                double val = (data.getRawValue(i) - minVal) / div;

                series[i].add(seconds, val);
            }
        }

        return coll;
    }

    private static double[] getDoubleArray(String line, String[] valStrs)
        throws StatParseException
    {
        double[] vals = new double[valStrs.length];
        for (int i = 0; i < vals.length; i++) {
            try {
                vals[i] = Double.parseDouble(valStrs[i]);
            } catch (NumberFormatException nfe) {
                throw new StatParseException("Bad double entry #" + i + " \"" +
                                             valStrs[i] + "\" in \"" + line +
                                             "\"");
            }
        }

        return vals;
    }

    private static long[] getLongArray(String line, String[] valStrs)
        throws StatParseException
    {
        long[] vals = new long[valStrs.length];
        for (int i = 0; i < vals.length; i++) {
            try {
                vals[i] = Long.parseLong(valStrs[i]);
            } catch (NumberFormatException nfe) {
                throw new StatParseException("Bad long entry #" + i + " \"" +
                                             valStrs[i] + "\" in \"" + line +
                                             "\"");
            }
        }

        return vals;
    }

    public static final boolean save(StatData statData, String sectionHost,
                                     String sectionName, ChartTime time,
                                     String line, boolean ignore)
        throws StatParseException
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return false;
        }

        String name = matcher.group(1);

        if (time == null) {
            throw new StatParseException("Found " + name +
                                         " list data before time was set");
        }

        String[] valStrs = matcher.group(2).split(", ");

        // strip quote marks
        for (int i = 0; i < valStrs.length; i++) {
            if (valStrs[i].startsWith("'") && valStrs[i].endsWith("'")) {
                valStrs[i] = valStrs[i].substring(1, valStrs[i].length() - 1);
            }
            if (valStrs[i].endsWith("L")) {
                valStrs[i] = valStrs[i].substring(0, valStrs[i].length() - 1);
            }
        }

        ListData data;
        if (valStrs.length == 0) {
            data = new LongListData(time, new long[0]);
        } else {
            try {
                data = new LongListData(time, getLongArray(line, valStrs));
            } catch (StatParseException spe) {
                // must not be a long value
                data = null;
            }

            if (data == null) {
                try {
                    data = new DoubleListData(time, getDoubleArray(line,
                                                                   valStrs));
                } catch (StatParseException spe) {
                    // must be a string value
                    data = null;
                }
            }

            if (data == null) {
                data = new StringListData(time, valStrs);
                if (data == null) {
                    System.err.println("Cannot parse " + name + " list data");
                    return true;
                }
            }
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name, data);
        }

        return true;
    }

    public boolean showLegend()
    {
        return true;
    }
}
