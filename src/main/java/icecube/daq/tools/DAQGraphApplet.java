package icecube.daq.tools;

import java.applet.Applet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import org.jfree.ui.RefineryUtilities;

class OLDTypeButtons
{
    private JRadioButton showAllBtn;
    private JRadioButton showSelBtn;
    private ArrayList sections = new ArrayList();

    OLDTypeButtons()
    {
    }

    void addSection(OLDIncAllCheckBox ckbox)
    {
        sections.add(ckbox);
    }

    void clearAll()
    {
        Iterator iter = sections.iterator();
        while (iter.hasNext()) {
            OLDIncAllCheckBox ckbox = (OLDIncAllCheckBox) iter.next();
            ckbox.clearAll();
        }
    }

    boolean disableShowAll()
    {
        if (showAllBtn.getSelectedObjects() == null) {
            return false;
        }

        showAllBtn.setSelected(false);
        showSelBtn.setSelected(true);
        return true;
    }

    void setShowAll(JRadioButton btn)
    {
        showAllBtn = btn;
    }

    void setShowSelected(JRadioButton btn)
    {
        showSelBtn = btn;
    }
}

abstract class OLDSectionChoicesCheckBox
    extends JCheckBox
    implements ItemListener
{
    private ChartChoices chartChoices;
    private SectionChoices choices;
    private OLDTypeButtons typeButtons;

    OLDSectionChoicesCheckBox(String name, ChartChoices chartChoices,
                              SectionChoices choices,
                              OLDTypeButtons typeButtons)
    {
        super(name);

        addItemListener(this);

        this.chartChoices = chartChoices;
        this.choices = choices;
        this.typeButtons = typeButtons;
    }

    SectionChoices getChoices()
    {
        return choices;
    }

    public abstract void itemStateChanged(ItemEvent evt);

    void setShowSelected()
    {
        if (typeButtons.disableShowAll()) {
            chartChoices.setType(ChartChoices.SHOW_SELECTED);
        }
    }
}

class OLDIncAllCheckBox
    extends OLDSectionChoicesCheckBox
{
    ArrayList list = new ArrayList();

    OLDIncAllCheckBox(String name, ChartChoices chartChoices,
                      SectionChoices choices, OLDTypeButtons typeButtons)
    {
        super(name, chartChoices, choices, typeButtons);
    }

    void addIndividual(JCheckBox ckbox)
    {
        list.add(ckbox);
    }

    void clearAll()
    {
        setSelected(false);
        clearIndividual();
    }

    void clearIndividual()
    {
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            JCheckBox ckbox = (JCheckBox) iter.next();
            ckbox.setSelected(false);
        }
    }

    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getChoices().setIncludeAll(true);
            setShowSelected();
            clearIndividual();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getChoices().setIncludeAll(false);
        } else {
            System.err.println("Unknown includeAll(" +
                               getChoices().getSection() + " event #" +
                               evt.getStateChange() + ": " + evt);
        }
    }
}

class OLDNameCheckBox
    extends OLDSectionChoicesCheckBox
{
    private JCheckBox incAllBox;

    OLDNameCheckBox(String name, ChartChoices chartChoices,
                    SectionChoices choices, OLDTypeButtons typeButtons,
                    JCheckBox incAllBox)
    {
        super(name, chartChoices, choices, typeButtons);

        this.incAllBox = incAllBox;
    }

    void clearSectionAll()
    {
        incAllBox.setSelected(false);
    }

    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getChoices().add(getText());
            setShowSelected();
            clearSectionAll();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getChoices().remove(getText());
        } else {
            System.err.println("Unknown sectionName(" +
                               getChoices().getSection() + " event #" +
                               evt.getStateChange() + ": " + evt);
        }
    }
}

public class DAQGraphApplet
    extends Applet
{
    private static final String[] DATA_DIR_NAME = new String[] {
        "/data/jacobsen/crashlogs-spts/",
        "/tmp/",
        "/Users/dglo/Desktop/",
    };

    private static final boolean ADD_DUMP_BUTTON = false;

    private ChartChoices chartChoices = new ChartChoices();

    private GraphSource inputSrc;
    private StatData statData;

    private OLDTypeButtons typeButtons = new OLDTypeButtons();
    private JTabbedPane tabbedPane = new JTabbedPane();

    private Component buildMenu(Object[] dataFiles)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JLabel menuLabel = new JLabel("File to graph:", JLabel.TRAILING);
        panel.add(menuLabel);

        if (dataFiles.length > 1) {
            JComboBox fileMenu = new JComboBox(dataFiles);
            fileMenu.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent evt)
                    {
                        if (evt.getStateChange() == ItemEvent.SELECTED) {
                            setChosen(new GraphSource((File) evt.getItem()));
                        } else if (evt.getStateChange() !=
                                   ItemEvent.DESELECTED)
                        {
                            logMessage("Unknown file menu event #" +
                                       evt.getStateChange() + ": " + evt);
                        }
                    }
                });
            panel.add(fileMenu);

            menuLabel.setLabelFor(fileMenu);

            setChosen(new GraphSource((File) fileMenu.getSelectedItem()));
        } else {
            String fileStr;
            if (dataFiles[0] instanceof URL) {
                fileStr = new File(((URL) dataFiles[0]).getFile()).getName();
                setChosen(new GraphSource((URL) dataFiles[0]));
            } else {
                fileStr = dataFiles[0].toString();
                setChosen(new GraphSource((File) dataFiles[0]));
            }

            JLabel fileLabel = new JLabel(fileStr, JLabel.LEADING);
            panel.add(fileLabel);

        }

        return panel;
    }

    private Component buildBottom()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        buttonPanel.add(buildBottomOptions(), BorderLayout.PAGE_START);
        buttonPanel.add(buildBottomMainTypes(), BorderLayout.PAGE_END);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(buttonPanel, BorderLayout.PAGE_START);
        panel.add(buildBottomDetail(), BorderLayout.CENTER);

        return panel;
    }

    private Component buildBottomDetail()
    {
        return tabbedPane;
    }

    private Component buildBottomMainTypes()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        ButtonGroup group = new ButtonGroup();

        JRadioButton showAllBtn = new JRadioButton("Graph all");
        showAllBtn.setToolTipText("Graph all statistics in separate graphs");
        showAllBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartChoices.SHOW_ALL);
                    typeButtons.clearAll();
                }
            });
        panel.add(showAllBtn);
        group.add(showAllBtn);
        typeButtons.setShowAll(showAllBtn);

        JRadioButton showSelBtn = new JRadioButton("Graph selected");
        showSelBtn.setToolTipText("Graph selected statistics" +
                                  " in separate graphs");
        showSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartChoices.SHOW_SELECTED);
                }
            });
        panel.add(showSelBtn);
        group.add(showSelBtn);
        typeButtons.setShowSelected(showSelBtn);

        JRadioButton deltaSelBtn = new JRadioButton("Delta selected");
        deltaSelBtn.setToolTipText("Graph changes to selected statistics" +
                                     " in separate graphs");
        deltaSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartChoices.SHOW_DELTA);
                }
            });
        panel.add(deltaSelBtn);
        group.add(deltaSelBtn);

        JRadioButton combineSelBtn = new JRadioButton("Combine selected");
        combineSelBtn.setToolTipText("Combine selected statistics in" +
                                     " a single graph");
        combineSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartChoices.SHOW_COMBINED);
                }
            });
        panel.add(combineSelBtn);
        group.add(combineSelBtn);

        JRadioButton scaleSelBtn = new JRadioButton("Combine scaled");
        scaleSelBtn.setToolTipText("Combine selected statistics in" +
                                     " a single scaled graph");
        scaleSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartChoices.SHOW_SCALED);
                }
            });
        panel.add(scaleSelBtn);
        group.add(scaleSelBtn);

        showAllBtn.setSelected(true);
        chartChoices.setType(ChartChoices.SHOW_ALL);

        return panel;
    }

    private Component buildBottomOptions()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JCheckBox showPointsCkbox = new JCheckBox("Show points");
        showPointsCkbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt)
                {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        chartChoices.setShowPoints(true);
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        chartChoices.setShowPoints(false);
                    } else {
                        logMessage("Unknown showPoints event #" +
                                   evt.getStateChange() + ": " + evt);
                    }
                }
            });
        panel.add(showPointsCkbox);

        JCheckBox filterBoringCkbox = new JCheckBox("Filter uninteresting");
        filterBoringCkbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt)
                {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        chartChoices.setFilterBoring(true);
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        chartChoices.setFilterBoring(false);
                    } else {
                        logMessage("Unknown filterBoring event #" +
                                   evt.getStateChange() + ": " + evt);
                    }
                }
            });
        panel.add(filterBoringCkbox);

        return panel;
    }

    private Component buildCommands()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        if (ADD_DUMP_BUTTON) {
            JButton dumpState = new JButton("Dump state");
            dumpState.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt)
                    {
                        System.out.println("Input Source: " + inputSrc);
                        chartChoices.dump();
                    }
                });
            panel.add(dumpState);
        }

        JButton drawGraphs = new JButton("Draw graphs");
        drawGraphs.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    ChartGenerator chartGen =
                        new ChartGenerator(statData, chartChoices);

                    if (chartGen.isEmpty()) {
                        logMessage("No data found!");
                    } else {
                        String title = chartGen.makeTitle(inputSrc);
                        GraphFrame frame = new GraphFrame(title);
                        frame.setContentPane(chartGen.layout());

                        frame.pack();
                        RefineryUtilities.centerFrameOnScreen(frame);
                        frame.setVisible(true);
                    }
                }
            });
        panel.add(drawGraphs);

        return panel;
    }

    private File[] findDataFiles()
    {
        ArrayList list = new ArrayList();

        for (int i = 0; i < DATA_DIR_NAME.length; i++) {
            File dataDir = new File(DATA_DIR_NAME[i]);
            if (!dataDir.isDirectory()) {
                continue;
            }

            File[] dataFiles = dataDir.listFiles();
            for (int f = 0; f < dataFiles.length; f++) {
                String name = dataFiles[f].getName();
                if (name.startsWith("eblog.") && name.endsWith(".log")) {
                    list.add(dataFiles[f]);
                }
            }

            if (list.size() > 0) {
                return (File[]) list.toArray(new File[list.size()]);
            }
        }

        return null;
    }

    public void init()
    {
        Object[] dataFiles;

        String fileName = getParameter("datafile");
        if (fileName == null) {
            dataFiles = findDataFiles();
        } else {
            URL url;
            try {
                dataFiles = new URL[] { new URL(fileName) };
            } catch (MalformedURLException muex) {
                throw new Error("Bad URL \"" + fileName + "\"", muex);
            }
        }

        if (dataFiles == null) {
            throw new Error("No data files found!");
        }

        setLayout(new BorderLayout());

        add(buildMenu(dataFiles), BorderLayout.PAGE_START);
        add(buildBottom(), BorderLayout.CENTER);
        add(buildCommands(), BorderLayout.PAGE_END);
    }

    private void loadTabbedPane()
    {
        while (tabbedPane.getTabCount() > 0) {
            tabbedPane.removeTabAt(0);
        }

        if (statData == null) {
            return;
        }

        chartChoices.clearSections();
        chartChoices.initialize(statData);

        Iterator sectIter = statData.getSections().iterator();
        while (sectIter.hasNext()) {
            String section = (String) sectIter.next();

            List names = statData.getSectionNames(section);
            if (names.size() > 0) {
                final int cols = 3;
                final int rows = (names.size() + cols - 1) / cols;

                SectionChoices sectionChoices =
                    chartChoices.getSection(section);

                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());

                OLDIncAllCheckBox includeAll =
                    new OLDIncAllCheckBox("Graph all " + section +
                                          " statistics", chartChoices,
                                          sectionChoices, typeButtons);
                panel.add(includeAll, BorderLayout.PAGE_START);
                typeButtons.addSection(includeAll);

                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new GridLayout(rows, cols));

                Iterator iter2 = names.iterator();
                while (iter2.hasNext()) {
                    String name = (String) iter2.next();

                    JCheckBox ckbox =
                        new OLDNameCheckBox(name, chartChoices, sectionChoices,
                                            typeButtons, includeAll);
                    gridPanel.add(ckbox);

                    includeAll.addIndividual(ckbox);
                }

                panel.add(gridPanel, BorderLayout.CENTER);

                tabbedPane.addTab(section, panel);
            }
        }

        repaint();
    }

    private void logMessage(String msg)
    {
        getAppletContext().showStatus(msg);
    }

    private void popupAlert(String[] msgLines)
    {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < msgLines.length; i++) {
            if (i > 0) {
                buf.append(" / ");
            }

            buf.append(msgLines[i]);
        }

        logMessage(buf.toString());
    }

    private void setChosen(GraphSource src)
    {
        inputSrc = src;

        if (statData == null) {
            statData = new StatData();
        }

        try {
            statData.addData(inputSrc);
        } catch (IOException ioex) {
            statData = null;
            popupAlert(new String[] {
                    "Couldn't get statistics from \"" + inputSrc + "\"",
                    ioex.getMessage()
                });
        }

        loadTabbedPane();
    }

    public void stop()
    {
    }

    public static void main(String[] args)
    {
        HashMap params;
        if (args.length == 0) {
            params = null;
        } else if (args.length == 0) {
            File argFile = new File(args[0]);
            if (!argFile.isFile()) {
                System.err.println("\"" + args[0] + "\" is not a valid file!");
                params = null;
            } else {
                try {
                    params = new AppletParams(args[0]);
                } catch (IOException ioe) {
                    try {
                        URL url = argFile.toURL();

                        params = new HashMap();
                        params.put("datafile", url.toString());
                    } catch (MalformedURLException muex) {
                        System.err.println("Could not build URL for \"" +
                                           args[0] + "\": " +
                                           muex.getMessage());
                        params = null;
                    }
                }
            }
        } else {
            params = new HashMap();

            for (int i = 0; i < args.length; i++) {
                File argFile = new File(args[i]);
                if (!argFile.isFile()) {
                    System.err.println("\"" + args[i] +
                                       "\" is not a valid file!");
                    continue;
                }

                URL url;
                try {
                    url = argFile.toURL();
                } catch (MalformedURLException muex) {
                    System.err.println("Could not build URL for \"" + argFile +
                                       "\": " + muex.getMessage());
                    continue;
                }

                params.put("datafile", url.toString());
            }
        }

        new AppletFrame(new DAQGraphApplet(), 640, 480, params);
    }
}
