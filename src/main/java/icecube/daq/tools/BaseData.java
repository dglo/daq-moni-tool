package icecube.daq.tools;

abstract class BaseData
{
    private ChartTime time;

    BaseData(ChartTime time)
    {
        this.time = time;
    }

    abstract StatParent createParent();

    abstract String getDataString();

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
