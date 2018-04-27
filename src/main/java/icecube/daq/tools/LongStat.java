package icecube.daq.tools;

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

    StatParent createParent()
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            final long val = data.getValue();
            try {
                series.add(seconds, val);
            } catch (Exception exc) {
                long oldVal;
                try {
                    oldVal = (long) series.getDataItem(seconds).getValue();
                } catch (Exception ex2) {
                    System.err.println("Cannot get previous value from" +
                                       " series \"" + seriesName + "\"");
                    continue;
                }

                if (Math.abs(val - oldVal) > 2) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains " + oldVal +
                                       " at " + seconds +
                                       ", cannot add value " + val);
                    continue;
                }
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
                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    System.err.println("Cannot extract seconds from " + data);
                    continue;
                }

                long deltaVal;
                try {
                    deltaVal = data.getValue() - prevVal;
                } catch (Exception exc) {
                    System.err.println("Cannot compute delta for " + data);
                    continue;
                }

                try {
                    series.add(seconds, deltaVal);
                } catch (Exception exc) {
                    long oldVal;
                    try {
                        oldVal = (long) series.getDataItem(seconds).getValue();
                    } catch (Exception ex2) {
                        System.err.println("Cannot get previous value from" +
                                           " series \"" + seriesName + "\"");
                        continue;
                    }

                    if (Math.abs(deltaVal - oldVal) > 2) {
                        System.err.println("Series \"" + seriesName +
                                       "\" already contains " + oldVal +
                                           " at " + seconds +
                                       ", cannot add value " + deltaVal +
                                       " (from " + data.getValue() + ")");
                        continue;
                    }
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
        if (div == 0.0) {
            System.err.println("Series \"" + seriesName + "\" min/max values" +
                               " are identical; skipping");
            return null;
        }

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            double val = ((double) (data.getValue() - minVal)) / div;

            try {
                series.add(seconds, val);
            } catch (Exception exc) {
                double oldVal;
                try {
                    oldVal = (long) series.getDataItem(seconds).getValue();
                } catch (Exception ex2) {
                    System.err.println("Cannot get previous value from" +
                                       " series \"" + seriesName + "\"");
                    continue;
                }

                if (Math.abs(val - oldVal) > 0.2) {
                    System.err.println("Series \"" + seriesName +
                                       "\" already contains " + oldVal +
                                       " at " + seconds +
                                       ", cannot add value " + val +
                                       " (from " + data + ")");
                    continue;
                }
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
