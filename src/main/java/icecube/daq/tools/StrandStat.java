package icecube.daq.tools;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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

    StatParent createParent()
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
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
    {
        final String prefix = pargs.getPrefix(key, name);

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
                Second seconds;
                try {
                    seconds = data.getTime().getSecond();
                } catch (Exception exc) {
                    System.err.println("Cannot extract seconds from " + data);
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
    {
        final String prefix = pargs.getPrefix(key, name);

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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                System.err.println("Cannot extract seconds from " + data);
                continue;
            }

            for (int i = 0; i < numStrands; i++) {
                double val = ((double) (data.getStrand(i) - minVal)) / div;

                series[i].add(seconds, val);
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
