package icecube.daq.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

class MatchResult
{
    public static final MatchResult EMPTY = new MatchResult(null);

    private Map<String, BaseData> dataMap;

    MatchResult(Map<String, BaseData> dataMap)
    {
        this.dataMap = dataMap;
    }
}

abstract class BaseParser
{
    private static final Logger LOG = Logger.getLogger(BaseParser.class);

    private boolean done;
    private ChartTime time;

    private String sectionHost;
    private String sectionName;

    private boolean grabStrandDepths;
    private boolean ignoreSection;

    // the strand parser is weird, can't be part or the 'parsers' array
    private StrandParser strandParser = new StrandParser();

    /** List of parsers */
    private BaseStatParser[] parsers = new BaseStatParser[] {
        new LongParser(),
        new ListParser(),
        new MemoryParser(),
        new DoubleParser(),
        new MapArrayParser(),
        new TimingParser(),
        new StringParser(),
    };

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

    public MatchResult match(StatData statData, String line, boolean verbose)
        throws StatParseException
    {
        if (time == null) {
            throw new StatParseException("Time has not been set");
        }

        if (grabStrandDepths) {
            grabStrandDepths = false;

            Map<String, BaseData> dataMap =
                strandParser.parseLine(time, line, verbose);
            if (dataMap == null) {
                throw new StatParseException("Bad strand depths \"" + line +
                                             "\"");
            }

            return new MatchResult(dataMap);
        }

        if (line.startsWith("Number of ")) {
            line = "Num " + line.substring(10);
        }

        if (line.startsWith("Healthy flag: ") ||
            line.contains("BackEndState: ") ||
            line.startsWith("Failed to fetch "))
        {
            return MatchResult.EMPTY;
        } else if (line.startsWith("StrandDepths:")) {
            grabStrandDepths = true;
            return MatchResult.EMPTY;
        }

        for (BaseStatParser parser : parsers) {
            Map<String, BaseData> dataMap =
                parser.parseLine(time, line, verbose);
            if (dataMap != null) {
                save(statData, sectionHost, sectionName, dataMap);
                if (verbose) {
                    System.err.println(parser.getClass().getName() + " <= " +
                                       line.trim());
                }

                return new MatchResult(dataMap);
            }
        }

        return null;
    }

    private void save(StatCollection collection, String sectionHost,
                      String sectionName, Map<String, BaseData> dataMap)
    {
        for (Map.Entry<String, BaseData> entry : dataMap.entrySet()) {
            collection.add(sectionHost, sectionName, entry.getKey(),
                           entry.getValue());
        }
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

    public MatchResult match(StatData statData, String line, boolean verbose)
        throws StatParseException
    {
        MatchResult result = super.match(statData, line, verbose);
        if (result != null) {
            return result;
        }

        if (matchStart(this, null, line, omitDataCollector) != null) {
if (verbose) System.err.println("??Stat <= " + line.trim());
            return MatchResult.EMPTY;
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

/**
 * A collection of statistics objects
 */
interface StatCollection
{
    void add(String host, String section, String name, BaseData datum);
}

public class StatData
        implements StatCollection
{
    private static final Logger LOG = Logger.getLogger(StatData.class);

    private HashMap<SectionKey, HashMap<String, StatParent>> sectionMap =
        new HashMap<SectionKey, HashMap<String, StatParent>>();

    public StatData()
    {
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
            LOG.error("Cannot add " + key + ":" + name + " datum " + datum +
                      ": " + err.getMessage());
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

    public List<SectionKey> getSectionKeys()
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

    public void loadFile(File file, boolean omitDataCollector, boolean verbose)
    {
        if (file.isDirectory()) {
            for (File entry : file.listFiles()) {
                loadFile(entry, omitDataCollector, verbose);
            }
        } else {
            System.out.println(file + ":");

            try {
                readData(new GraphSource(file), omitDataCollector, verbose);
            } catch (IOException ioe) {
                LOG.error("Couldn't load \"" + file + "\"", ioe);
            }
        }
    }

    public void readData(GraphSource inputSrc, boolean omitDataCollector,
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
                    System.err.println("?? " + line);
                }
            } else {
                MatchResult result;
                try {
                    result = parser.match(this, line, verbose);
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

    public void transform()
    {
        for (SectionKey key : sectionMap.keySet()) {
            HashMap<String, StatParent> nameMap = sectionMap.get(key);
            List<String> keys = new ArrayList<String>(nameMap.keySet());
            for (String name : keys) {
                StatParent stats = nameMap.get(name);

                Map<String, StatParent> newMap = stats.transform(name);
                if (newMap == null) {
                    continue;
                }

                // remove old data, add in new data
                nameMap.remove(name);
                nameMap.putAll(newMap);
            }
        }
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
