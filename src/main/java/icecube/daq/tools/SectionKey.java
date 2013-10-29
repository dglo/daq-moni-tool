package icecube.daq.tools;

public class SectionKey
    implements Comparable
{
    private String host;
    private String section;

    private String comp;
    private int inst;

    SectionKey(String host, String section)
    {
        if (host == null) {
            throw new Error("Host cannot be null");
        } else if (section == null) {
            throw new Error("Section cannot be null");
        }

        this.host = host;
        this.section = section;

        final int minusIdx = host.lastIndexOf('-');
        if (minusIdx < 0) {
            comp = host;
            inst = 0;
        } else {
            comp = host.substring(0, minusIdx);

            String numStr = host.substring(minusIdx + 1);
            try {
                inst = Integer.parseInt(numStr);
            } catch (NumberFormatException nfe) {
                System.err.println("Bad instance number \"" + numStr +
                                   "\" for host \"" + host + "\"");
            }
        }
    }

    public int compareTo(Object obj)
    {
        if (obj == null) {
            return 1;
        }

        if (!(obj instanceof SectionKey)) {
            return getClass().getName().compareTo(getClass().getName());
        }

        SectionKey other = (SectionKey) obj;
        int val = host.compareTo(other.host);
        if (val == 0) {
            val = section.compareTo(other.section);
        }

        return val;
    }

    public boolean equals(Object obj)
    {
        return compareTo(obj) == 0;
    }

    public String getComponent()
    {
        return comp;
    }

    public String getHost()
    {
        return host;
    }

    public int getInstance()
    {
        return inst;
    }

    public String getSection()
    {
        return section;
    }

    public int hashCode()
    {
        return (host.hashCode() & 0xffff) << 16 +
            (section.hashCode() & 0xffff);
    }

    public String toString()
    {
        return host + ":" + section;
    }
}
