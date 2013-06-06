package icecube.daq.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

public class ChartGenerator
{
    private String title;
    private ArrayList<JFreeChart> chartList = new ArrayList<JFreeChart>();

    ChartGenerator(List<ComponentData> compList, StatData statData,
                   ChartChoices choices)
    {
        if (choices.getType() == ChartChoices.SHOW_ALL ||
            choices.getType() == ChartChoices.SHOW_SELECTED ||
            choices.getType() == ChartChoices.SHOW_DELTA)
        {
            showMultiple(compList, statData, choices);
        } else if (choices.getType() == ChartChoices.SHOW_COMBINED ||
                   choices.getType() == ChartChoices.SHOW_SCALED)
        {
            showCombined(compList, statData, choices);
        } else {
            throw new Error("Unknown chart type#" + choices.getType());
        }
    }

    private void addChart(String name, TimeSeriesCollection coll,
                          boolean showLegend, boolean showPoints)
    {
        JFreeChart chart = createTimeSeriesChart(name, "Time", name, coll,
                                                 showLegend, true, false);
        chart.setBackgroundPaint(Color.white);

        if (showPoints) {
            XYPlot plot = chart.getXYPlot();
            XYItemRenderer renderer = plot.getRenderer();
            if (renderer instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer rr = (XYLineAndShapeRenderer) renderer;
                rr.setShapesVisible(true);
                rr.setShapesFilled(false);

                Shape cross =
                    org.jfree.util.ShapeUtilities.createRegularCross(1.0f,
                                                                     1.0f);
                renderer.setShape(cross);
            }
        }

        chartList.add(chart);
    }

    private JFreeChart createTimeSeriesChart(String title, String timeAxisLabel,
                                             String valueAxisLabel,
                                             XYDataset dataset, boolean legend,
                                             boolean tooltips, boolean urls)
    {
        ValueAxis timeAxis = new DateAxis(timeAxisLabel,
                                          TimeZone.getTimeZone("UTC"));
        // reduce the default margins
        timeAxis.setLowerMargin(0.02);
        timeAxis.setUpperMargin(0.02);

        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        // override default
        valueAxis.setAutoRangeIncludesZero(false);

        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);

        XYToolTipGenerator toolTipGenerator = null;
        if (tooltips) {
            toolTipGenerator
                = StandardXYToolTipGenerator.getTimeSeriesInstance();
        }

        XYURLGenerator urlGenerator = null;
        if (urls) {
            urlGenerator = new StandardXYURLGenerator();
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true,
                false);

        renderer.setBaseToolTipGenerator(toolTipGenerator);
        renderer.setURLGenerator(urlGenerator);
        plot.setRenderer(renderer);


        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot,
                              legend);
    }

    public BufferedImage getImage()
    {
        final int numCharts = chartList.size();
        if (numCharts == 0) {
            return null;
        }

        int numRows = (int) Math.sqrt((double) numCharts);
        int numCols = (numCharts + numRows - 1) / numRows;

        final int colWidth = 300;
        final int rowHeight = 300;

        BufferedImage bigImg =
            new BufferedImage(numCols * colWidth, numRows * rowHeight,
                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bigImg.createGraphics();

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                final int idx = (r * numRows) + c;
                if (idx >= numCharts) {
                    break;
                }

                JFreeChart chart = chartList.get(idx);
                BufferedImage img =
                    chart.createBufferedImage(colWidth, rowHeight);
                g2d.drawImage(img, c * colWidth, r * rowHeight, null);
            }
        }

        return bigImg;
    }

    private String getSectionTitle(List<ComponentData> compList)
    {
        String secTitle = null;

        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd.iterator()) {

                boolean foundInst = false;
                for (InstanceBean bean : ci.iterator()) {
                    if (bean.hasGraphs()) {
                        foundInst = true;
                        break;
                    }
                }

                if (foundInst) {
                    final String compInstName =
                        cd.getName() + "-" + ci.getNumber();
                    if (secTitle == null) {
                        secTitle = compInstName;
                    } else {
                        secTitle = secTitle + "+" + compInstName;
                    }
                }
            }
        }

        return secTitle;
    }

    public String getTitle()
    {
        return title;
    }

    private boolean hasMultipleSections(List<ComponentData> compList,
                                        ChartChoices choices)
    {
        boolean rtnVal;

        if (compList.size() <= 1) {
            rtnVal = false;
        } else if (choices.getType() == ChartChoices.SHOW_COMBINED ||
            choices.getType() == ChartChoices.SHOW_SCALED)
        {
            rtnVal = false;
        } else if (choices.getType() == ChartChoices.SHOW_ALL) {
            rtnVal = true;
        } else {
            rtnVal = false;

            boolean foundOne = false;

            for (ComponentData cd : compList) {
                for (ComponentInstance ci : cd.iterator()) {
                    for (InstanceBean bean : ci.iterator()) {
                        if (bean.hasGraphs()) {
                            if (foundOne) {
                                // if we've found two sections with graphs,
                                // we're done
                                return true;
                            }

                            foundOne = true;
                        }
                    }
                }
            }
        }

        return rtnVal;
    }

    public boolean isEmpty()
    {
        return (chartList.size() == 0);
    }

    private static boolean isInteresting(TimeSeriesCollection coll)
    {
        if (coll == null) {
            return false;
        }

        for (Object obj : coll.getSeries()) {
            TimeSeries series = (TimeSeries) obj;

            double prevVal = Double.NaN;

            for (Object o2 : series.getItems()) {
                TimeSeriesDataItem item =
                    (TimeSeriesDataItem) o2;

                double curVal = item.getValue().doubleValue();

                if (Double.isNaN(prevVal)) {
                    prevVal = curVal;
                } else {
                    double diff = prevVal - curVal;

                    if (diff != 0.0 &&
                        (prevVal != 0.0 && prevVal != 1.0))
                    {
                        return true;
                    }
                }

                prevVal = curVal;
            }
        }

        return false;
    }

    public JPanel layout()
    {
        final int numCharts = chartList.size();
        if (numCharts == 0) {
            return null;
        }

        int numRows = (int) Math.sqrt((double) numCharts);
        int numCols = (numCharts + numRows - 1) / numRows;

        JPanel panel = new JPanel(new GridLayout(numRows, numCols));

        for (JFreeChart chart : chartList) {
            panel.add(new ChartPanel(chart));
        }

        panel.setPreferredSize(new Dimension(800, 600));
        return panel;
    }

    String makeTitle(GraphSource src)
    {
        if (title == null || title.length() == 0) {
            return src.toString();
        }

        return src.toString() + ": " + title;
    }

    private void showCombined(List<ComponentData> compList, StatData statData,
                              ChartChoices choices)
    {
        final boolean multiSection = hasMultipleSections(compList, choices);

        final boolean scale =
            (choices.getType() == ChartChoices.SHOW_SCALED);

        title = getSectionTitle(compList);

        TimeSeriesCollection coll = new TimeSeriesCollection();
        coll.setDomainIsPointsInTime(true);

        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd.iterator()) {
                for (InstanceBean bean : ci.iterator()) {
                    if (!bean.hasGraphs()) {
                        continue;
                    }

                    final String sectionName = bean.getSectionKey().toString();

                    int numCharted = 0;
                    String chartName = null;

                    for (String name : bean.graphIterator()) {
                        StatParent stat =
                            statData.getStatistics(bean.getSectionKey(), name);

                        if (!multiSection) {
                            chartName = name;
                        }

                        if (!scale) {
                            stat.plot(coll, sectionName, name, true);
                        } else {
                            stat.plotScaled(coll, sectionName, name);
                        }
                        numCharted++;
                    }

                    if (title == null) {
                        if (numCharted == 1) {
                            title = chartName;
                        } else {
                            title = sectionName;
                        }
                    }
                }
            }
        }

        String chartName;
        if (scale) {
            chartName = "Scaled Data";
            title = "Scaled " + title;
        } else {
            chartName = "Combined Data";
            title = "Combined " + title;
        }

        boolean showLegend = !choices.hideLegends();

        addChart(chartName, coll, showLegend, choices.showPoints());
    }

    private void showMultiple(List<ComponentData> compList, StatData statData,
                              ChartChoices choices)
    {
        final boolean multiSection = hasMultipleSections(compList, choices);

        final boolean showAll = (choices.getType() == ChartChoices.SHOW_ALL);

        final boolean delta = (choices.getType() == ChartChoices.SHOW_DELTA);

        title = getSectionTitle(compList);

        HashMap<String, List> collisions = new HashMap<String, List>();
        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd.iterator()) {
                for (InstanceBean bean : ci.iterator()) {
                    if (!bean.hasGraphs()) {
                        continue;
                    }

                    Iterable<String> iter;
                    if (showAll || bean.isIncludeAll()) {
                        iter = bean.iterator();
                    } else {
                        iter = bean.graphIterator();
                    }
                    for (String name : iter) {
                        if (!collisions.containsKey(name)) {
                            collisions.put(name, new ArrayList());
                        }
                        collisions.get(name).add(bean);
                    }
                }
            }
        }

        for (ComponentData cd : compList) {
            for (ComponentInstance ci : cd.iterator()) {
                for (InstanceBean bean : ci.iterator()) {
                    if (!bean.hasGraphs()) {
                        continue;
                    }

                    final String sectionName = bean.getSectionKey().toString();

                    int numCharted = 0;
                    String chartName = null;

                    Iterable<String> iter;
                    if (showAll || bean.isIncludeAll()) {
                        iter = bean.iterator();
                    } else {
                        iter = bean.graphIterator();
                    }
                    for (String name : iter) {
                        StatParent stat =
                            statData.getStatistics(bean.getSectionKey(), name);

                        TimeSeriesCollection coll;
                        if (!delta) {
                            coll =
                                stat.plot(sectionName, name, false);
                        } else {
                            coll = stat.plotDelta(sectionName, name);
                        }

                        boolean showLegend =
                            stat.showLegend() && !choices.hideLegends();

                        if (choices.filterBoring() && !isInteresting(coll)) {
                            continue;
                        }

                        String deltaStr;
                        if (!delta) {
                            deltaStr = "";
                        } else {
                            deltaStr = "Delta ";
                        }

                        final boolean collides =
                            collisions.get(name).size() > 1;
                        if (multiSection) {
                            chartName =
                                deltaStr + sectionName + " " + name;
                        } else if (collides) {
                            chartName =
                                deltaStr + bean.getName() + " " + name;
                        } else {
                            chartName = deltaStr + name;
                        }

                        addChart(chartName, coll, showLegend,
                                 choices.showPoints());
                        numCharted++;
                    }

                    if (title == null) {
                        if (numCharted == 1) {
                            title = chartName;
                        } else {
                            title = sectionName;
                        }
                    }
                }
            }
        }
    }
}
