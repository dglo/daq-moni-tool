package icecube.daq.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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

    StatParent createParent()
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

class TimingParser
    extends BaseStatParser
{
    private static final Logger LOG = Logger.getLogger(TimingParser.class);

    private static final Pattern STAT_PAT =
        Pattern.compile("^(\\S+.*\\s+Timing:|\\s+\\S+Timing):?\\s+(.*)\\s*$");
    private static final Pattern PIECE_PAT =
        Pattern.compile("\\s*([^:]+):\\s(\\d+)/(\\d+)=(\\d+)#(\\d+\\.?\\d*%)");

    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
        throws StatParseException
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String dataStr = matcher.group(2);
        if (dataStr.equals("NOT RUNNING")) {
            return new HashMap<String, BaseData>();
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
                LOG.error("Couldn't parse timing piece", pex);
                continue;
            }

            if (timing == null) {
                timing = new ArrayList<TimingPiece>();
            }
            timing.add(data);

            startPos = matcher.end();
        }

        TimingData data = new TimingData(time, timing);

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class TimingStat
    extends StatParent<TimingData>
{
    private static final Logger LOG = Logger.getLogger(TimingStat.class);

    private ArrayList<String> titles = new ArrayList<String>();

    TimingStat()
    {
    }

    void add(TimingData data)
    {
        for (TimingPiece piece : data.iterator()) {
            if (!titles.contains(piece.getTitle())) {
                titles.add(piece.getTitle());
            }
        }

        super.add(data);
    }

    private TimeSeries[] generateSeries(SectionKey key, String name,
                                        PlotArguments pargs)
        throws StatPlotException
    {
        final String prefix = pargs.getSeriesPrefix(key, name);

        TimeSeries[] series = new TimeSeries[titles.size()];
        for (int i = 0; i < series.length; i++) {
            series[i] = new TimeSeries(prefix + titles.get(i), Second.class);
        }

        return series;
    }

    double getValue(TimingPiece piece)
    {
        return piece.getProfileTime();
    }

    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
        throws StatPlotException
    {
        TimeSeries series[] = generateSeries(key, name, pargs);
        for (TimeSeries entry : series) {
            coll.addSeries(entry);
        }

        double[] prevVal = new double[series.length];
        for (int i = 0; i < series.length; i++) {
            prevVal[i] = 0;
        }

        for (BaseData bd : iterator()) {
            TimingData data = (TimingData) bd;

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            for (TimingPiece piece : data.iterator()) {
                int idx = titles.indexOf(piece.getTitle());

                double val = getValue(piece);

                double tmpVal = val;
                val = val - prevVal[idx];
                prevVal[idx] = tmpVal;

                series[idx].add(seconds, val);
            }
        }

        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
        throws StatPlotException
    {
        return plot(coll, key, name, pargs);
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

            Second seconds;
            try {
                seconds = data.getTime().getSecond();
            } catch (Exception exc) {
                LOG.error("Cannot extract seconds from " + data);
                continue;
            }

            for (TimingPiece piece : data.iterator()) {
                int idx = titles.indexOf(piece.getTitle());

                double val = getValue(piece);

                double tmpVal = val;
                val = val - prevVal[idx];
                prevVal[idx] = tmpVal;

                double dVal = (val - minVal[idx]) / div[idx];
                series[idx].add(seconds, dVal);
            }
        }

        return coll;
    }

    public boolean showLegend()
    {
        return true;
    }
}
