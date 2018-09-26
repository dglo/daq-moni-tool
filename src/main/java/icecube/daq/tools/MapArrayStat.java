package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

abstract class MapArrayData
    extends BaseData
{
    MapArrayData(ChartTime time) {
        super(time);
    }

    void addDouble(int index, double val)
        throws StatParseException
    {
        throw new Error("Unimplemented");
    }

    void addLong(int index, long val)
        throws StatParseException
    {
        throw new Error("Unimplemented");
    }

    abstract void addToSeries(TimeSeries series, Second seconds, int index);

    @Override
    String getDataString()
    {
        return toString();
    }

    double getTotalDouble(int index) {
        throw new Error("Unimplemented");
    }

    long getTotalLong(int index) {
        throw new Error("Unimplemented");
    }

    double getValueDouble(int index) {
        throw new Error("Unimplemented");
    }

    long getValueLong(int index) {
        throw new Error("Unimplemented");
    }

    abstract String getValueString(int index);

    boolean isDouble()
    {
        return false;
    }

    @Override
    boolean isEmpty()
    {
        return false;
    }

    boolean isLong()
    {
        return false;
    }

    abstract int length();

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder("[");
        for (int idx = 0; idx < length(); idx++) {
            if (idx > 0) {
                buf.append(", ");
            }
            buf.append(getValueString(idx));
        }
        buf.append("]");
        return buf.toString();
    }
}

class DoubleArrayData
    extends MapArrayData
{
    private double[] array;

    DoubleArrayData(ChartTime time, int length)
    {
        super(time);

        this.array = new double[length];
    }

    void addDouble(int index, double val)
        throws StatParseException
    {
        if (index < 0) {
            throw new StatParseException("Bad index #" + index + " (subzero)");
        } else if (index >= array.length) {
            throw new StatParseException("Bad index #" + index + " (max is " +
                                         (array.length - 1) + ")");
        }

        array[index] = val;
    }

    void addToSeries(TimeSeries series, Second seconds, int index)
    {
        series.add(seconds, array[index]);
    }

    @Override
    StatParent createParent()
    {
        return new MapArrayStat();
    }

    double getTotalDouble() {
        double total = 0.0;
        for (int idx = 0; idx < array.length; idx++) {
            total += array[idx];
        }
        return total;
    }

    @Override
    double getValueDouble(int index) {
        return array[index];
    }

    @Override
    String getValueString(int index)
    {
        return Double.toString(array[index]);
    }

    @Override
    boolean isDouble()
    {
        return true;
    }

    @Override
    int length()
    {
        return array.length;
    }
}

class LongArrayData
    extends MapArrayData
{
    private long[] array;

    LongArrayData(ChartTime time, int length)
    {
        super(time);

        array = new long[length];
    }

    void addLong(int index, long val)
        throws StatParseException
    {
        if (index < 0) {
            throw new StatParseException("Bad index #" + index + " (subzero)");
        } else if (index >= array.length) {
            throw new StatParseException("Bad index #" + index + " (max is " +
                                         (array.length - 1) + ")");
        }

        array[index] = val;
    }

    void addToSeries(TimeSeries series, Second seconds, int index)
    {
        series.add(seconds, array[index]);
    }

    @Override
    StatParent createParent()
    {
        return new MapArrayStat();
    }

    long getTotalLong() {
        long total = 0;
        for (int idx = 0; idx < array.length; idx++) {
            total += array[idx];
        }
        return total;
    }

    @Override
    long getValueLong(int index) {
        return array[index];
    }

    @Override
    String getValueString(int index)
    {
        return Long.toString(array[index]);
    }

    @Override
    boolean isLong()
    {
        return true;
    }

    @Override
    int length()
    {
        return array.length;
    }
}

class MapArrayParser
    extends BaseStatParser
{
    private static final Logger LOG = Logger.getLogger(MapArrayParser.class);

    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s*(\\S+):\\s*\\{(.*)\\}\\s*$");
    private static final Pattern ENTRY_PAT =
        Pattern.compile("^\\s*['\"](\\S+)['\"]:?\\s+\\[([^\\]]*)\\]\\s*,?");

    private static final Pattern COMMA_PAT =
        Pattern.compile("\\s*,\\s*");


    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String name = matcher.group(1);
        String dataStr = matcher.group(2);

        Map<String, BaseData> dataMap =
            new HashMap<String, BaseData>();

boolean debug = false;
if(debug)System.err.printf("--- PARSE %s // %s\n", name, dataStr);
        while (true) {
            matcher = ENTRY_PAT.matcher(dataStr);
if(debug)System.err.println("\tPCHECK " + dataStr);
            if (!matcher.find()) {
if(debug)System.err.println("--- PARSE done");
                break;
            }

            final String fldName = matcher.group(1);
            final String fldList = matcher.group(2);
if(debug)System.err.printf("\tPFOUND %s // %s\n", fldName, fldList);
            try {
                MapArrayData data = parseList(time, fldName, fldList, verbose);
                dataMap.put(name + "_" + fldName, data);
            } catch (StatParseException spe) {
                if (verbose) {
                    LOG.error("Cannot parse \"" + line.trim() + "\"", spe);
                }
if(debug)System.err.println("--- PARSE failed");
                return null;
            }

            dataStr = dataStr.substring(matcher.end());
        }

        return dataMap;
    }

    private static MapArrayData parseList(ChartTime time, String name,
                                          String list, boolean verbose)
        throws StatParseException
    {
        MapArrayData data = null;

        String[] fields = COMMA_PAT.split(list);
        for (int idx = 0; idx < fields.length; idx++) {
            final String fldStr = fields[idx];
            if (data == null || data.isLong()) {
                // try to parse field as a long value
                try {
                    final long val = Long.parseLong(fldStr);
                    if (data == null) {
                        data = new LongArrayData(time, fields.length);
                    }
                    data.addLong(idx, val);
                    continue;
                } catch (NumberFormatException nfe) {
                    // if we previously parsed a long, we're in trouble
                    if (data != null) {
                        final String msg = "Cannot parse long value \"" +
                            fldStr +"\" (\"" + name + "\" field#" + idx +
                            ": " + list + ")";
                        throw new StatParseException(msg);
                    }
                }
            }

            // Long value should have been handled before this point
            if (data != null && data.isLong()) {
                throw new StatParseException("Here be parser dragons!");
            }

            // try to parse field as a double value
            try {
                final double val = Double.parseDouble(fldStr);
                if (data == null) {
                    data = new DoubleArrayData(time, fields.length);
                }
                data.addDouble(idx, val);
                continue;
            } catch (NumberFormatException nfe) {
                final String msg = "Unparseable value \"" + fldStr + "\" (\"" +
                    name + "\" field#" + idx + ": " + list + ")";
                throw new StatParseException(msg);
            }
        }

        return data;
    }
}

class MapArrayStat
    extends StatParent<MapArrayData>
{
    private static final Logger LOG = Logger.getLogger(MapArrayStat.class);

    private static final String[] CPU_STAT_FIELDS = {
        "User", "Nice", "System", "Idle", "IOWait", "IRQ", "SoftIRQ", "Other",
    };

    private static final String[] PROFILE_TIMES_FIELDS = {
        "Count", "Min Value", "Max Value", "Average", "RMS",
    };

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String prefix = pargs.getSeriesPrefix(key, name);

        final int numEntries = length();

        String[] fieldNames = new String[numEntries];

        if (name.startsWith("ProfileTimes")) {
            fieldNames = PROFILE_TIMES_FIELDS;
        } else if (name.startsWith("CPUStatistics")) {
            fieldNames = CPU_STAT_FIELDS;
        } else {
            for (int idx = 0; idx < fieldNames.length; idx++) {
                fieldNames[idx] = String.format(prefix + "Field#" + idx);
            }
        }

        TimeSeries series[] = new TimeSeries[fieldNames.length];

        int offset = 0;
        for (int idx = 0; idx < fieldNames.length; idx++) {
            series[idx] = new TimeSeries(fieldNames[idx], Second.class);
        }

        return series;
    }

    private int length()
    {
        MapArrayData first = null;
        for (MapArrayData data : iterator()) {
            first = (MapArrayData) data;
            break;
        }

        if (first == null) {
            return -1;
        }

        return first.length();
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll,
                                     SectionKey key, String name,
                                     PlotArguments pargs)
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
        }

        int idleIndex = -1;
        if (name.startsWith("CPUStatistics")) {
            for (int idx = 0; idx < CPU_STAT_FIELDS.length; idx++) {
                if (CPU_STAT_FIELDS[idx].equals("Idle")) {
                    idleIndex = idx;
                    break;
                }
            }
        }
        final int extraData = length() - series.length;

        for (MapArrayData data : iterator()) {
            final int len = data.length();
            if (len != series.length + extraData) {
                LOG.error("Ignoring map array with " + len +
                          " entries, expected " + series.length);
                continue;
            }

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            int offset = 0;
            for (int idx = 0; idx < len; idx++) {
                if (idx == idleIndex) {
                    offset += 1;
                } else {
                    data.addToSeries(series[idx - offset], seconds, idx);
                }
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        throw new Error("Unimplemented");
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        throw new Error("Unimplemented");
    }

    @Override
    public boolean showLegend()
    {
        return true;
    }

    @Override
    public Map<String, StatParent> transform(String name)
    {
        final String cpuStatsName = "CPUStatistics";
        if (name.startsWith(cpuStatsName)) {
            String fixedName = name.substring(cpuStatsName.length());
            if (fixedName.charAt(0) == '_') {
                fixedName = fixedName.substring(1);
            }

            return transformCPUStatistics(fixedName);
        }

        return null;
    }

    private Map<String, StatParent> transformCPUStatistics(String name)
    {
        Map<String, StatParent> newMap = new HashMap<String, StatParent>();

        DoubleStat[] stats = new DoubleStat[CPU_STAT_FIELDS.length];
        for (int idx = 0; idx < stats.length; idx++) {
            stats[idx] = new DoubleStat();

            newMap.put(CPU_STAT_FIELDS[idx] + "_" + name, stats[idx]);
        }

        for (MapArrayData maData : iterator()) {
            LongArrayData data = (LongArrayData) maData;
            double total = (double) data.getTotalLong();
            for (int idx = 0; idx < stats.length; idx++) {
                double value = (double) data.getValueLong(idx);
                stats[idx].add(new DoubleData(data.getTime(),
                                              (value * 100.0) / total));
            }
        }

        return newMap;
    }
}
