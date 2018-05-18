package icecube.daq.tools;

public class DAQMoniToolException
    extends Exception
{
    DAQMoniToolException()
    {
        super();
    }

    DAQMoniToolException(String s)
    {
        super(s);
    }

    DAQMoniToolException(Throwable t)
    {
        super(t);
    }

    DAQMoniToolException(String s, Throwable t)
    {
        super(s, t);
    }
}
