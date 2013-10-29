package icecube.daq.tools;

public class ChartChoices
{
    public static final int SHOW_ALL = 1;
    public static final int SHOW_SELECTED = 2;
    public static final int SHOW_COMBINED = 3;
    public static final int SHOW_SCALED = 4;
    public static final int SHOW_LOGARITHMIC = 5;
    public static final int SHOW_DELTA = 6;

    private boolean filterBoring;
    private boolean hideLegends;
    private boolean showPoints;
    private int type = SHOW_ALL;

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

    public int getType()
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

    public void setType(int val)
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
        switch (type) {
        case SHOW_ALL:
            showName = "show ALL";
            break;
        case SHOW_SELECTED:
            showName = "show SELECTED";
            break;
        case SHOW_COMBINED:
            showName = "show COMBINED";
            break;
        case SHOW_SCALED:
            showName = "show SCALED";
            break;
        case SHOW_LOGARITHMIC:
            showName = "show LOGARITHMIC";
            break;
        case SHOW_DELTA:
            showName = "show DELTA";
            break;
        default:
            showName = "Unknown type #" + type;
            break;
        }
        buf.append(showName).append("]");

        return buf.toString();
    }
}
