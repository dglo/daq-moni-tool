package icecube.daq.tools;

public class StatPlotException
    extends DAQMoniToolException
{
    StatPlotException()
    {
        super();
    }

    StatPlotException(String s)
    {
        super(s);
    }

    StatPlotException(Throwable t)
    {
        super(t);
    }

    StatPlotException(String s, Throwable t)
    {
        super(s, t);
    }
}
