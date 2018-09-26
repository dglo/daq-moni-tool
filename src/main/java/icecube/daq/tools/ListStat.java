package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

class InternalListError
    extends Error
{
    InternalListError(String msg)
    {
        super(msg);
    }
}

abstract class ListData
    extends BaseData
{
    ListData(ChartTime time)
    {
        super(time);
    }

    @Override
    abstract String getDataString();

    abstract int getNumEntries();

    @Override
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

    @Override
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

    @Override
    int getNumEntries()
    {
        return vals.length;
    }

    @Override
    double getRawValue(int i)
    {
        return vals[i];
    }

    @Override
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

    @Override
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

    @Override
    int getNumEntries()
    {
        return vals.length;
    }

    @Override
    double getRawValue(int i)
    {
        return (double) vals[i];
    }

    @Override
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

    @Override
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

    @Override
    int getNumEntries()
    {
        return vals.length;
    }

    @Override
    double getRawValue(int i)
    {
        return 0.0;
    }

    @Override
    boolean isEmpty()
    {
        return vals == null || (vals.length == 1 && vals[0] == null);
    }
}

class ListParser
    extends BaseStatParser
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s+([^\\s:]+):?\\s+\\[(.*)\\]\\s*$");

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

    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        final String name = matcher.group(1);

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
                // data is not a long value
                data = null;
            }

            if (data == null) {
                try {
                    data = new DoubleListData(time, getDoubleArray(line,
                                                                   valStrs));
                } catch (StatParseException spe) {
                    // data is not a double value
                    data = null;
                }

                if (data == null) {
                    data = new StringListData(time, valStrs);
                }
            }
        }

        if (data == null) {
            return null;
        }

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class ListStat
    extends StatParent<ListData>
{
    private static final Logger LOG = Logger.getLogger(ListStat.class);

    private static final String[] LOADAVG_FIELDS = {
        "1 Minute", "5 Minute", "15 Minute",
    };

    private static final String[] MEMSTAT_FIELDS = {
        "Used", "Total",
    };

    private static final String[] EVTDATA_FIELDS = {
        "Run Number", "Events", "Ticks",
    };

    private int numEntries;

    private String[] fieldNames;

    ListStat(int numEntries)
    {
        this.numEntries = numEntries;
    }

    @Override
    void add(ListData data)
    {
        if (data.getNumEntries() != numEntries) {
            throw new InternalListError("Expected " + numEntries +
                                        " entries, not " +
                                        data.getNumEntries());
        }

        super.add(data);
    }

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String prefix = pargs.getSeriesPrefix(key, name);

        if (fieldNames == null) {
            if (name.startsWith("LoadAverage")) {
                fieldNames = LOADAVG_FIELDS;
            } else if (name.startsWith("MemoryStatistics")) {
                fieldNames = MEMSTAT_FIELDS;
            } else if (name.startsWith("EventData")) {
                fieldNames = EVTDATA_FIELDS;
            } else {
                // generate field names
                fieldNames = new String[numEntries];
                for (int idx = 0; idx < fieldNames.length; idx++) {
                    fieldNames[idx] = String.format(prefix + "List " + idx);
                }
            }
        } else if (fieldNames.length != numEntries) {
            // number of field names does not match number of entries
            final String errmsg = "Field name list for " + prefix +
                " should contain " + numEntries + " entries, not " +
                fieldNames.length;
            throw new StatPlotException(errmsg);
        }

        TimeSeries[] series = new TimeSeries[numEntries];
        for (int idx = 0; idx < series.length; idx++) {
            series[idx] = new TimeSeries(fieldNames[idx], Second.class);
        }

        return series;
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
        }

        for (BaseData bd : iterator()) {
            ListData data = (ListData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
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
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
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
                    LOG.error("Cannot extract seconds from " + data);
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
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
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
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numEntries; i++) {
                double val = (data.getRawValue(i) - minVal) / div;

                series[i].add(seconds, val);
            }
        }

        return coll;
    }

    @Override
    public boolean showLegend()
    {
        return true;
    }
}
