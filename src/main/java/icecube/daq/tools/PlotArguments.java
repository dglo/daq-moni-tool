package icecube.daq.tools;

import java.util.List;

public class PlotArguments
{
    private boolean useLongName;
    private boolean sameHost;
    private boolean sameSection;
    private boolean sameName;

    PlotArguments(List<ComponentData> compList, boolean useLongName)
    {
        this.useLongName = useLongName;

        sameHost = true;
        sameSection = true;
        sameName = true;

        SectionKey prevKey = null;
        String prevName = null;

        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd) {
                for (InstanceBean bean : ci) {
                    if (!bean.hasGraphs()) {
                        continue;
                    }

                    if (prevKey == null) {
                        prevKey = bean.getSectionKey();
                    } else if (sameHost || sameSection) {
                        final SectionKey key = bean.getSectionKey();
                        if (!prevKey.getHost().equals(key.getHost())) {
                            sameHost = false;
                        }
                        if (!prevKey.getSection().equals(key.getSection())) {
                            sameSection = false;
                        }
                    }

                    if (sameName) {
                        for (String name : bean.graphIterable()) {
                            if (prevName == null) {
                                prevName = name;
                            } else if (!prevName.equals(name)) {
                                sameName = false;
                                break;
                            }
                        }
                    } else if (!sameHost && !sameSection) {
                        break;
                    }
                }

                if (!sameHost && !sameSection && !sameName) {
                    break;
                }
            }

            if (!sameHost && !sameSection && !sameName) {
                break;
            }
        }
    }

    private String buildName(SectionKey key, String name)
    {
        StringBuilder buf = new StringBuilder();

        if (!sameHost) {
            buf.append(key.getHost());
        }

        if (!sameSection) {
            if (!sameHost) {
                buf.append(':');
            }

            buf.append(key.getSection());
        }

        if (!sameName || !sameSection || (sameHost && sameSection)) {
            if (buf.length() > 0) {
                buf.append(' ');
            }

            buf.append(name);
        }

        return buf.toString();
    }

    public String getSeriesName(SectionKey key, String name)
    {
        return buildName(key, name);
    }

    public String getSeriesPrefix(SectionKey key, String name)
    {
        return getSeriesName(key, name) + " ";
    }

    public String getSectionTitle(List<ComponentData> compList)
    {
        int sameNum = 0;
        if (sameHost) {
            sameNum++;
        }
        if (sameSection) {
            sameNum++;
        }
        if (sameName) {
            sameNum++;
        }

        final boolean forceHost = sameNum == 0 || sameNum == 3;

        StringBuilder secTitle = new StringBuilder();

        String prevHost = null;
        String prevSection = null;
        String prevName = null;

        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd) {

                boolean addedHost = false;
                for (InstanceBean bean : ci) {
                    if (bean.hasGraphs()) {
                        if (!addedHost && (forceHost || sameHost)) {
                            if (secTitle.length() > 0) {
                                secTitle.append('+');
                            }

                            secTitle.append(cd.getName()).append('-');
                            secTitle.append(ci.getNumber());

                            addedHost = true;

                            if (forceHost) {
                                break;
                            }
                        }

                        if (sameSection) {
                            final String section =
                                bean.getSectionKey().getSection();
                            if (prevSection == null ||
                                !section.equals(prevSection))
                            {
                                if (secTitle.length() > 0) {
                                    if (prevSection == null) {
                                        secTitle.append('+');
                                    } else {
                                        secTitle.append('|');
                                    }
                                }

                                secTitle.append(section);

                                prevSection = section;
                                prevName = null;
                            }
                        }

                        if (sameName) {
                            for (String name : bean.graphIterable()) {
                                if (prevName == null ||
                                    !name.equals(prevName))
                                {
                                    if (secTitle.length() > 0) {
                                        if (prevName == null) {
                                            secTitle.append(' ');
                                        } else {
                                            secTitle.append('/');
                                        }
                                    }

                                    secTitle.append(name);

                                    prevName = name;
                                }
                            }
                        }
                    }
                }
            }
        }

        return secTitle.toString();
    }

    @Override
    public String toString()
    {
        return String.format("PlotArguments[UseLong %s," +
                             "Same[host %s sect %s name %s]]",
                             useLongName, sameHost, sameSection, sameName);
    }
}
