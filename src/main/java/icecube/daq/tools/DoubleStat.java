package icecube.daq.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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

    StatParent createParent()
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            final double val = data.getValue();

            try {
                series.add(seconds, val);
            } catch (Exception exc) {
                double oldVal;
                try {
                    oldVal = (double) series.getDataItem(seconds).getValue();
                } catch (Exception ex2) {
                    System.err.println("Cannot get previous value from" +
                                       " series \"" + seriesName + "\"");
                    continue;
                }

                if (Math.abs(val - oldVal) > 0.2) {
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

        double prevVal = 0;
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            DoubleData data = (DoubleData) bd;

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

                double deltaVal;
                try {
                    deltaVal = data.getValue() - prevVal;
                } catch (Exception exc) {
                    System.err.println("Cannot compute delta for " + data);
                    continue;
                }

                try {
                    series.add(seconds, deltaVal);
                } catch (Exception ex) {
                    double oldVal;
                    try {
                        oldVal = (long) series.getDataItem(seconds).getValue();
                    } catch (Exception ex2) {
                        System.err.println("Cannot get previous value from" +
                                           " series \"" + seriesName + "\"");
                        continue;
                    }

                    if (Math.abs(deltaVal - oldVal) > 0.2) {
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            double val = (data.getValue() - minVal) / div;

            try {
                series.add(seconds, val);
            } catch (Exception ex) {
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
