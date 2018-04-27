package icecube.daq.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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

    StatParent createParent()
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
    private static final Logger LOG = Logger.getLogger(TriggerStat.class);

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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            try {
                valSeries.add(seconds, data.getValue());
            } catch (Exception ex) {
                LOG.error("Series \"" + seriesName +
                          "\" already contains data at " + seconds +
                          ", cannot add value " + data.getValue());
            }

            try {
                trigSeries.add(seconds, data.getDoubleValue());
            } catch (Exception ex) {
                LOG.error("Series \"" + seriesName +
                          "\" already contains data at " + seconds +
                          ", cannot add value " + data.getDoubleValue());
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
                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    LOG.error("Cannot extract seconds from " + data);
                    continue;
                }

                long lVal =  data.getValue() - prevVal;
                try {
                    valSeries.add(seconds, lVal);
                } catch (Exception ex) {
                    LOG.error("Series \"" + seriesName +
                              "\" already contains data at " + seconds +
                              ", cannot add value " + lVal);
                }

                double dVal =  data.getDoubleValue() - prevTrig;
                try {
                    trigSeries.add(seconds, dVal);
                } catch (Exception ex) {
                    LOG.error("Series \"" + seriesName +
                              "\" already contains data at " + seconds +
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            double val = ((double) (data.getValue() - minVal)) / div;
            try {
                valSeries.add(seconds, val);
            } catch (Exception ex) {
                LOG.error("Series \"" + seriesName +
                          "\" already contains data at " + seconds +
                          ", cannot add value " + val);
            }

            double dblVal = (data.getDoubleValue() - minDbl) / dblDiv;
            try {
                trigSeries.add(seconds, dblVal);
            } catch (Exception ex) {
                LOG.error("Series \"" + seriesName +
                          "\" already contains data at " + seconds +
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
