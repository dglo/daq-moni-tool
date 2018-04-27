package icecube.daq.tools;

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

    String getDataString()
    {
        return Long.toString(usedMem) + " used, " + freeMem + " free";
    }

    long getFreeMemory()
    {
        return freeMem;
    }

    StatParent createParent()
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
    private static final Logger LOG = Logger.getLogger(MemoryStat.class);

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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            try {
                usedSeries.add(seconds, data.getUsedMemory());
            } catch (Exception ex) {
                final Number tmpVal =
                    usedSeries.getDataItem(seconds).getValue();
                System.err.println("Series \"" + prefix +
                                   " Used\" already contains " + tmpVal +
                                   " at " + seconds + ", cannot add value " +
                                   data.getUsedMemory());
            }
            try {
                freeSeries.add(seconds, data.getFreeMemory());
            } catch (Exception ex) {
                final Number tmpVal =
                    freeSeries.getDataItem(seconds).getValue();
                System.err.println("Series \"" + prefix +
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
    {
        final String prefix = pargs.getPrefix(key, name);

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

                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    System.err.println("Cannot extract seconds from " + data);
                    continue;
                }

                val = data.getUsedMemory() - prevUsed;
                try {
                    usedSeries.add(seconds, val);
                } catch (Exception ex) {
                    final Number tmpVal =
                        usedSeries.getDataItem(seconds).getValue();
                    LOG.error("Series \"" + prefix +
                              " Used\" already contains " + tmpVal +
                                       " at " + seconds +
                                       ", cannot add value " + val +
                                       " (from " + data + ")");
                }

                val = data.getFreeMemory() - prevFree;
                try {
                    freeSeries.add(seconds, val);
                } catch (Exception ex) {
                    final Number tmpVal =
                        freeSeries.getDataItem(seconds).getValue();
                    LOG.error("Series \"" + prefix +
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
    {
        final String prefix = pargs.getPrefix(key, name);

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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            double used = ((double) (data.getUsedMemory() - minVal)) / div;
            try {
                usedSeries.add(seconds, used);
            } catch (Exception ex) {
                LOG.error("Series \"" + prefix +
                          " Used\" already contains data at " + seconds +
                          ", cannot add value " + used);
            }

            double free = ((double) (data.getFreeMemory() - minVal)) / div;
            try {
                freeSeries.add(seconds, free);
            } catch (Exception ex) {
                LOG.error("Series \"" + prefix +
                          " Free\" already contains data at " + seconds +
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
