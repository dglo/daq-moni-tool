package icecube.daq.tools;

public class ChartChoices
{
    private boolean filterBoring;
    private boolean hideLegends;
    private boolean showPoints;
    private ChartType type = ChartType.ALL;

    public ChartChoices()
    {
    }

    public void dump()
    {
        System.out.println(toString());
    }

    public boolean filterBoring()
    {
        return filterBoring;
    }

    public ChartType getType()
    {
        return type;
    }

    public boolean hideLegends()
    {
        return hideLegends;
    }

    public void setFilterBoring(boolean val)
    {
        filterBoring = val;
    }

    public void setHideLegends(boolean val)
    {
        hideLegends = val;
    }

    public void setShowPoints(boolean val)
    {
        showPoints = val;
    }

    public void setType(ChartType val)
    {
        this.type = val;
    }

    public boolean showPoints()
    {
        return showPoints;
    }


    public String toString()
    {
        StringBuffer buf = new StringBuffer("ChartChoices[");
        buf.append(filterBoring ? "" : "!").append("filterBoring ");
        buf.append(hideLegends ? "" : "!").append("hideLegends ");
        buf.append(showPoints ? "" : "!").append("showPoints ");

        String showName;
        buf.append(type.name()).append("]");

        return buf.toString();
    }
}
