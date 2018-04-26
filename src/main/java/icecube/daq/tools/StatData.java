package icecube.daq.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

class LongData
    extends BaseData
{
    private long val;

    LongData(ChartTime time, long val)
    {
        super(time);

        this.val = val;
    }

    String getDataString()
    {
        return Long.toString(val);
    }

    StatParent getParent()
    {
        return new LongStat();
    }

    long getValue()
    {
        return val;
    }

    boolean isEmpty()
    {
        return val == 0L;
    }
}

class LongStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile(
            "^(\\s+([^\\s:]+)|\\s*(.+)\\s*):?\\s+([\\-\\+]?\\d+)L?\\s*$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof LongData)) {
            throw new ClassCastException("Expected LongData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String seriesName = pargs.getName(key, name);

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            try {
                series.add(data.getTime().getSecond(), data.getValue());
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + data.getValue());
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        long prevVal = 0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                try {
                    series.add(data.getTime().getSecond(),
                               data.getValue() - prevVal);
                } catch (Exception ex) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + data.getValue());
                }
            }

            prevVal = data.getValue();
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        long minVal = Long.MAX_VALUE;
        long maxVal = Long.MIN_VALUE;

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            if (data.getValue() < minVal) {
                minVal = data.getValue();
            }
            if (data.getValue() > maxVal) {
                maxVal = data.getValue();
            }
        }

        double div = maxVal - minVal;

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            double val = ((double) (data.getValue() - minVal)) / div;
            try {
                series.add(data.getTime().getSecond(), val);
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + val);
            }
        }

        return coll;
    }

    public static final boolean save(StatData statData, String sectionName,
                                     ChartTime time, String line)
        throws StatParseException
    {
        return save(statData, null, sectionName, time, line, false);
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

        final long val;
        try {
            val = Long.parseLong(matcher.group(4));
        } catch (NumberFormatException nfe) {
            throw new StatParseException("Bad number \"" + matcher.group(4) +
                                         "\" in \"" + line + "\"");
        }

        String name = matcher.group(2);
        if (name == null) {
            name = matcher.group(3);
            if (name == null) {
                throw new StatParseException("No name found in \"" + line +
                                             "\"");
            }
        }

        if (time == null) {
            throw new StatParseException("Found " + name + " stat " + val +
                                         " before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new LongData(time, val));
        }

        return true;
    }
}

class DoubleData
    extends BaseData
{
    private double val;

    DoubleData(ChartTime time, double val)
    {
        super(time);

        this.val = val;
    }

    String getDataString()
    {
        return Double.toString(val);
    }

    StatParent getParent()
    {
        return new DoubleStat();
    }

    double getValue()
    {
        return val;
    }

    boolean isEmpty()
    {
        return val == 0.0;
    }
}

class DoubleStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile(
            "^(\\s+([^\\s:]+):?|\\s*(.+)\\s*:)\\s+([\\-\\+]?\\d+\\.?\\d*)\\s*$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof DoubleData)) {
            throw new ClassCastException("Expected DoubleData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String seriesName = pargs.getName(key, name);

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        for (BaseData bd : iterator()) {
            DoubleData data = (DoubleData) bd;

            try {
                series.add(data.getTime().getSecond(), data.getValue());
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + data.getValue());
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        double prevVal = 0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            DoubleData data = (DoubleData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                try {
                    series.add(data.getTime().getSecond(),
                               data.getValue() - prevVal);
                } catch (Exception ex) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + data.getValue());
                }
            }

            prevVal = data.getValue();
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries series = new TimeSeries(seriesName, Second.class);
        coll.addSeries(series);

        double minVal = Double.POSITIVE_INFINITY;
        double maxVal = Double.NEGATIVE_INFINITY;

        for (BaseData bd : iterator()) {
            DoubleData data = (DoubleData) bd;

            if (data.getValue() < minVal) {
                minVal = data.getValue();
            }
            if (data.getValue() > maxVal) {
                maxVal = data.getValue();
            }
        }

        double div = maxVal - minVal;

        for (BaseData bd : iterator()) {
            DoubleData data = (DoubleData) bd;

            double val = (data.getValue() - minVal) / div;

            try {
                series.add(data.getTime().getSecond(), val);
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + val);
            }
        }

        return coll;
    }

    public static final boolean save(StatData statData, String sectionName,
                                     ChartTime time, String line)
        throws StatParseException
    {
        return save(statData, null, sectionName, time, line, false);
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

        final double val;
        try {
            val = Double.parseDouble(matcher.group(4));
        } catch (NumberFormatException nfe) {
            throw new StatParseException("Bad number \"" + matcher.group(4) +
                                         "\" in \"" + line + "\"");
        }

        String name = matcher.group(2);
        if (name == null) {
            name = matcher.group(3);
            if (name == null) {
                throw new StatParseException("No name found in \"" + line +
                                             "\"");
            }
        }

        if (time == null) {
            throw new StatParseException("Found " + name + " stat " + val +
                                         " before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new DoubleData(time, val));
        }

        return true;
    }
}

class MemoryData
    extends BaseData
{
    private long usedMem;
    private long freeMem;

    MemoryData(ChartTime time, long[] vals)
    {
        this(time, vals[0], vals[1]);
    }

    MemoryData(ChartTime time, long usedMem, long freeMem)
    {
        super(time);

        this.usedMem = usedMem;
        this.freeMem = freeMem;
    }

    String getDataString()
    {
        return Long.toString(usedMem) + " used, " + freeMem + " free";
    }

    long getFreeMemory()
    {
        return freeMem;
    }

    StatParent getParent()
    {
        return new MemoryStat();
    }

    long getUsedMemory()
    {
        return usedMem;
    }

    boolean isEmpty()
    {
        return usedMem == 0L && freeMem == 0L;
    }
}

class MemoryStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^(\\s+([^\\s:]+):?|\\s*(.+)\\s*:)" +
                        "\\s+(\\d+)([KMG]?)\\s+used," +
                        "\\s+(\\d+)([KMG]?)\\s+of" +
                        "\\s+(\\d+)([KMG]?)\\s+free\\.$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof MemoryData)) {
            throw new ClassCastException("Expected MemoryData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries usedSeries = new TimeSeries(prefix + "Used", Second.class);
        coll.addSeries(usedSeries);

        TimeSeries freeSeries = new TimeSeries(prefix + "Free", Second.class);
        coll.addSeries(freeSeries);

        for (BaseData bd : iterator()) {
            MemoryData data = (MemoryData) bd;

            try {
                usedSeries.add(data.getTime().getSecond(),
                               data.getUsedMemory());
            } catch (Exception ex) {
                System.err.println("Series \"" + prefix +
                                   " Used\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " +
                                   data.getUsedMemory());
            }
            try {
                freeSeries.add(data.getTime().getSecond(),
                               data.getFreeMemory());
            } catch (Exception ex) {
                System.err.println("Series \"" + prefix +
                                   " Free\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " +
                                   data.getFreeMemory());
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

        TimeSeries usedSeries = new TimeSeries(prefix + "Used", Second.class);
        coll.addSeries(usedSeries);

        TimeSeries freeSeries = new TimeSeries(prefix + "Free", Second.class);
        coll.addSeries(freeSeries);

        long prevUsed = 0;
        long prevFree = 0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            MemoryData data = (MemoryData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                long val;

                val = data.getUsedMemory() - prevUsed;
                try {
                    usedSeries.add(data.getTime().getSecond(), val);
                } catch (Exception ex) {
                    System.err.println("Series \"" + prefix +
                                       " Used\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + val);
                }

                val = data.getFreeMemory() - prevFree;
                try {
                    freeSeries.add(data.getTime().getSecond(), val);
                } catch (Exception ex) {
                    System.err.println("Series \"" + prefix +
                                       " Free\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + val);
                }
            }

            prevUsed = data.getUsedMemory();
            prevFree = data.getFreeMemory();
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

        TimeSeries usedSeries = new TimeSeries(prefix + "Used", Second.class);
        coll.addSeries(usedSeries);

        TimeSeries freeSeries = new TimeSeries(prefix + "Free", Second.class);
        coll.addSeries(freeSeries);

        long minVal = Long.MAX_VALUE;
        long maxVal = Long.MIN_VALUE;

        for (BaseData bd : iterator()) {
            MemoryData data = (MemoryData) bd;

            if (data.getUsedMemory() < minVal) {
                minVal = data.getUsedMemory();
            }
            if (data.getUsedMemory() > maxVal) {
                maxVal = data.getUsedMemory();
            }
            if (data.getFreeMemory() < minVal) {
                minVal = data.getFreeMemory();
            }
            if (data.getFreeMemory() > maxVal) {
                maxVal = data.getFreeMemory();
            }
        }

        double div = maxVal - minVal;

        for (BaseData bd : iterator()) {
            MemoryData data = (MemoryData) bd;

            double used = ((double) (data.getUsedMemory() - minVal)) / div;
            try {
                usedSeries.add(data.getTime().getSecond(), used);
            } catch (Exception ex) {
                System.err.println("Series \"" + prefix +
                                   " Used\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + used);
            }

            double free = ((double) (data.getFreeMemory() - minVal)) / div;
            try {
                freeSeries.add(data.getTime().getSecond(), free);
            } catch (Exception ex) {
                System.err.println("Series \"" + prefix +
                                   " Free\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + free);
            }
        }

        return coll;
    }

    public static final boolean save(StatData statData, String sectionName,
                                     ChartTime time, String line)
        throws StatParseException
    {
        return save(statData, null, sectionName, time, line, false);
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

        String name = matcher.group(2);
        if (name == null) {
            name = matcher.group(3);
            if (name == null) {
                throw new StatParseException("No name found in \"" + line +
                                             "\"");
            }
        }

        long[] memVals = new long[3];
        for (int i = 0; i < memVals.length; i++) {
            final int offset = 4 + (i * 2);

            try {
                memVals[i] = Long.parseLong(matcher.group(offset));
            } catch (NumberFormatException nfe) {
                throw new StatParseException("Bad memory statistic \"" +
                                             matcher.group(offset) +
                                             "\" in \"" + line + "\"");
            }

            final String suffix = matcher.group(offset + 1);
            if (suffix != null && suffix.length() == 1) {
                switch (suffix.charAt(0)) {
                case 'K':
                    memVals[i] *= 1024L;
                    break;
                case 'M':
                    memVals[i] *= 1024L * 1024L;
                    break;
                case 'G':
                    memVals[i] *= 1024L * 1024L * 1024L;
                    break;
                default:
                    throw new Error("Unknown memory suffix '" + suffix + "'");
                }
            }
        }

        if (time == null) {
            throw new StatParseException("Found " + name +
                                         " stat before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new MemoryData(time, memVals));
        }

        return true;
    }

    public boolean showLegend()
    {
        return true;
    }
}

class StringData
    extends BaseData
{
    private String val;

    StringData(ChartTime time, String val)
    {
        super(time);

        this.val = val;
    }

    String getDataString()
    {
        return "\"" + val + "\"";
    }

    StatParent getParent()
    {
        return new StringStat();
    }

    String getValue()
    {
        return val;
    }

    boolean isEmpty()
    {
        return val == null || val.length() == 0;
    }
}

class StringStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s+([^\\s:]+):?\\s+(.*)\\s*$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof StringData)) {
            throw new ClassCastException("Expected StringData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        // do nothing
        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        // do nothing
        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        // do nothing
        return coll;
    }

    public static final boolean save(StatData statData, ChartTime time,
                                     String line)
        throws StatParseException
    {
        return save(statData, null, time, line);
    }

    public static final boolean save(StatData statData, String sectionName,
                                     ChartTime time, String line)
        throws StatParseException
    {
        return save(statData, null, sectionName, time, line, false);
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
        final String val = matcher.group(2);

        if (time == null) {
            throw new StatParseException("Found " + name + " stat " + val +
                                         " before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new StringData(time, val));
        }

        return true;
    }
}

class StrandData
    extends BaseData
{
    private long[] depths;

    StrandData(ChartTime time, long[] vals)
    {
        super(time);

        this.depths = vals;
    }

    public String getDataString()
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < depths.length; i++) {
            if (i > 0) {
                buf.append(' ');
            }

            buf.append(depths[i]);
        }
        return buf.toString();
    }

    int getNumStrands()
    {
        return depths.length;
    }

    StatParent getParent()
    {
        return new StrandStat(depths.length);
    }

    long getStrand(int i)
    {
        return depths[i];
    }

    boolean isEmpty()
    {
        return depths == null || depths.length == 0 ||
            (depths.length == 1 && depths[0] == 0);
    }
}

class StrandStat
    extends StatParent
{
    private int numStrands;

    StrandStat(int numStrands)
    {
        this.numStrands = numStrands;
    }

    void add(BaseData data)
    {
        StrandData sData = (StrandData) data;
        if (sData.getNumStrands() != numStrands) {
            throw new Error("Expected " + numStrands + " strands, not " +
                            sData.getNumStrands());
        }

        super.add(data);
    }

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof StrandData)) {
            throw new ClassCastException("Expected StrandData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries[] series = new TimeSeries[numStrands];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "Strand " + i, Second.class);
            coll.addSeries(series[i]);
        }

        for (BaseData bd : iterator()) {
            StrandData data = (StrandData) bd;

            for (int i = 0; i < numStrands; i++) {
                series[i].add(data.getTime().getSecond(), data.getStrand(i));
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

        TimeSeries[] series = new TimeSeries[numStrands];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "Strand " + i, Second.class);
            coll.addSeries(series[i]);
        }

        long[] prevVal = new long[numStrands];
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            StrandData data = (StrandData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                for (int i = 0; i < numStrands; i++) {
                    series[i].add(data.getTime().getSecond(),
                                  data.getStrand(i) - prevVal[i]);
                }
            }

            for (int i = 0; i < numStrands; i++) {
                prevVal[i] = data.getStrand(i);
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

        TimeSeries[] series = new TimeSeries[numStrands];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "Strand " + i, Second.class);
            coll.addSeries(series[i]);
        }

        long minVal = Long.MAX_VALUE;
        long maxVal = Long.MIN_VALUE;

        for (BaseData bd : iterator()) {
            StrandData data = (StrandData) bd;

            for (int i = 0; i < numStrands; i++) {
                long strand = data.getStrand(i);

                if (strand < minVal) {
                    minVal = strand;
                }
                if (strand > maxVal) {
                    maxVal = strand;
                }
            }
        }

        double div = maxVal - minVal;

        for (BaseData bd : iterator()) {
            StrandData data = (StrandData) bd;

            for (int i = 0; i < numStrands; i++) {
                double val = ((double) (data.getStrand(i) - minVal)) / div;

                series[i].add(data.getTime().getSecond(), val);
            }
        }

        return coll;
    }

    public static final boolean save(StatData statData, String sectionHost,
                                     String sectionName, ChartTime time,
                                     String line, boolean ignore)
        throws StatParseException
    {
        String[] flds = line.split("\\s+");
        if (flds == null || flds.length == 0) {
            return false;
        }

        long[] vals = new long[flds.length];
        for (int i = 0; i < vals.length; i++) {
            try {
                vals[i] = Long.parseLong(flds[i]);
            } catch (NumberFormatException nfe) {
                throw new StatParseException("Bad strand statistic #" + i +
                                             " \"" + flds[i] + "\" in \"" +
                                             line + "\"");
            }
        }

        final String name = "Strand Depths";

        if (time == null) {
            throw new StatParseException("Found " + name +
                                         " stat before time was set");
        }

        if (!ignore) {
            try {
                statData.add(sectionHost, sectionName, name,
                             new StrandData(time, vals));
            } catch (Error err) {
                System.err.println("Bad strands for " + sectionHost + "/" +
                                   sectionName + "/" + name + " " + time + ": " +
                                   err.getMessage());
            }
        }

        return true;
    }

    public boolean showLegend()
    {
        return true;
    }
}

class TriggerData
    extends BaseData
{
    private long val;
    private double dVal;

    TriggerData(ChartTime time, long val, double dVal)
    {
        super(time);

        this.val = val;
        this.dVal = dVal;
    }

    public String getDataString()
    {
        return Long.toString(val) + " / " + dVal;
    }

    double getDoubleValue()
    {
        return dVal;
    }

    StatParent getParent()
    {
        return new TriggerStat();
    }

    long getValue()
    {
        return val;
    }

    boolean isEmpty()
    {
        return val == 0L && dVal == 0.0;
    }
}

class TriggerStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^Trigger\\s+count:\\s+(\\S+)" +
                        "Trigger(\\d?\\d?)\\s+(\\d+)\\s+(\\d+\\.\\d+)\\s*$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof TriggerData)) {
            throw new ClassCastException("Expected TriggerData, not " +
                                         data.getClass().getName());
        }
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String seriesName = pargs.getName(key, name);

        TimeSeries valSeries = new TimeSeries(seriesName, Second.class);
        coll.addSeries(valSeries);

        TimeSeries trigSeries =
            new TimeSeries(name + " Trigger", Second.class);
        coll.addSeries(trigSeries);

        for (BaseData bd : iterator()) {
            TriggerData data = (TriggerData) bd;

            try {
                valSeries.add(data.getTime().getSecond(), data.getValue());
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + data.getValue());
            }

            try {
                trigSeries.add(data.getTime().getSecond(),
                               data.getDoubleValue());
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " +
                                   data.getDoubleValue());
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries valSeries = new TimeSeries(seriesName, Second.class);
        coll.addSeries(valSeries);

        TimeSeries trigSeries =
            new TimeSeries(name + " Trigger", Second.class);
        coll.addSeries(trigSeries);

        long prevVal = 0;
        double prevTrig = 0.0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            TriggerData data = (TriggerData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                long lVal =  data.getValue() - prevVal;
                try {
                    valSeries.add(data.getTime().getSecond(), lVal);
                } catch (Exception ex) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + lVal);
                }

                double dVal =  data.getDoubleValue() - prevTrig;
                try {
                    trigSeries.add(data.getTime().getSecond(), dVal);
                } catch (Exception ex) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains data at " +
                                       data.getTime().getSecond() +
                                       ", cannot add value " + dVal);
                }
            }

            prevVal = data.getValue();
            prevTrig = data.getDoubleValue();
        }

        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String seriesName = key + " " + name;

        TimeSeries valSeries = new TimeSeries(seriesName, Second.class);
        coll.addSeries(valSeries);

        TimeSeries trigSeries =
            new TimeSeries(name + " Trigger", Second.class);
        coll.addSeries(trigSeries);

        long minVal = Long.MAX_VALUE;
        long maxVal = Long.MIN_VALUE;
        double minDbl = Double.NEGATIVE_INFINITY;
        double maxDbl = Double.POSITIVE_INFINITY;

        for (BaseData bd : iterator()) {
            TriggerData data = (TriggerData) bd;

            if (data.getValue() < minVal) {
                minVal = data.getValue();
            }
            if (data.getValue() > maxVal) {
                maxVal = data.getValue();
            }

            if (data.getDoubleValue() < minDbl) {
                minDbl = data.getDoubleValue();
            }
            if (data.getDoubleValue() > maxDbl) {
                maxDbl = data.getDoubleValue();
            }
        }

        double div = maxVal - minVal;
        double dblDiv = maxDbl - minDbl;

        for (BaseData bd : iterator()) {
            TriggerData data = (TriggerData) bd;

            double val = ((double) (data.getValue() - minVal)) / div;
            try {
                valSeries.add(data.getTime().getSecond(), val);
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + val);
            }

            double dblVal = (data.getDoubleValue() - minDbl) / dblDiv;
            try {
                trigSeries.add(data.getTime().getSecond(), dblVal);
            } catch (Exception ex) {
                System.err.println("Series \"" + seriesName +
                                   "\" already contains data at " +
                                   data.getTime().getSecond() +
                                   ", cannot add value " + dblVal);
            }
        }

        return coll;
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

        final long val;
        try {
            val = Long.parseLong(matcher.group(3));
        } catch (NumberFormatException nfe) {
            throw new StatParseException("Bad number \"" + matcher.group(3) +
                                         "\" in \"" + line + "\"");
        }

        final double dVal;
        try {
            dVal = Double.parseDouble(matcher.group(4));
        } catch (NumberFormatException nfe) {
            throw new StatParseException("Bad number \"" + matcher.group(4) +
                                         "\" in \"" + line + "\"");
        }

        String name = matcher.group(1);
        if (matcher.group(2) != null) {
            name = name + matcher.group(2);
        }

        if (time == null) {
            throw new StatParseException("Found " + name + " stat " + val +
                                         "/" + dVal + " before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new TriggerData(time, val, dVal));
        }

        return true;
    }
}

class TimingPiece
{
    private String title;
    private long time;
    private long calls;

    TimingPiece(String title, String timeStr, String callsStr)
        throws StatParseException
    {
        this.title = title;
        this.time = parseLong(timeStr);
        this.calls = parseLong(callsStr);
    }

    double getAverageTime()
    {
        return (time == 0 ? 0.0 : (double) time / (double) calls);
    }

    long getProfileCalls()
    {
        return calls;
    }

    long getProfileTime()
    {
        return time;
    }

    String getTitle()
    {
        return title;
    }

    private static long parseLong(String str)
        throws StatParseException
    {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException nfe) {
            throw new StatParseException("Bad number \"" + str + "\"");
        }
    }

    public String toString()
    {
        return title + "=" + time + "/" + calls;
    }
}

class TimingData
    extends BaseData
{
    private List<TimingPiece> list;

    TimingData(ChartTime time, List<TimingPiece> list)
    {
        super(time);

        this.list = list;
    }

    public String getDataString()
    {
        StringBuilder buf = new StringBuilder();

        for (TimingPiece piece : list) {
            if (buf.length() > 0) {
                buf.append(' ');
            }

            buf.append(piece.toString());
        }

        return buf.toString();
    }

    StatParent getParent()
    {
        return new TimingStat();
    }

    Iterable<TimingPiece> iterator()
    {
        return list;
    }

    boolean isEmpty()
    {
        return false;
    }
}

class TimingStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^(\\S+.*\\s+Timing:|\\s+\\S+Timing):?\\s+(.*)\\s*$");
    private static final Pattern PIECE_PAT =
        Pattern.compile("\\s*([^:]+):\\s(\\d+)/(\\d+)=(\\d+)#(\\d+\\.?\\d*%)");

    private ArrayList<String> titles = new ArrayList<String>();

    TimingStat()
    {
    }

    void add(BaseData data)
    {
        TimingData tData = (TimingData) data;

        for (TimingPiece piece : tData.iterator()) {
            if (!titles.contains(piece.getTitle())) {
                titles.add(piece.getTitle());
            }
        }

        super.add(data);
    }

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof TimingData)) {
            throw new ClassCastException("Expected TimingData, not " +
                                         data.getClass().getName());
        }
    }

    double getValue(TimingPiece piece)
    {
        return piece.getProfileTime();
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        final String prefix = pargs.getPrefix(key, name);

        TimeSeries[] series = new TimeSeries[titles.size()];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + titles.get(i), Second.class);
            coll.addSeries(series[i]);
        }

        double[] prevVal = new double[series.length];

        for (int i = 0; i < series.length; i++) {
            prevVal[i] = 0;
        }

        for (BaseData bd : iterator()) {
            TimingData data = (TimingData) bd;

            for (TimingPiece piece : data.iterator()) {
                int idx = titles.indexOf(piece.getTitle());

                double val = getValue(piece);

                double tmpVal = val;
                val = val - prevVal[idx];
                prevVal[idx] = tmpVal;

                series[idx].add(data.getTime().getSecond(), val);
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        return plot(coll, key, name, pargs);
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

        TimeSeries[] series = new TimeSeries[titles.size()];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + titles.get(i), Second.class);
            coll.addSeries(series[i]);
        }

        double[] prevVal = new double[series.length];

        double[] minVal = new double[series.length];
        double[] maxVal = new double[series.length];
        for (int i = 0; i < series.length; i++) {
            minVal[i] = Double.POSITIVE_INFINITY;
            maxVal[i] = Double.NEGATIVE_INFINITY;
        }

        for (int i = 0; i < series.length; i++) {
            prevVal[i] = 0;
        }

        for (BaseData bd : iterator()) {
            TimingData data = (TimingData) bd;

            for (TimingPiece piece : data.iterator()) {
                int idx = titles.indexOf(piece.getTitle());

                double val = getValue(piece);

                double tmpVal = val;
                val = val - prevVal[idx];
                prevVal[idx] = tmpVal;

                if (val < minVal[idx]) {
                    minVal[idx] = val;
                }
                if (val > maxVal[idx]) {
                    maxVal[idx] = val;
                }
            }
        }

        double[] div = new double[series.length];
        for (int i = 0; i < series.length; i++) {
            div[i] = maxVal[i] - minVal[i];
        }

        for (int i = 0; i < series.length; i++) {
            prevVal[i] = 0;
        }

        for (BaseData bd : iterator()) {
            TimingData data = (TimingData) bd;

            for (TimingPiece piece : data.iterator()) {
                int idx = titles.indexOf(piece.getTitle());

                double val = getValue(piece);

                double tmpVal = val;
                val = val - prevVal[idx];
                prevVal[idx] = tmpVal;

                double dVal = (val - minVal[idx]) / div[idx];
                series[idx].add(data.getTime().getSecond(), dVal);
            }
        }

        return coll;
    }

    public static final boolean save(StatData statData, String sectionName,
                                     ChartTime time, String line)
        throws StatParseException
    {
        return save(statData, null, sectionName, time, line, false);
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

        String dataStr = matcher.group(2);
        if (dataStr.equals("NOT RUNNING")) {
            return true;
        }

        String name = matcher.group(1);

        ArrayList<TimingPiece> timing = null;

        int startPos = 0;
        while (true) {
            matcher = PIECE_PAT.matcher(dataStr);
            if (!matcher.find(startPos)) {
                break;
            }

            final String title = matcher.group(1);
            final String cTime = matcher.group(2);
            final String num = matcher.group(3);

            TimingPiece data;
            try {
                data = new TimingPiece(title, cTime, num);
            } catch (StatParseException pex) {
                System.err.println("Couldn't parse timing piece: " +
                                   pex.getMessage());
                continue;
            }

            if (timing == null) {
                timing = new ArrayList<TimingPiece>();
            }
            timing.add(data);

            startPos = matcher.end();
        }

        if (timing == null) {
            return false;
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new TimingData(time, timing));
        }

        return true;
    }

    public boolean showLegend()
    {
        return true;
    }
}

abstract class ListData
    extends BaseData
{
    ListData(ChartTime time)
    {
        super(time);
    }

    abstract String getDataString();
    abstract int getNumEntries();

    StatParent getParent()
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

            for (int i = 0; i < numEntries; i++) {
                series[i].add(data.getTime().getSecond(), data.getRawValue(i));
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        final String prefix = key + " " + name + " ";

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
                for (int i = 0; i < numEntries; i++) {
                    series[i].add(data.getTime().getSecond(),
                                  data.getRawValue(i) - prevVal[i]);
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
        final String prefix = key + " " + name + " ";

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

            for (int i = 0; i < numEntries; i++) {
                double val = (data.getRawValue(i) - minVal) / div;

                series[i].add(data.getTime().getSecond(), val);
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

abstract class BaseParser
{
    private boolean done;
    private ChartTime time;

    BaseParser()
    {
    }

    public ChartTime getTime()
    {
        return time;
    }

    public boolean isDone()
    {
        return done;
    }

    abstract boolean match(StatData statData, String line)
        throws StatParseException;

    void setDone()
    {
        done = true;
    }

    void setTime(long secs)
    {
        time = new ChartTime(secs);
    }
}

final class BombardParser
    extends BaseParser
{
    private static final Pattern PARSE_PAT =
        Pattern.compile(
            "^\\.*((\\S+#\\d+)\\s+)?(\\d+):\\s+(\\S*)MonitoringData:\\s*$");

    private String sectionName;

    private BombardParser(String name)
    {
        sectionName = name;
    }

    public boolean match(StatData statData, String line)
        throws StatParseException
    {
        ChartTime time = getTime();

        if (LongStat.save(statData, sectionName, time, line)) {
            return true;
        }

        if (MemoryStat.save(statData, sectionName, time, line)) {
            return true;
        }

        if (DoubleStat.save(statData, sectionName, time, line)) {
            return true;
        }

        if (TimingStat.save(statData, sectionName, time, line)) {
            return true;
        }

        if (StringStat.save(statData, sectionName, time, line)) {
            return true;
        }

        if (matchStart(this, line) != null) {
            return true;
        }

        if (line.startsWith("BufMgr ") || line.startsWith("Processed ")) {
            setDone();
            return true;
        }

        throw new StatParseException("Unknown line \"" + line + "\"");
    }

    static BombardParser matchStart(String line)
    {
        return matchStart(null, line);
    }

    static BombardParser matchStart(BombardParser parser, String line)
    {
        Matcher matcher = PARSE_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        long val;
        try {
            val = Long.parseLong(matcher.group(3));
        } catch (NumberFormatException nfe) {
            return null;
        }

        final String name = matcher.group(1);

        if (parser == null) {
            parser = new BombardParser(name);
        } else {
            parser.sectionName = name;
        }

        parser.setTime(val);

        return parser;
    }
}

final class EBLogParser
    extends BaseParser
{
    private static final Pattern PARSE_PAT =
        Pattern.compile("^\\s+(\\d+):\\s+(\\S+)\\s+(\\S\\S[^\\s:]+):\\s*$");
    private static final Pattern HUB_PAT =
        Pattern.compile("^\\s+(\\d+):\\s+(\\S+)__(\\d+):\\s*$");
    private static final Pattern RAWTIME_PAT =
        Pattern.compile("^\\s+(\\d+):\\s*$");
    private static final Pattern RAWNAME_PAT =
        Pattern.compile("^(\\S+)\\s+(\\S\\S[^\\s:]+):\\s*$");

    private String sectionHost;
    private String sectionName;

    private boolean grabStrandDepths;
    private boolean ignoreSection;

    private EBLogParser(String host, String name)
    {
        sectionHost = host;
        sectionName = name;
    }

    public boolean match(StatData statData, String line)
        throws StatParseException
    {
        ChartTime time = getTime();

        if (grabStrandDepths) {
            grabStrandDepths = false;
            if (StrandStat.save(statData, sectionHost, sectionName, time,
                                line, ignoreSection))
            {
                return true;
            }

            System.err.println("Bad strand depths \"" + line + "\"");
            return false;
        }

        if (line.startsWith("Number of ")) {
            line = "Num " + line.substring(10);
        }

        if (line.startsWith("Healthy flag: ") ||
            line.startsWith("Back End State: ") ||
            line.startsWith("Missing field: ") ||
            line.startsWith("Statistics for ") ||
            line.startsWith("Dump for ") ||
            line.startsWith("Fetch of ") ||
            line.startsWith("nodeport localhost") ||
            line.startsWith("Failed to fetch "))
        {
            return true;
        } else if (line.startsWith("Strand Depths:")) {
            grabStrandDepths = true;
            return true;
        } else if (line.startsWith("Trigger count:")) {
            if (TriggerStat.save(statData, sectionHost, sectionName, time,
                                 line, ignoreSection))
            {
                return true;
            }

            System.err.println("Bad trigger count line \"" + line + "\"");
            return false;
        }

        if (LongStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (MemoryStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (DoubleStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (TimingStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (matchStart(this, line) != null) {
            return true;
        }

        if (line.startsWith(sectionHost + " " + sectionName)) {
            return true;
        }

        final String oldHost = "spts-evbuilder";
        final String oldName = "ebstrands";

        if (line.startsWith(oldHost + " " + oldName)) {

            sectionHost = oldHost;
            sectionName = oldName;

            return true;
        }

        Matcher matcher;

        matcher = RAWTIME_PAT.matcher(line);
        if (matcher.find()) {
            return true;
        }

        matcher = RAWNAME_PAT.matcher(line);
        if (matcher.find()) {
            return true;
        }

        throw new StatParseException("Unknown line \"" + line + "\"");
    }

    static EBLogParser matchStart(String line)
    {
        return matchStart(null, line);
    }

    static EBLogParser matchStart(EBLogParser parser, String line)
    {
        Matcher matcher = PARSE_PAT.matcher(line);
        if (!matcher.find()) {
            matcher = HUB_PAT.matcher(line);
            if (!matcher.find()) {
                return null;
            }
        }

        long val;
        try {
            val = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException nfe) {
            return null;
        }

        final String host = matcher.group(2);
        final String name = matcher.group(3);

        if (parser == null) {
            parser = new EBLogParser(host, name);
        } else {
            parser.sectionHost = host;
            parser.sectionName = name;
        }

        // eblog times are in seconds
        parser.setTime(val * 1000);

        return parser;
    }
}

final class PDAQParser
    extends BaseParser
{
    private static final Pattern BEAN_PAT =
        Pattern.compile("^Bean\\s+(\\S+)\\s*$");
    private static final Pattern BEANDATE_PAT =
        Pattern.compile("^(\\S+):\\s+(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s" +
                        "\\d\\d:\\d\\d:\\d\\d.\\d+):\\s*$");

    private static SimpleDateFormat dateFmt;

    static {
        dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    };

    private String sectionHost;
    private String sectionName;
    private boolean omitDataCollector;

    private boolean grabStrandDepths;
    private boolean ignoreSection;

    private PDAQParser(String sectionHost, String sectionName,
                       boolean omitDataCollector)
    {
        this.sectionHost = sectionHost;
        this.sectionName = sectionName;
        this.omitDataCollector = omitDataCollector;
    }

    public boolean match(StatData statData, String line)
        throws StatParseException
    {
        ChartTime time = getTime();

        if (grabStrandDepths) {
            grabStrandDepths = false;
            if (StrandStat.save(statData, sectionHost, sectionName, time,
                                line, ignoreSection))
            {
                return true;
            }

            System.err.println("Bad strand depths \"" + line + "\"");
            return false;
        }

        if (line.startsWith("Number of ")) {
            line = "Num " + line.substring(10);
        }

        if (line.startsWith("Healthy flag: ") ||
            line.contains("BackEndState: ") ||
            line.startsWith("Failed to fetch "))
        {
            return true;
        } else if (line.startsWith("StrandDepths:")) {
            grabStrandDepths = true;
            return true;
        } else if (line.startsWith("Triggercount:")) {
            if (TriggerStat.save(statData, sectionHost, sectionName, time,
                                 line, ignoreSection))
            {
                return true;
            }

            System.err.println("Bad trigger count line \"" + line + "\"");
            return false;
        }

        if (LongStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (ListStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (MemoryStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (DoubleStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (TimingStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (StringStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (matchStart(this, null, line, omitDataCollector) != null) {
            return true;
        }

        throw new StatParseException("Unknown line \"" + line + "\"");
    }

    static PDAQParser matchStart(GraphSource inputSrc, String line,
                                 boolean omitDataCollector)
    {
        return matchStart(null, inputSrc, line, omitDataCollector);
    }

    static PDAQParser matchStart(PDAQParser parser, GraphSource inputSrc,
                                 String line, boolean omitDataCollector)
    {
        Matcher matcher = BEANDATE_PAT.matcher(line);
        if (!matcher.find()) {
            matcher = BEAN_PAT.matcher(line);
            if (!matcher.find()) {
                return null;
            }
        }

        String sectionHost;
        if (inputSrc == null) {
            sectionHost = parser.sectionHost;
        } else {
            sectionHost = inputSrc.toString();
            if (sectionHost.endsWith(".moni")) {
                sectionHost =
                    sectionHost.substring(0, sectionHost.length() - 5);
            }
        }

        String sectionName = matcher.group(1);

        if (parser == null) {
            parser =
                new PDAQParser(sectionHost, sectionName, omitDataCollector);
        } else {
            parser.sectionHost = sectionHost;
            parser.sectionName = sectionName;
        }

        parser.ignoreSection = omitDataCollector &&
            sectionName.startsWith("DataCollectorMonitor");

        if (matcher.groupCount() > 1) {
            Date myDate;
            try {
                myDate = dateFmt.parse(matcher.group(2));
            } catch (ParseException pe) {
                System.err.println("Ignoring bad date " + matcher.group(2));
                myDate = null;
            }

            if (myDate != null) {
                parser.setTime(myDate.getTime());
            }
        }

        return parser;
    }
}

public class StatData
{
    private HashMap<SectionKey, HashMap<String, StatParent>> sectionMap =
        new HashMap<SectionKey, HashMap<String, StatParent>>();

    public StatData()
    {
    }

    public void addData(GraphSource inputSrc, boolean omitDataCollector,
                        boolean verbose)
        throws IOException
    {
        if (inputSrc == null) {
            throw new IOException("No input source specified");
        }

        BaseParser parser = null;

        BufferedReader rdr = inputSrc.getReader();
        while (true) {
            String line = rdr.readLine();
            if (line == null) {
                break;
            }

            if (line.length() == 0) {
                continue;
            }

            while (line.charAt(0) == '.') {
                line = line.substring(1);
            }

            if (parser == null) {
                parser =
                    PDAQParser.matchStart(inputSrc, line, omitDataCollector);
                if (parser == null) {
                    parser = EBLogParser.matchStart(line);
                    if (parser == null) {
                        parser = BombardParser.matchStart(line);
                        if (parser == null) {
                            System.err.println("?? " + line);
                        }
                    }
                }
            } else {
                try {
                    parser.match(this, line);
                } catch (StatParseException pe) {
                    System.err.println(pe.getMessage());
                    continue;
                }

                if (parser.isDone()) {
                    break;
                }
            }
        }

        try {
            rdr.close();
        } catch (IOException ioe) {
            // ignore errors on close
        }
    }

    public void add(String host, String section, String name, BaseData datum)
    {
/*
        String key;
        if (host == null) {
            key = section;
        } else {
            key = host + ":" + section;
        }
*/
        SectionKey key = new SectionKey(host, section);

        if (!sectionMap.containsKey(key)) {
            sectionMap.put(key, new HashMap<String, StatParent>());
        }

        StatParent parent;
        boolean isCreated;

        HashMap<String, StatParent> statMap = sectionMap.get(key);
        if (statMap.containsKey(name)) {
            parent = (StatParent) statMap.get(name);
            isCreated = false;
        } else {
            parent = datum.getParent();
            statMap.put(name, parent);
            isCreated = true;
        }

        try {
            parent.add(datum);
        } catch (ClassCastException cce) {
            if (isCreated || !parent.isEmpty()) {
                System.err.println("Cannot add " + key + ":" + name +
                                   " datum " + datum + ": " +
                                   cce.getMessage());
            } else {
                parent = datum.getParent();
                parent.add(datum);

                statMap.put(name, parent);
            }
        } catch (Error err) {
            System.err.println("Cannot add " + key + ":" + name +
                               " datum " + datum + ": " +  err.getMessage());
        }
    }

    public List<String> getSectionNames(SectionKey key)
    {
        if (!sectionMap.containsKey(key)) {
            return null;
        }

        ArrayList<String> names =
            new ArrayList<String>(sectionMap.get(key).keySet());
        Collections.sort(names);
        return names;
    }

    public List<SectionKey> getSections()
    {
        ArrayList<SectionKey> sections =
            new ArrayList<SectionKey>(sectionMap.keySet());
        Collections.sort(sections);
        return sections;
    }

    public StatParent getStatistics(SectionKey section, String name)
    {
        if (!sectionMap.containsKey(section)) {
            return null;
        }

        HashMap<String, StatParent> nameMap = sectionMap.get(section);
        if (nameMap == null || !nameMap.containsKey(name)) {
            return null;
        }

        return nameMap.get(name);
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder("StatData[");

        boolean needComma = false;
        for (SectionKey key : sectionMap.keySet()) {
            if (needComma) {
                buf.append(',');
            } else {
                needComma = true;
            }

            buf.append(key);
        }
        buf.append(']');

        return buf.toString();
    }
}
