package icecube.daq.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ChartChoices
{
    public static final int SHOW_ALL = 1;
    public static final int SHOW_SELECTED = 2;
    public static final int SHOW_COMBINED = 3;
    public static final int SHOW_SCALED = 4;
    public static final int SHOW_DELTA = 5;

    private boolean showPoints;
    private boolean filterBoring;
    private int type = SHOW_ALL;
    private HashMap sectionMap = new HashMap();

    public ChartChoices()
    {
    }

    public ChartChoices(StatData statData, List includeList, List excludeList)
    {
        if (includeList.size() > 0 && excludeList.size() > 0) {
            throw new Error("Cannot specify both" +
                            " included and excluded sections!");
        }

        initialize(statData);

        if (includeList.size() > 0) {
            // only include specified sections
            Iterator iter = includeList.iterator();
            while (iter.hasNext()) {
                String section = (String) iter.next();

                if (sectionMap.containsKey(section)) {
                    SectionChoices sc =
                        (SectionChoices) sectionMap.get(section);
                    sc.setIncludeAll(true);
                }
            }

            type = SHOW_SELECTED;
        } else {
            // exclude specified sections
            Iterator iter = sectionMap.values().iterator();
            while (iter.hasNext()) {
                SectionChoices sc = (SectionChoices) iter.next();
                if (!excludeList.contains(sc.getSection())) {
                    sc.setIncludeAll(true);
                }
            }

            type = SHOW_SELECTED;
        }
    }

    public void addSection(SectionChoices sc)
    {
        sectionMap.put(sc.getSection(), sc);
    }

    public void clearSections()
    {
        sectionMap.clear();
    }

    public void dump()
    {
        System.out.println((showPoints ? "" : "!") + "showPoints");
        System.out.println((filterBoring ? "" : "!") + "filterBoring");

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
        case SHOW_DELTA:
            showName = "show DELTA";
            break;
        default:
            showName = "Unknown type #" + type;
            break;
        }
        System.out.println(showName);

        System.out.println(sectionMap.values());
    }

    public boolean filterBoring()
    {
        return filterBoring;
    }

    public SectionChoices getSection(String section)
    {
        if (!sectionMap.containsKey(section)) {
            return null;
        }

        return (SectionChoices) sectionMap.get(section);
    }

    public int getType()
    {
        return type;
    }

    public void initialize(StatData statData)
    {
        Iterator sectIter = statData.getSections().iterator();
        while (sectIter.hasNext()) {
            String section = (String) sectIter.next();

            List names = statData.getSectionNames(section);
            if (names.size() > 0) {
                sectionMap.put(section, new SectionChoices(section));
            }
        }
    }

    public void setFilterBoring(boolean val)
    {
        filterBoring = val;
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
}

