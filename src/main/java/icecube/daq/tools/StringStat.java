package icecube.daq.tools;

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

    String getDataString()
    {
        return "\"" + val + "\"";
    }

    StatParent createParent()
    {
        return new StringStat();
    }

    String getValue()
    {
        return val;
    }

    boolean isEmpty()
    {
        return val == null || val.length() == 0;
    }
}

class StringStat
    extends StatParent
{
    private static final Pattern STAT_PAT =
        Pattern.compile("^\\s+([^\\s:]+):?\\s+(.*)\\s*$");

    public void checkDataType(BaseData data)
    {
        if (!(data instanceof StringData)) {
            throw new ClassCastException("Expected StringData, not " +
                                         data.getClass().getName());
        }
    }

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

    public static final boolean save(StatData statData, ChartTime time,
                                     String line)
        throws StatParseException
    {
        return save(statData, null, time, line);
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

        String name = matcher.group(1);
        final String val = matcher.group(2);

        if (time == null) {
            throw new StatParseException("Found " + name + " stat " + val +
                                         " before time was set");
        }

        if (!ignore) {
            statData.add(sectionHost, sectionName, name,
                         new StringData(time, val));
        }

        return true;
    }
}
