package icecube.daq.tools;

import java.util.ArrayList;
import java.util.Iterator;

import org.jfree.data.time.TimeSeriesCollection;

public abstract class StatParent
{
    private ArrayList dataList;

    void add(BaseData data)
    {
        if (dataList == null) {
            dataList = new ArrayList();
        }

        dataList.add(data);
    }

    public boolean isEmpty()
    {
        if (dataList.size() == 0) {
            return true;
        }

        if (dataList.size() > 1) {
            return false;
        }

        return ((BaseData) dataList.get(0)).isEmpty();
    }

    public Iterator iterator()
    {
        return dataList.iterator();
    }

    public TimeSeriesCollection plot(String section, String name,
                                     boolean useLongName)
    {
        return plot(new TimeSeriesCollection(), section, name, useLongName);
    }

    public abstract TimeSeriesCollection plot(TimeSeriesCollection coll,
                                              String section, String name,
                                              boolean useLongName);

    public TimeSeriesCollection plotDelta(String section, String name)
    {
        return plotDelta(new TimeSeriesCollection(), section, name);
    }

    public abstract TimeSeriesCollection plotDelta(TimeSeriesCollection coll,
                                                   String section,
                                                   String name);

    public abstract TimeSeriesCollection plotScaled(TimeSeriesCollection coll,
                                                    String section,
                                                    String name);

    public boolean showLegend()
    {
        return false;
    }

    public String toString()
    {
        return (dataList == null ? "null" : dataList.toString());
    }
}
