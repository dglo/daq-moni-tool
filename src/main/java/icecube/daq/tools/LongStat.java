package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

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

class LongParser
    extends BaseStatParser
{
    private static final Pattern STAT_PAT =
        Pattern.compile(
            "^(\\s+([^\\s:]+)|\\s*(.+)\\s*):?\\s+([\\-\\+]?\\d+)L?\\s*$");

    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
        throws StatParseException
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
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

        final LongData data = new LongData(time, val);

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class LongStat
    extends StatParent<LongData>
{
    private static final Logger LOG = Logger.getLogger(LongStat.class);

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String seriesName = pargs.getSeriesName(key, name);

        return new TimeSeries[] {
            new TimeSeries(seriesName, Second.class),
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
            LongData data = (LongData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            final long val = data.getValue();
            try {
                series[0].add(seconds, val);
            } catch (Exception exc) {
                long oldVal;
                try {
                    oldVal = (long) series[0].getDataItem(seconds).getValue();
                } catch (Exception ex2) {
                    LOG.error("Cannot get previous value from series \"" +
                              pargs.getSeriesName(key, name) + "\"");
                    continue;
                }

                if (Math.abs(val - oldVal) > 2) {
                    LOG.error("Series \"" + pargs.getSeriesName(key, name) +
                              "\" already contains " + oldVal + " at " +
                              seconds + ", cannot add value " + val);
                    continue;
                }
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
                    LOG.error("Cannot extract seconds from " + data);
                    continue;
                }

                long deltaVal;
                try {
                    deltaVal = data.getValue() - prevVal;
                } catch (Exception exc) {
                    LOG.error("Cannot compute delta for " + data);
                    continue;
                }

                try {
                    series[0].add(seconds, deltaVal);
                } catch (Exception exc) {
                    long oldVal;
                    try {
                        oldVal =
                            (long) series[0].getDataItem(seconds).getValue();
                    } catch (Exception ex2) {
                        LOG.error("Cannot get previous value from series \"" +
                                  pargs.getSeriesName(key, name) + "\"");
                        continue;
                    }

                    if (Math.abs(deltaVal - oldVal) > 2) {
                        LOG.error("Series \"" +
                                  pargs.getSeriesName(key, name) +
                                  "\" already contains " + oldVal + " at " +
                                  seconds + ", cannot add value " + deltaVal +
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
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
        }

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
            LOG.error("Series \"" + pargs.getSeriesName(key, name) +
                      "\" min/max values are identical; skipping");
            return null;
        }

        for (BaseData bd : iterator()) {
            LongData data = (LongData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            double val = ((double) (data.getValue() - minVal)) / div;

            try {
                series[0].add(seconds, val);
            } catch (Exception exc) {
                double oldVal;
                try {
                    oldVal = (long) series[0].getDataItem(seconds).getValue();
                } catch (Exception ex2) {
                    LOG.error("Cannot get previous value from series \"" +
                              pargs.getSeriesName(key, name) + "\"");
                    continue;
                }

                if (Math.abs(val - oldVal) > 0.2) {
                    LOG.error("Series \"" + pargs.getSeriesName(key, name) +
                              "\" already contains " + oldVal + " at " +
                              seconds + ", cannot add value " + val +
                              " (from " + data + ")");
                    continue;
                }
            }
        }

        return coll;
    }
}
