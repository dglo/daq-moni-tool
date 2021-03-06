package icecube.daq.tools;

public class StatParseException
    extends DAQMoniToolException
{
    StatParseException()
    {
        super();
    }

    StatParseException(String s)
    {
        super(s);
    }

    StatParseException(Throwable t)
    {
        super(t);
    }

    StatParseException(String s, Throwable t)
    {
        super(s, t);
    }
}
