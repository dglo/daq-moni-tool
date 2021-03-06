package icecube.daq.tools;

import icecube.daq.common.ColoredAppender;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.jfree.ui.RefineryUtilities;

class InstanceBean
    implements Iterable<String>
{
    private String name;
    private List<String> itemNames;
    private SectionKey key;

    private boolean includeAll;
    private List<String> graphNames;

    InstanceBean(String name, List<String> itemNames, SectionKey key)
    {
        this.name = name;
        this.itemNames = itemNames;
        this.key = key;
    }

    public void addGraph(String name)
    {
        if (!itemNames.contains(name)) {
            throw new Error("Bogus graph " + name + " for " + this.name);
        }

        if (graphNames == null) {
            graphNames = new ArrayList<String>();
        }

        graphNames.add(name);
    }

    String getName()
    {
        return name;
    }

    SectionKey getSectionKey()
    {
        return key;
    }

    Iterable<String> graphIterable()
    {
        if (includeAll) {
            return itemNames;
        }

        return graphNames;
    }

    public boolean hasGraphs()
    {
        return includeAll || (graphNames != null && graphNames.size() > 0);
    }

    public boolean isIncludeAll()
    {
        return includeAll;
    }

    public boolean isChosen(String name)
    {
        return includeAll || (graphNames != null && graphNames.contains(name));
    }

    public Iterator<String> iterator()
    {
        return itemNames.iterator();
    }

    boolean matches(String name)
    {
        return this.name.equals(name);
    }

    public void removeGraph(String name)
    {
        if (graphNames != null) {
            graphNames.remove(name);
            if (graphNames.size() == 0) {
                graphNames = null;
            }
        }
    }

    void setIncludeAll(boolean val)
    {
        includeAll = val;
    }

    int size()
    {
        return itemNames.size();
    }

    @Override
    public String toString()
    {
        return name;
    }
}

class ComponentInstance
    implements Iterable<InstanceBean>
{
    private int num;
    private ArrayList<InstanceBean> list;

    ComponentInstance(int num)
    {
        this.num = num;
        this.list = new ArrayList<InstanceBean>();
    }

    InstanceBean create(String name, List<String> itemNames, SectionKey key)
    {
        InstanceBean bean = new InstanceBean(name, itemNames, key);
        list.add(bean);
        return bean;
    }

    InstanceBean get(String name)
    {
        for (InstanceBean ib : list) {
            if (ib.matches(name)) {
                return ib;
            }
        }

        return null;
    }

    int getNumber()
    {
        return num;
    }

    public Iterator<InstanceBean> iterator()
    {
        return list.iterator();
    }

    boolean matches(int num)
    {
        return this.num == num;
    }
}

class ComponentData
    implements Iterable<ComponentInstance>
{
    private static final Logger LOG = Logger.getLogger(ComponentData.class);

    private String name;
    private ArrayList<ComponentInstance> list;

    ComponentData(String name)
    {
        this.name = name;
        this.list = new ArrayList<ComponentInstance>();
    }

    ComponentInstance create(int instNum)
    {
        ComponentInstance inst = new ComponentInstance(instNum);
        list.add(inst);
        return inst;
    }

    public static ArrayList<ComponentData> extract(StatData statData)
    {
        ArrayList<ComponentData> list = new ArrayList<ComponentData>();

        HashMap<String, ComponentData> map =
            new HashMap<String, ComponentData>();

        for (SectionKey key : statData.getSectionKeys()) {
            List<String> names = statData.getSectionNames(key);
            if (names.size() <= 0) {
                continue;
            }

            String compName = key.getComponent();
            int instNum = key.getInstance();
            String beanName = key.getSection();

            ComponentData compData = (ComponentData) map.get(compName);
            if (compData == null) {
                compData = new ComponentData(compName);
                map.put(compName, compData);
                list.add(compData);
            }

            ComponentInstance compInst = compData.get(instNum);
            if (compInst == null) {
                compInst = compData.create(instNum);
            }

            InstanceBean beanData = compInst.get(beanName);
            if (beanData != null) {
                LOG.error("Found multiple instances of component \"" +
                          compName + "\" instance " + instNum + " bean \"" +
                          beanName + "\"");
                continue;
            }

            compInst.create(beanName, names, key);
        }

        return list;
    }

    ComponentInstance get(int instNum)
    {
        for (ComponentInstance ci : list) {
            if (ci.matches(instNum)) {
                return ci;
            }
        }

        return null;
    }

    ComponentInstance getFirst()
    {
        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    String getName()
    {
        return name;
    }

    boolean isSingleInstance()
    {
        return list.size() == 1;
    }

    public Iterator<ComponentInstance> iterator()
    {
        return list.iterator();
    }

    boolean matches(String name)
    {
        return this.name.equals(name);
    }
}

class TypeButtons
{
    private JRadioButton showAllBtn;
    private JRadioButton showSelBtn;
    private ArrayList<IncAllCheckBox> sections =
        new ArrayList<IncAllCheckBox>();

    TypeButtons()
    {
    }

    void addSection(IncAllCheckBox ckbox)
    {
        sections.add(ckbox);
    }

    void clearAll()
    {
        for (IncAllCheckBox ckbox : sections) {
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

abstract class SectionChoicesCheckBox
    extends JCheckBox
    implements ItemListener
{
    private ChartChoices chartChoices;
    private InstanceBean instBean;
    private TypeButtons typeButtons;

    SectionChoicesCheckBox(String name, ChartChoices chartChoices,
                           InstanceBean instBean, TypeButtons typeButtons)
    {
        super(name);

        addItemListener(this);

        this.chartChoices = chartChoices;
        this.instBean = instBean;
        this.typeButtons = typeButtons;
    }

    InstanceBean getBean()
    {
        return instBean;
    }

    public abstract void itemStateChanged(ItemEvent evt);

    void setShowSelected()
    {
        if (typeButtons.disableShowAll()) {
            chartChoices.setType(ChartType.SELECTED);
        }
    }
}

class IncAllCheckBox
    extends SectionChoicesCheckBox
{
    private static final Logger LOG = Logger.getLogger(IncAllCheckBox.class);

    private ArrayList<JCheckBox> list = new ArrayList<JCheckBox>();

    IncAllCheckBox(String name, ChartChoices chartChoices,
                   InstanceBean instBean, TypeButtons typeButtons)
    {
        super(name, chartChoices, instBean, typeButtons);
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
        for (JCheckBox ckbox : list) {
            ckbox.setSelected(false);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getBean().setIncludeAll(true);
            setShowSelected();
            clearIndividual();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getBean().setIncludeAll(false);
        } else {
            LOG.error("Unknown includeAll(" + getBean() + " event #" +
                      evt.getStateChange() + ": " + evt);
        }
    }
}

class NameCheckBox
    extends SectionChoicesCheckBox
{
    private static final Logger LOG = Logger.getLogger(NameCheckBox.class);

    private JCheckBox incAllBox;

    NameCheckBox(String name, ChartChoices chartChoices, InstanceBean instBean,
                 TypeButtons typeButtons, JCheckBox incAllBox)
    {
        super(name, chartChoices, instBean, typeButtons);

        this.incAllBox = incAllBox;
    }

    void clearSectionAll()
    {
        incAllBox.setSelected(false);
    }

    @Override
    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getBean().addGraph(getText());
            setShowSelected();
            clearSectionAll();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getBean().removeGraph(getText());
        } else {
            LOG.error("Unknown sectionName(" + getBean() + " event #" +
                      evt.getStateChange() + ": " + evt);
        }
    }
}

class TemplateCheckBox
    extends JCheckBox
    implements ItemListener
{
    private List<JCheckBox> ckboxList;

    TemplateCheckBox(String name, List<JCheckBox> ckboxList)
    {
        super(name);

        addItemListener(this);

        this.ckboxList = ckboxList;
    }

    @Override
    public void itemStateChanged(ItemEvent evt)
    {
        for (JCheckBox ckbox : ckboxList) {
            if (evt.getStateChange() == ItemEvent.SELECTED &&
                !ckbox.isSelected())
            {
                ckbox.doClick();
            } else if (evt.getStateChange() == ItemEvent.DESELECTED &&
                       ckbox.isSelected())
            {
                ckbox.doClick();
            }
        }
    }
}

public class DAQMoniChart
    extends JFrame
{
    private static final Logger LOG = Logger.getLogger(DAQMoniChart.class);

    private static final String TEMPLATE_TITLE = "All";

    private ChartChoices chartChoices = new ChartChoices();

    private TypeButtons typeButtons = new TypeButtons();
    private JTabbedPane tabbedPane = new JTabbedPane();

    public DAQMoniChart(ArrayList<ComponentData> compList, StatData statData)
    {
        setTitle("Monitoring Charts");
        setLayout(new BorderLayout());

        add(buildBottom(), BorderLayout.CENTER);
        add(buildCommands(compList, statData), BorderLayout.PAGE_END);

        loadTabbedPane(compList);

        resize(640, 480);
        show();
    }

    private void addBeanPanel(JTabbedPane pane, ComponentInstance compInst)
    {
        for (InstanceBean instBean : compInst) {
            final int cols = 3;
            final int rows = (instBean.size() + cols - 1) / cols;

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            IncAllCheckBox includeAll =
                new IncAllCheckBox("Graph all " + instBean.getName() +
                                   " statistics", chartChoices, instBean,
                                   typeButtons);
            panel.add(includeAll, BorderLayout.PAGE_START);
            typeButtons.addSection(includeAll);

            JPanel gridPanel = new JPanel();
            gridPanel.setLayout(new GridLayout(rows, cols));

            for (String name : instBean) {
                JCheckBox ckbox =
                    new NameCheckBox(name, chartChoices, instBean, typeButtons,
                                     includeAll);
                gridPanel.add(ckbox);

                includeAll.addIndividual(ckbox);
            }

            panel.add(gridPanel, BorderLayout.CENTER);

            pane.addTab(instBean.getName(), panel);
        }
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
                    chartChoices.setType(ChartType.ALL);
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
                    chartChoices.setType(ChartType.SELECTED);
                }
            });
        panel.add(showSelBtn);
        group.add(showSelBtn);
        typeButtons.setShowSelected(showSelBtn);

        JRadioButton deltaSelBtn = new JRadioButton("Delta");
        deltaSelBtn.setToolTipText("Graph changes to selected statistics" +
                                     " in separate graphs");
        deltaSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartType.DELTA);
                }
            });
        panel.add(deltaSelBtn);
        group.add(deltaSelBtn);

        JRadioButton combineSelBtn = new JRadioButton("Combined");
        combineSelBtn.setToolTipText("Combine selected statistics in" +
                                     " a single graph");
        combineSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartType.COMBINED);
                }
            });
        panel.add(combineSelBtn);
        group.add(combineSelBtn);

        JRadioButton scaleSelBtn = new JRadioButton("Scaled");
        scaleSelBtn.setToolTipText("Combine selected statistics in" +
                                     " a single scaled graph");
        scaleSelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartType.SCALED);
                }
            });
        panel.add(scaleSelBtn);
        group.add(scaleSelBtn);

        JRadioButton scaleLogBtn = new JRadioButton("Logarithmic");
        scaleLogBtn.setToolTipText("Combine selected statistics in" +
                                     " a single logarithmic graph");
        scaleLogBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    chartChoices.setType(ChartType.LOGARITHMIC);
                }
            });
        panel.add(scaleLogBtn);
        group.add(scaleLogBtn);

        showAllBtn.setSelected(true);
        chartChoices.setType(ChartType.ALL);

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
                        LOG.error("Unknown showPoints event #" +
                                  evt.getStateChange() + ": " + evt);
                    }
                }
            });
        panel.add(showPointsCkbox);

        JCheckBox hideLegendsCkbox = new JCheckBox("Hide legends");
        hideLegendsCkbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt)
                {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        chartChoices.setHideLegends(true);
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        chartChoices.setHideLegends(false);
                    } else {
                        LOG.error("Unknown hideLegend event #" +
                                  evt.getStateChange() + ": " + evt);
                    }
                }
            });
        panel.add(hideLegendsCkbox);

        JCheckBox filterBoringCkbox = new JCheckBox("Filter uninteresting");
        filterBoringCkbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt)
                {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        chartChoices.setFilterBoring(true);
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        chartChoices.setFilterBoring(false);
                    } else {
                        LOG.error("Unknown filterBoring event #" +
                                  evt.getStateChange() + ": " + evt);
                    }
                }
            });
        panel.add(filterBoringCkbox);

        return panel;
    }

    private Component buildCommands(final List<ComponentData> compList,
                                    final StatData statData)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JButton drawGraphs = new JButton("Draw graphs");
        drawGraphs.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    ChartGenerator chartGen =
                        new ChartGenerator(compList, statData, chartChoices);

                    if (chartGen.isEmpty()) {
                        LOG.error("No data found!");
                    } else {
                        String title = chartGen.getTitle();
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

    private void fillTemplatePanel(JTabbedPane topPane, JTabbedPane pane)
    {
        HashMap<String, Map<String, List<JCheckBox>>> map =
            new HashMap<String, Map<String, List<JCheckBox>>>();

        for (int i = 0; i < topPane.getTabCount(); i++) {
            if (topPane.getTitleAt(i).equals(TEMPLATE_TITLE)) {
                continue;
            }

            JTabbedPane instPane = (JTabbedPane) topPane.getComponentAt(i);
            for (int j = 0; j < instPane.getTabCount(); j++) {
                final String title = instPane.getTitleAt(j);

                Map<String, List<JCheckBox>> nameMap = map.get(title);
                if (nameMap == null) {
                    nameMap = new HashMap<String, List<JCheckBox>>();
                    map.put(title, nameMap);
                }

                JPanel subPane = (JPanel) instPane.getComponentAt(j);
                if (subPane.getComponentCount() != 2) {
                    LOG.error("Unexpected number of components in " +
                              pane.getTitleAt(i) + "-" +
                              instPane.getTitleAt(j) + " panel");
                    continue;
                }

                JPanel boxPane = (JPanel) subPane.getComponent(1);
                for (int k = 0; k < boxPane.getComponentCount(); k++) {
                    NameCheckBox ckBox =
                        (NameCheckBox) boxPane.getComponent(k);

                    String label = ckBox.getText();

                    List<JCheckBox> ckboxList = nameMap.get(label);
                    if (ckboxList == null) {
                        ckboxList = new ArrayList<JCheckBox>();
                        nameMap.put(label, ckboxList);
                    }

                    ckboxList.add(ckBox);
                }
            }
        }

        List<String> beanNames = new ArrayList<String>(map.keySet());
        Collections.sort(beanNames);

        for (String beanName : beanNames) {

            Map<String, List<JCheckBox>> nameMap = map.get(beanName);

            List<String> names = new ArrayList<String>(nameMap.keySet());
            Collections.sort(names);

            final int cols = 3;
            final int rows = (names.size() + cols - 1) / cols;

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(rows, cols));

            for (String name : names) {
                List<JCheckBox> list = nameMap.get(name);

                JCheckBox ckbox = new TemplateCheckBox(name, list);
                panel.add(ckbox);
            }

            pane.addTab(beanName, panel);
        }
    }

    private JTabbedPane getPane(JTabbedPane tabbedPane, String name)
    {
        final int paneIdx = tabbedPane.indexOfTab(name);
        if (paneIdx >= 0) {
            return (JTabbedPane) tabbedPane.getComponentAt(paneIdx);
        }

        JTabbedPane pane = new JTabbedPane();
        tabbedPane.addTab(name, pane);

        return pane;
    }

    private void loadTabbedPane(ArrayList<ComponentData> compList)
    {
        while (tabbedPane.getTabCount() > 0) {
            tabbedPane.removeTabAt(0);
        }

        for (ComponentData compData : compList) {
            JTabbedPane compPane = getPane(tabbedPane, compData.getName());

            if (compData.isSingleInstance()) {
                ComponentInstance compInst = compData.getFirst();

                addBeanPanel(compPane, compInst);
            } else {
                JTabbedPane allPane = new JTabbedPane();
                compPane.addTab(TEMPLATE_TITLE, allPane);

                for (ComponentInstance compInst : compData) {
                    JTabbedPane instPane = new JTabbedPane();
                    compPane.addTab(Integer.toString(compInst.getNumber()),
                                      instPane);

                    addBeanPanel(instPane, compInst);
                }

                fillTemplatePanel(compPane, allPane);
            }
        }

        repaint();
    }

    private void popupAlert(String[] msgLines)
    {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < msgLines.length; i++) {
            if (i > 0) {
                buf.append(" / ");
            }

            buf.append(msgLines[i]);
        }

        LOG.error(buf.toString());
    }

    public static void main(String[] args)
    {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new ColoredAppender());

        boolean omitDataCollector = false;
        boolean verbose = false;
        List<File> fileList = new ArrayList<File>();

        boolean usage = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].length() == 0) {
                System.err.println("Ignoring empty argument");
                continue;
            }

            if (args[i].charAt(0) == '-') {
                boolean badArg = false;
                if (args[i].length() == 1) {
                    badArg = true;
                } else {
                    switch (args[i].charAt(1)) {
                    case 'o':
                        omitDataCollector = true;
                        break;
                    case 'v':
                        verbose = true;
                        break;
                    default:
                        badArg = true;
                        break;
                    }
                }

                if (badArg) {
                    System.err.format("Bad argument '%s'\n", args[i]);
                    usage = true;
                }

                continue;
            }

            // add statistics from file
            File f = new File(args[i]);
            if (!f.exists()) {
                System.err.println("No such file \"" + f + "\"");
            } else {
                fileList.add(f);
            }
        }

        if (fileList.size() == 0) {
            System.err.println("No data files found!");
            usage = true;
        }

        if (usage) {
            final String msg =
                String.format("Usage: %s [-o(mitDataCollector)]" +
                              " file.moni [file.moni ...]",
                              DAQMoniChart.class.getName());
            throw new Error(msg);
        }

        StatData statData = new StatData();
        for (File file : fileList) {
            statData.loadFile(file, omitDataCollector, verbose);
        }

        // reorganize some data
        statData.transform();

        ArrayList<ComponentData> compList = ComponentData.extract(statData);

        new DAQMoniChart(compList, statData);
    }
}
