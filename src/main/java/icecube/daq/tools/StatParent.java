package icecube.daq.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jfree.data.time.TimeSeriesCollection;

public abstract class StatParent<T>
{
    private List<T> dataList;

    void add(T data)
    {
        if (dataList == null) {
            dataList = new ArrayList<T>();
        }

        dataList.add(data);
    }

    public boolean isEmpty()
    {
        return dataList.size() < 2;
    }

    public Iterable<T> iterator()
    {
        return dataList;
    }

    public TimeSeriesCollection plot(SectionKey key, String name,
                                     PlotArguments pargs)
        throws StatPlotException
    {
        return plot(new TimeSeriesCollection(), key, name, pargs);
    }

    public abstract TimeSeriesCollection plot(TimeSeriesCollection coll,
                                              SectionKey key, String name,
                                              PlotArguments pargs)
        throws StatPlotException;

    public TimeSeriesCollection plotDelta(SectionKey key, String name,
                                          PlotArguments pargs)
        throws StatPlotException
    {
        return plotDelta(new TimeSeriesCollection(), key, name, pargs);
    }

    public abstract TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                                   SectionKey key,
                                                   String name,
                                                   PlotArguments pargs)
        throws StatPlotException;

    public abstract TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                                    SectionKey key,
                                                    String name,
                                                    PlotArguments pargs)
        throws StatPlotException;

    public boolean showLegend()
    {
        return false;
    }

    public Map<String, StatParent> transform(String name)
    {
        return null;
    }

    @Override
    public String toString()
    {
        return (dataList == null ? "null" : dataList.toString());
    }
}
