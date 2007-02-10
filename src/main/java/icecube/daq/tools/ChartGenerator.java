package icecube.daq.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

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
    private ArrayList chartList = new ArrayList();

    ChartGenerator(StatData statData, ChartChoices choices)
    {
        if (statData == null) {
            // do nothing
        } else if (choices.getType() == ChartChoices.SHOW_ALL ||
                   choices.getType() == ChartChoices.SHOW_SELECTED ||
                   choices.getType() == ChartChoices.SHOW_DELTA)
        {
            showMultiple(statData, choices);
        } else if (choices.getType() == ChartChoices.SHOW_COMBINED ||
                   choices.getType() == ChartChoices.SHOW_SCALED)
        {
            showCombined(statData, choices);
        } else {
            throw new Error("Unknown chart type#" + choices.getType());
        }
    }

    private void addChart(String name, TimeSeriesCollection coll,
                          boolean showLegend, boolean showPoints)
    {
        JFreeChart chart = createTimeSeriesChart(name, "Seconds", name, coll,
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
        //ValueAxis timeAxis = new DateAxis(timeAxisLabel,
        //                                  TimeZone.getTimeZone("UTC"));
        ValueAxis timeAxis = new SecondAxis(timeAxisLabel,
                                            TimeZone.getTimeZone("UTC"));
        timeAxis.setLowerMargin(0.02);  // reduce the default margins
        timeAxis.setUpperMargin(0.02);

        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);  // override default

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

                JFreeChart chart = (JFreeChart) chartList.get(idx);
                BufferedImage img =
                    chart.createBufferedImage(colWidth, rowHeight);
                g2d.drawImage(img, c * colWidth, r * rowHeight, null);
            }
        }

        return bigImg;
    }

    private String getSectionTitle(StatData statData, ChartChoices choices)
    {
        boolean rtnVal;

        Collection sections = statData.getSections();
        if (sections == null || sections.size() <= 1) {
            return "No data";
        }

        String title = null;
        Iterator iter = sections.iterator();
        while (iter.hasNext()) {
            String section = (String) iter.next();

            SectionChoices sc = choices.getSection(section);
            if (sc.hasGraphs()) {
                if (title == null) {
                    title = section;
                } else {
                    title = title + "+" + section;
                }
            }
        }

        return title;
    }

    public String getTitle()
    {
        return title;
    }

    private boolean hasMultipleSections(StatData statData,
                                        ChartChoices choices)
    {
        boolean rtnVal;

        Collection sections = statData.getSections();
        if (sections == null || sections.size() <= 1) {
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

            Iterator iter = sections.iterator();
            while (iter.hasNext()) {
                String section = (String) iter.next();

                SectionChoices sc = choices.getSection(section);
                if (sc.hasGraphs()) {
                    if (foundOne) {
                        // if we've found two sections with graphs, we're done
                        rtnVal = true;
                        break;
                    }

                    foundOne = true;
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

        Iterator iter = coll.getSeries().iterator();
        while (iter.hasNext()) {
            TimeSeries series = (TimeSeries) iter.next();

            double prevVal = Double.NaN;

            Iterator sIt = series.getItems().iterator();
            while (sIt.hasNext()) {
                TimeSeriesDataItem item =
                    (TimeSeriesDataItem) sIt.next();

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

        Iterator iter = chartList.iterator();
        while (iter.hasNext()) {
            panel.add(new ChartPanel((JFreeChart) iter.next()));
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

    private void showCombined(StatData statData, ChartChoices choices)
    {
        final boolean scale =
            (choices.getType() == ChartChoices.SHOW_SCALED);

        final boolean multiSection = hasMultipleSections(statData, choices);

        title = getSectionTitle(statData, choices);

        TimeSeriesCollection coll = new TimeSeriesCollection();
        coll.setDomainIsPointsInTime(true);

        int numSeries = 0;

        Iterator sectIter = statData.getSections().iterator();
        while (sectIter.hasNext()) {
            String section = (String) sectIter.next();

            SectionChoices sectionChoice = choices.getSection(section);
            if (sectionChoice == null || !sectionChoice.hasGraphs()) {
                continue;
            }

            int numCharted = 0;
            String chartName = null;

            Iterator nameIter = statData.getSectionNames(section).iterator();
            while (nameIter.hasNext()) {
                String name = (String) nameIter.next();

                if (sectionChoice != null && !sectionChoice.isChosen(name)) {
                    continue;
                }

                StatParent stat = statData.getStatistics(section, name);

                if (!multiSection) {
                    chartName = name;
                }

                if (!scale) {
                    stat.plot(coll, section, name, true);
                } else {
                    stat.plotScaled(coll, section, name);
                }
                numCharted++;
            }

            if (title == null) {
                if (numCharted == 1) {
                    title = chartName;
                } else {
                    title = section;
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

    private void showMultiple(StatData statData, ChartChoices choices)
    {
        final boolean multiSection = hasMultipleSections(statData, choices);

        final boolean showAll = (choices.getType() == ChartChoices.SHOW_ALL);

        final boolean delta =
            (choices.getType() == ChartChoices.SHOW_DELTA);

        title = getSectionTitle(statData, choices);

        Iterator sectIter = statData.getSections().iterator();
        while (sectIter.hasNext()) {
            String section = (String) sectIter.next();

            SectionChoices sectionChoice;
            if (showAll) {
                sectionChoice = null;
            } else {
                sectionChoice = choices.getSection(section);
                if (sectionChoice == null || !sectionChoice.hasGraphs()) {
                    continue;
                }
            }

            int numCharted = 0;
            String chartName = null;

            Iterator nameIter = statData.getSectionNames(section).iterator();
            while (nameIter.hasNext()) {
                String name = (String) nameIter.next();

                if (!showAll && sectionChoice != null &&
                    !sectionChoice.isChosen(name))
                {
                    continue;
                }

                StatParent stat = statData.getStatistics(section, name);

                TimeSeriesCollection coll;
                if (!delta) {
                    coll = stat.plot(section, name, false);
                } else {
                    coll = stat.plotDelta(section, name);
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

                if (multiSection && section != null) {
                    chartName = deltaStr + section + " " + name;
                } else {
                    chartName = deltaStr + name;
                }

                addChart(chartName, coll, showLegend, choices.showPoints());
                numCharted++;
            }

            if (title == null) {
                if (numCharted == 1) {
                    title = chartName;
                } else {
                    title = section;
                }
            }
        }
    }
}
