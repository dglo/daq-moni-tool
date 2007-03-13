package icecube.daq.tools;

abstract class BaseData
{
    private ChartTime time;

    BaseData(ChartTime time)
    {
        this.time = time;
    }

    abstract String getDataString();

    abstract StatParent getParent();

    ChartTime getTime()
    {
        return time;
    }

    abstract boolean isEmpty();

    public String toString()
    {
        return time.getTime() + "=" + getDataString();
    }
}
