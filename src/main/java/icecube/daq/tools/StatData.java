package icecube.daq.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

abstract class BaseParser
{
    private static final Logger LOG = Logger.getLogger(BaseParser.class);

    private boolean done;
    private ChartTime time;

    private String sectionHost;
    private String sectionName;

    private boolean grabStrandDepths;
    private boolean ignoreSection;

    BaseParser(String host, String name)
    {
        sectionHost = host;
        sectionName = name;
    }

    public String getHost()
    {
        return sectionHost;
    }

    public String getName()
    {
        return sectionName;
    }

    public ChartTime getTime()
    {
        return time;
    }

    public boolean isDone()
    {
        return done;
    }

    public boolean match(StatData statData, String line, boolean verbose)
        throws StatParseException
    {
        if (grabStrandDepths) {
            grabStrandDepths = false;
            if (StrandStat.save(statData, sectionHost, sectionName, time,
                                line, ignoreSection))
            {
                return true;
            }

            LOG.error("Bad strand depths \"" + line + "\"");
            return false;
        }

        if (line.startsWith("Number of ")) {
            line = "Num " + line.substring(10);
        }

        if (line.startsWith("Healthy flag: ") ||
            line.contains("BackEndState: ") ||
            line.startsWith("Failed to fetch "))
        {
            return true;
        } else if (line.startsWith("StrandDepths:")) {
            grabStrandDepths = true;
            return true;
        }

        if (LongStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (ListStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (MemoryStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (DoubleStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        if (TimingStat.save(statData, sectionHost, sectionName, time, line,
                            ignoreSection))
        {
            return true;
        }

        if (StringStat.save(statData, sectionHost, sectionName, time, line,
                          ignoreSection))
        {
            return true;
        }

        return false;
    }

    void setDone()
    {
        done = true;
    }

    void setHostAndName(String host, String name)
    {
        sectionHost = host;
        sectionName = name;
    }

    void setIgnoreSection(boolean val)
    {
        ignoreSection = val;
    }

    void setTime(long secs)
    {
        time = new ChartTime(secs);
    }
}

final class EBLogParser
    extends BaseParser
{
    private static final Pattern PARSE_PAT =
        Pattern.compile("^\\s+(\\d+):\\s+(\\S+)\\s+(\\S\\S[^\\s:]+):\\s*$");
    private static final Pattern HUB_PAT =
        Pattern.compile("^\\s+(\\d+):\\s+(\\S+)__(\\d+):\\s*$");
    private static final Pattern RAWTIME_PAT =
        Pattern.compile("^\\s+(\\d+):\\s*$");
    private static final Pattern RAWNAME_PAT =
        Pattern.compile("^(\\S+)\\s+(\\S\\S[^\\s:]+):\\s*$");

    private EBLogParser(String host, String name)
    {
        super(host, name);
    }

    public boolean match(StatData statData, String line, boolean verbose)
        throws StatParseException
    {
        if (super.match(statData, line, verbose)) {
            return true;
        }

        if (matchStart(this, line) != null) {
            return true;
        }

        if (line.startsWith(getHost() + " " + getName())) {
            return true;
        }

        Matcher matcher;

        matcher = RAWTIME_PAT.matcher(line);
        if (matcher.find()) {
            return true;
        }

        matcher = RAWNAME_PAT.matcher(line);
        if (matcher.find()) {
            return true;
        }

        throw new StatParseException("Unknown line \"" + line + "\"");
    }

    static EBLogParser matchStart(String line)
    {
        return matchStart(null, line);
    }

    static EBLogParser matchStart(EBLogParser parser, String line)
    {
        Matcher matcher = PARSE_PAT.matcher(line);
        if (!matcher.find()) {
            matcher = HUB_PAT.matcher(line);
            if (!matcher.find()) {
                return null;
            }
        }

        long val;
        try {
            val = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException nfe) {
            return null;
        }

        final String host = matcher.group(2);
        final String name = matcher.group(3);

        if (parser == null) {
            parser = new EBLogParser(host, name);
        } else {
            parser.setHostAndName(host, name);
        }

        // eblog times are in seconds
        parser.setTime(val * 1000);

        return parser;
    }
}

final class PDAQParser
    extends BaseParser
{
    private static final Logger LOG = Logger.getLogger(PDAQParser.class);

    private static final Pattern BEAN_PAT =
        Pattern.compile("^Bean\\s+(\\S+)\\s*$");
    private static final Pattern BEANDATE_PAT =
        Pattern.compile("^(\\S+):\\s+(\\d\\d\\d\\d-\\d\\d-\\d\\d\\s" +
                        "\\d\\d:\\d\\d:\\d\\d.\\d+):\\s*$");

    private static SimpleDateFormat dateFmt;

    static {
        dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    };

    private boolean omitDataCollector;

    private PDAQParser(String sectionHost, String sectionName,
                       boolean omitDataCollector)
    {
        super(sectionHost, sectionName);

        this.omitDataCollector = omitDataCollector;
    }

    public boolean match(StatData statData, String line, boolean verbose)
        throws StatParseException
    {
        if (super.match(statData, line, verbose)) {
            return true;
        }

        if (matchStart(this, null, line, omitDataCollector) != null) {
            return true;
        }

        throw new StatParseException("Unknown line \"" + line + "\"");
    }

    static PDAQParser matchStart(GraphSource inputSrc, String line,
                                 boolean omitDataCollector)
    {
        return matchStart(null, inputSrc, line, omitDataCollector);
    }

    static PDAQParser matchStart(PDAQParser parser, GraphSource inputSrc,
                                 String line, boolean omitDataCollector)
    {
        Matcher matcher = BEANDATE_PAT.matcher(line);
        if (!matcher.find()) {
            matcher = BEAN_PAT.matcher(line);
            if (!matcher.find()) {
                return null;
            }
        }

        String sectionHost;
        if (inputSrc == null) {
            sectionHost = parser.getHost();
        } else {
            sectionHost = inputSrc.toString();
            if (sectionHost.endsWith(".moni")) {
                sectionHost =
                    sectionHost.substring(0, sectionHost.length() - 5);
            } else if (sectionHost.endsWith(".moni.gz")) {
                sectionHost =
                    sectionHost.substring(0, sectionHost.length() - 8);
            }
        }

        String sectionName = matcher.group(1);

        if (parser == null) {
            parser =
                new PDAQParser(sectionHost, sectionName, omitDataCollector);
        } else {
            parser.setHostAndName(sectionHost, sectionName);
        }

        final boolean ignore = omitDataCollector &&
            sectionName.startsWith("DataCollectorMonitor");
        parser.setIgnoreSection(ignore);

        if (matcher.groupCount() > 1) {
            Date myDate;
            try {
                myDate = dateFmt.parse(matcher.group(2));
            } catch (ParseException pe) {
                LOG.error("Ignoring bad date " + matcher.group(2));
                myDate = null;
            }

            if (myDate != null) {
                parser.setTime(myDate.getTime());
            }
        }

        return parser;
    }
}

public class StatData
{
    private static final Logger LOG = Logger.getLogger(StatData.class);

    private HashMap<SectionKey, HashMap<String, StatParent>> sectionMap =
        new HashMap<SectionKey, HashMap<String, StatParent>>();

    public StatData()
    {
    }

    public void addData(GraphSource inputSrc, boolean omitDataCollector,
                        boolean verbose)
        throws IOException
    {
        if (inputSrc == null) {
            throw new IOException("No input source specified");
        }

        BaseParser parser = null;

        BufferedReader rdr = inputSrc.getReader();
        while (true) {
            String line = rdr.readLine();
            if (line == null) {
                break;
            }

            if (line.length() == 0) {
                continue;
            }

            while (line.charAt(0) == '.') {
                line = line.substring(1);
            }

            if (parser == null) {
                parser =
                    PDAQParser.matchStart(inputSrc, line, omitDataCollector);
                if (parser == null) {
                    parser = EBLogParser.matchStart(line);
                    if (parser == null) {
                        System.err.println("?? " + line);
                    }
                }
            } else {
                try {
                    parser.match(this, line, verbose);
                } catch (StatParseException pe) {
                    pe.printStackTrace();
                    continue;
                } catch (RuntimeException re) {
                    re.printStackTrace();
                    continue;
                }

                if (parser.isDone()) {
                    break;
                }
            }
        }

        try {
            rdr.close();
        } catch (IOException ioe) {
            // ignore errors on close
        }
    }

    public void add(String host, String section, String name, BaseData datum)
    {
        SectionKey key = new SectionKey(host, section);

        if (!sectionMap.containsKey(key)) {
            sectionMap.put(key, new HashMap<String, StatParent>());
        }

        StatParent parent;
        boolean isCreated;

        HashMap<String, StatParent> statMap = sectionMap.get(key);
        if (statMap.containsKey(name)) {
            parent = (StatParent) statMap.get(name);
            isCreated = false;
        } else {
            parent = datum.createParent();
            statMap.put(name, parent);
            isCreated = true;
        }

        try {
            parent.add(datum);
        } catch (ClassCastException cce) {
            if (isCreated || !parent.isEmpty()) {
                LOG.error("Cannot add " + key + ":" + name + " datum " +
                          datum, cce);
            } else {
                parent = datum.createParent();
                parent.add(datum);

                statMap.put(name, parent);
            }
        } catch (Error err) {
            LOG.error("Cannot add " + key + ":" + name + " datum " + datum,
                      err);
        }
    }

    public List<String> getSectionNames(SectionKey key)
    {
        if (!sectionMap.containsKey(key)) {
            return null;
        }

        ArrayList<String> names =
            new ArrayList<String>(sectionMap.get(key).keySet());
        Collections.sort(names);
        return names;
    }

    public List<SectionKey> getSections()
    {
        ArrayList<SectionKey> sections =
            new ArrayList<SectionKey>(sectionMap.keySet());
        Collections.sort(sections);
        return sections;
    }

    public StatParent getStatistics(SectionKey section, String name)
    {
        if (!sectionMap.containsKey(section)) {
            return null;
        }

        HashMap<String, StatParent> nameMap = sectionMap.get(section);
        if (nameMap == null || !nameMap.containsKey(name)) {
            return null;
        }

        return nameMap.get(name);
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder("StatData[");

        boolean needComma = false;
        for (SectionKey key : sectionMap.keySet()) {
            if (needComma) {
                buf.append(',');
            } else {
                needComma = true;
            }

            buf.append(key);
        }
        buf.append(']');

        return buf.toString();
    }
}
