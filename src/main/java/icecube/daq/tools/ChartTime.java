package icecube.daq.tools;

import java.util.Date;

import org.jfree.data.time.Second;

class ChartTime
{
    private long time;
    private Second second;

    ChartTime(long time)
    {
        this.time = time;
    }

    Second getSecond()
    {
        if (second == null) {
            second = new Second(new Date(time));
        }

        return second;
    }

    long getTime()
    {
        return time;
    }

    @Override
    public String toString()
    {
        return Long.toString(time);
    }
}
