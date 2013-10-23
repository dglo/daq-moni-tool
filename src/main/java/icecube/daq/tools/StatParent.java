package icecube.daq.tools;

import java.util.ArrayList;

import org.jfree.data.time.TimeSeriesCollection;

public abstract class StatParent
{
    private ArrayList<BaseData> dataList;

    void add(BaseData data)
    {
        if (dataList == null) {
            dataList = new ArrayList<BaseData>();
        }

        checkDataType(data);

        dataList.add(data);
    }

    public abstract void checkDataType(BaseData data);

    public boolean isEmpty()
    {
        return dataList.size() < 2;
    }

    public Iterable<BaseData> iterator()
    {
        return dataList;
    }

    public TimeSeriesCollection plot(SectionKey key, String name,
                                     PlotArguments pargs)
    {
        return plot(new TimeSeriesCollection(), key, name, pargs);
    }

    public abstract TimeSeriesCollection plot(TimeSeriesCollection coll,
                                              SectionKey key, String name,
                                              PlotArguments pargs);

    public TimeSeriesCollection plotDelta(SectionKey key, String name,
                                          PlotArguments pargs)
    {
        return plotDelta(new TimeSeriesCollection(), key, name, pargs);
    }

    public abstract TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                                   SectionKey key,
                                                   String name,
                                                   PlotArguments pargs);

    public abstract TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                                    SectionKey key,
                                                    String name,
                                                    PlotArguments pargs);

    public boolean showLegend()
    {
        return false;
    }

    public String toString()
    {
        return (dataList == null ? "null" : dataList.toString());
    }
}
