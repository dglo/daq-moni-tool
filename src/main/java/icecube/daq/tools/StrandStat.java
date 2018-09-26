package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

class InternalStrandError
    extends Error
{
    InternalStrandError(String msg)
    {
        super(msg);
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

    StatParent createParent()
    {
        return new StrandStat(depths.length);
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

class StrandParser
    extends BaseStatParser
{
    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
        throws StatParseException
    {
        String[] flds = line.split("\\s+");
        if (flds == null || flds.length == 0) {
            return null;
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

        StrandData data = new StrandData(time, vals);

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class StrandStat
    extends StatParent<StrandData>
{
    private static final Logger LOG = Logger.getLogger(StrandStat.class);

    private int numStrands;

    StrandStat(int numStrands)
    {
        this.numStrands = numStrands;
    }

    void add(StrandData data)
    {
        if (data.getNumStrands() != numStrands) {
            throw new InternalStrandError("Expected " + numStrands +
                                          " strands, not " +
                                          data.getNumStrands());
        }

        super.add(data);
    }

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String prefix = pargs.getSeriesPrefix(key, name);

        TimeSeries[] series = new TimeSeries[numStrands];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + "Strand " + i, Second.class);
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
            StrandData data = (StrandData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numStrands; i++) {
                series[i].add(seconds, data.getStrand(i));
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

        long[] prevVal = new long[numStrands];
        boolean firstVal = true;

        for (BaseData bd : iterator()) {
            StrandData data = (StrandData) bd;

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

                for (int i = 0; i < numStrands; i++) {
                    series[i].add(seconds, data.getStrand(i) - prevVal[i]);
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
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numStrands; i++) {
                double val = ((double) (data.getStrand(i) - minVal)) / div;

                series[i].add(seconds, val);
            }
        }

        return coll;
    }

    public boolean showLegend()
    {
        return true;
    }
}
