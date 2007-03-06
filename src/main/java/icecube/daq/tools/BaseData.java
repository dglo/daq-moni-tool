package icecube.daq.tools;

abstract class BaseData
{
    private ChartTime time;

    BaseData(ChartTime time)
    {
        this.time = time;
    }

    abstract String getDataString();

    ChartTime getTime()
    {
        return time;
    }

    public String toString()
    {
        return time.getTime() + "=" + getDataString();
    }
}
