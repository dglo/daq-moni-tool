package icecube.daq.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

class StringData
    extends BaseData
{
    private String val;

    StringData(ChartTime time, String val)
    {
        super(time);

        this.val = val;
    }

    @Override
    StatParent createParent()
    {
        return new StringStat();
    }

    @Override
    String getDataString()
    {
        return "\"" + val + "\"";
    }

    String getValue()
    {
        return val;
    }

    @Override
    boolean isEmpty()
    {
        return val == null || val.length() == 0;
    }
}

class StringParser
    extends BaseStatParser
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s+([^\\s:]+):?\\s+(.*)\\s*$");

    Map<String, BaseData> parseLine(ChartTime time, String line,
                                    boolean verbose)
        throws StatParseException
    {
        Matcher matcher = STAT_PAT.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String name = matcher.group(1);
        final String val = matcher.group(2);

        StringData data = new StringData(time, val);

        Map<String, BaseData> map = new HashMap<String, BaseData>();
        map.put(name, data);
        return map;
    }
}

class StringStat
    extends StatParent<StringData>
{
    public TimeSeriesCollection plot(TimeSeriesCollection coll, SectionKey key,
                                     String name, PlotArguments pargs)
    {
        // do nothing
        return coll;
    }

    public TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                          SectionKey key, String name,
                                          PlotArguments pargs)
    {
        // do nothing
        return coll;
    }

    public TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                           SectionKey key, String name,
                                           PlotArguments pargs)
    {
        // do nothing
        return coll;
    }
}
