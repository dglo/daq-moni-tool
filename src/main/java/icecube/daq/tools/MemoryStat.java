package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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

    @Override
    StatParent createParent()
    {
        return new MemoryStat();
    }

    @Override
    String getDataString()
    {
        return Long.toString(usedMem) + " used, " + freeMem + " free";
    }

    long getFreeMemory()
    {
        return freeMem;
    }

    long getUsedMemory()
    {
        return usedMem;
    }

    @Override
    boolean isEmpty()
    {
        return usedMem == 0L && freeMem == 0L;
    }
}

class MemoryParser
    extends BaseStatParser
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^(\\s+([^\\s:]+):?|\\s*(.+)\\s*:)" +
                        "\\s+(\\d+)([KMG]?)\\s+used," +
                        "\\s+(\\d+)([KMG]?)\\s+of" +
                        "\\s+(\\d+)([KMG]?)\\s+free\\.$");

    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
        throws StatParseException
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
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
            throw new StatParseException("Found " + name +
                                         " stat before time was set");
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

        MemoryData data = new MemoryData(time, memVals);

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class MemoryStat
    extends StatParent<MemoryData>
{
    private static final Logger LOG = Logger.getLogger(MemoryStat.class);

    private static final int USED_INDEX = 0;
    private static final int FREE_INDEX = 1;

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String prefix = pargs.getSeriesPrefix(key, name);

        return new TimeSeries[] {
            new TimeSeries(prefix + "Used", Second.class),  // USED_INDEX
            new TimeSeries(prefix + "Free", Second.class),  // FREE_INDEX
        };
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
            MemoryData data = (MemoryData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            try {
                series[USED_INDEX].add(seconds, data.getUsedMemory());
            } catch (Exception exc) {
                final Number tmpVal =
                    series[USED_INDEX].getDataItem(seconds).getValue();
                System.err.println("Series \"" +
                                   pargs.getSeriesPrefix(key, name) +
                                   " Used\" already contains " + tmpVal +
                                   " at " + seconds + ", cannot add value " +
                                   data.getUsedMemory());
            }
            try {
                series[FREE_INDEX].add(seconds, data.getFreeMemory());
            } catch (Exception exc) {
                final Number tmpVal =
                    series[FREE_INDEX].getDataItem(seconds).getValue();
                System.err.println("Series \"" +
                                   pargs.getSeriesPrefix(key, name) +
                                   " Free\" already contains " + tmpVal +
                                   " at " + seconds + ", cannot add value " +
                                   data.getFreeMemory());
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

        long prevUsed = 0;
        long prevFree = 0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            MemoryData data = (MemoryData) bd;

            if (firstVal) {
                firstVal = false;
            } else {
                long val;

                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    System.err.println("Cannot extract seconds from " + data);
                    continue;
                }

                val = data.getUsedMemory() - prevUsed;
                try {
                    series[USED_INDEX].add(seconds, val);
                } catch (Exception exc) {
                    final Number tmpVal =
                        series[USED_INDEX].getDataItem(seconds).getValue();
                    LOG.error("Series \"" + pargs.getSeriesPrefix(key, name) +
                              " Used\" already contains " + tmpVal +
                                       " at " + seconds +
                                       ", cannot add value " + val +
                                       " (from " + data + ")");
                }

                val = data.getFreeMemory() - prevFree;
                try {
                    series[FREE_INDEX].add(seconds, val);
                } catch (Exception exc) {
                    final Number tmpVal =
                        series[FREE_INDEX].getDataItem(seconds).getValue();
                    LOG.error("Series \"" + pargs.getSeriesPrefix(key, name) +
                              " Free\" already contains " + tmpVal + " at " +
                              seconds + ", cannot add value " + val +
                              " (from " + data + ")");
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
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
        }

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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            double used = ((double) (data.getUsedMemory() - minVal)) / div;
            try {
                series[USED_INDEX].add(seconds, used);
            } catch (Exception exc) {
                LOG.error("Series \"" + pargs.getSeriesPrefix(key, name) +
                          " Used\" already contains data at " + seconds +
                          ", cannot add value " + used);
            }

            double free = ((double) (data.getFreeMemory() - minVal)) / div;
            try {
                series[FREE_INDEX].add(seconds, free);
            } catch (Exception exc) {
                LOG.error("Series \"" + pargs.getSeriesPrefix(key, name) +
                          " Free\" already contains data at " + seconds +
                          ", cannot add value " + free);
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
