package icecube.daq.tools;

import java.util.Map;

abstract class BaseStatParser
{
    abstract Map<String, BaseData> parseLine(ChartTime time, String line,
                                             boolean verbose)
        throws StatParseException;
}
