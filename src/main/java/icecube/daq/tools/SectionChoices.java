package icecube.daq.tools;

import java.util.ArrayList;

public class SectionChoices
{
    private String section;
    private boolean includeAll;
    private ArrayList names;

    public SectionChoices(String section)
    {
        this.section = section;
    }

    public void add(String name)
    {
        if (names == null) {
            names = new ArrayList();
        }

        names.add(name);
    }

    public String getSection()
    {
        return section;
    }

    public boolean hasGraphs()
    {
        return (includeAll || (names != null && names.size() > 0));
    }

    public boolean isChosen(String name)
    {
        return (includeAll || (names != null && names.contains(name)));
    }

    public void remove(String name)
    {
        if (names != null) {
            names.remove(name);
        }
    }

    public void setIncludeAll(boolean val)
    {
        includeAll = val;
    }

    public String toString()
    {
        if (!includeAll && (names == null || names.size() == 0)) {
            return "";
        }

        return section + ": " + (includeAll ? "ALL" : names.toString());
    }
}
