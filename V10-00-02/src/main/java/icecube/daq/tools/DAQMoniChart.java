package icecube.daq.tools;

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

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import org.jfree.ui.RefineryUtilities;

class InstanceBean
{
    private String name;
    private List itemNames;
    private String sectionName;

    private boolean includeAll;
    private List graphNames;

    InstanceBean(String name, List itemNames, String sectionName)
    {
        this.name = name;
        this.itemNames = itemNames;
        this.sectionName = sectionName;
    }

    public void addGraph(String name)
    {
        if (!itemNames.contains(name)) {
            throw new Error("Bogus graph " + name + " for " + this.name);
        }

        if (graphNames == null) {
            graphNames = new ArrayList();
        }

        graphNames.add(name);
    }

    String getName()
    {
        return name;
    }

    String getSectionName()
    {
        return sectionName;
    }

    public boolean hasGraphs()
    {
        return graphNames != null && (includeAll || graphNames.size() > 0);
    }

    public boolean isIncludeAll()
    {
        return includeAll;
    }

    public boolean isChosen(String name)
    {
        return graphNames != null && (includeAll || graphNames.contains(name));
    }

    List list()
    {
        return itemNames;
    }

    List listGraph()
    {
        return graphNames;
    }

    boolean matches(String name)
    {
        return this.name.equals(name);
    }

    public void removeGraph(String name)
    {
        if (graphNames != null) {
            graphNames.remove(name);
        }
    }

    void setIncludeAll(boolean val)
    {
        includeAll = val;
    }

    public String toString()
    {
        return name;
    }
}

class ComponentInstance
{
    private int num;
    private ArrayList<InstanceBean> list;

    ComponentInstance(int num)
    {
        this.num = num;
        this.list = new ArrayList<InstanceBean>();
    }

    InstanceBean create(String name, List itemNames, String sectionName)
    {
        InstanceBean bean = new InstanceBean(name, itemNames, sectionName);
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

    List<InstanceBean> list()
    {
        return list;
    }

    boolean matches(int num)
    {
        return this.num == num;
    }
}

class ComponentData
{
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

    ComponentInstance get(int instNum)
    {
        for (ComponentInstance ci : list) {
            if (ci.matches(instNum)) {
                return ci;
            }
        }

        return null;
    }

    String getName()
    {
        return name;
    }

    boolean isSingleInstance()
    {
        return list.size() == 1;
    }

    List<ComponentInstance> list()
    {
        return list;
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
    private ArrayList sections = new ArrayList();

    TypeButtons()
    {
    }

    void addSection(IncAllCheckBox ckbox)
    {
        sections.add(ckbox);
    }

    void clearAll()
    {
        Iterator iter = sections.iterator();
        while (iter.hasNext()) {
            IncAllCheckBox ckbox = (IncAllCheckBox) iter.next();
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
            chartChoices.setType(ChartChoices.SHOW_SELECTED);
        }
    }
}

class IncAllCheckBox
    extends SectionChoicesCheckBox
{
    private ArrayList list = new ArrayList();

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
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            JCheckBox ckbox = (JCheckBox) iter.next();
            ckbox.setSelected(false);
        }
    }

    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getBean().setIncludeAll(true);
            setShowSelected();
            clearIndividual();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getBean().setIncludeAll(false);
        } else {
            System.err.println("Unknown includeAll(" +
                               getBean() + " event #" +
                               evt.getStateChange() + ": " + evt);
        }
    }
}

class NameCheckBox
    extends SectionChoicesCheckBox
{
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

    public void itemStateChanged(ItemEvent evt)
    {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            getBean().addGraph(getText());
            setShowSelected();
            clearSectionAll();
        } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
            getBean().removeGraph(getText());
        } else {
            System.err.println("Unknown sectionName(" +
                               getBean() + " event #" +
                               evt.getStateChange() + ": " + evt);
        }
    }
}

class TemplateCheckBox
    extends JCheckBox
    implements ItemListener
{
    private List ckboxList;

    TemplateCheckBox(String name, List ckboxList)
    {
        super(name);

        addItemListener(this);

        this.ckboxList = ckboxList;
    }

    public void itemStateChanged(ItemEvent evt)
    {
        Iterator iter = ckboxList.iterator();
        while (iter.hasNext()) {
            NameCheckBox ckbox = (NameCheckBox) iter.next();
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
    private static final String TEMPLATE_TITLE = "All";

    private ChartChoices chartChoices = new ChartChoices();

    private StatData statData;

    private TypeButtons typeButtons = new TypeButtons();
    private JTabbedPane tabbedPane = new JTabbedPane();

    public DAQMoniChart(String[] args)
    {
        for (int i = 0; i < args.length; i++) {
            File f = new File(args[i]);
            if (!f.exists()) {
                System.err.println("No such file \"" + f + "\"");
                continue;
            }

            if (statData == null) {
                statData = new StatData();
            }

            try {
                statData.addData(new GraphSource(f));
            } catch (IOException ioe) {
                System.err.println("Couldn't load \"" + f + "\":");
                ioe.printStackTrace();
                continue;
            }

            System.out.println(f + ":");
        }

        if (statData == null) {
            throw new Error("No data files found!");
        }

        setTitle("Monitoring Charts");
        setLayout(new BorderLayout());

        ArrayList<ComponentData> compList = extractComponentData(statData);

        add(buildBottom(), BorderLayout.CENTER);
        add(buildCommands(compList, statData), BorderLayout.PAGE_END);

        loadTabbedPane(compList);

        resize(640, 480);
        show();
    }

    private void addBeanPanel(JTabbedPane pane, ComponentInstance compInst)
    {
        for (InstanceBean instBean : compInst.list()) {
            List names = instBean.list();

            final int cols = 3;
            final int rows = (names.size() + cols - 1) / cols;

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

            Iterator iter = names.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();

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

        JCheckBox hideLegendsCkbox = new JCheckBox("Hide legends");
        hideLegendsCkbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent evt)
                {
                    if (evt.getStateChange() == ItemEvent.SELECTED) {
                        chartChoices.setHideLegends(true);
                    } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
                        chartChoices.setHideLegends(false);
                    } else {
                        logMessage("Unknown hideLegend event #" +
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
                        logMessage("Unknown filterBoring event #" +
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
                        logMessage("No data found!");
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

    private ArrayList<ComponentData> extractComponentData(StatData statData)
    {
        ArrayList<ComponentData> list = new ArrayList<ComponentData>();

        HashMap map = new HashMap();

        Iterator sectIter = statData.getSections().iterator();
        while (sectIter.hasNext()) {
            String section = (String) sectIter.next();

            List names = statData.getSectionNames(section);
            if (names.size() <= 0) {
                continue;
            }

            final int minusIdx = section.indexOf('-');
            final int colonIdx = section.indexOf(':', minusIdx + 1);
            if (colonIdx < 0 || minusIdx < 0) {
                System.err.println("Bad section name \"" + section + "\"");
                continue;
            }

            String compName = section.substring(0, minusIdx);
            String beanName = section.substring(colonIdx + 1);

            int instNum;
            if (minusIdx == colonIdx - 2 &&
                section.charAt(minusIdx + 1) == '0')
            {
                instNum = 0;
            } else {
                String numStr = section.substring(minusIdx + 1, colonIdx);
                try {
                    instNum = Integer.parseInt(numStr);
                } catch (NumberFormatException nfe) {
                    System.err.println("Bad instance number \"" + numStr +
                                       "\" in section \"" + section + "\"");
                    continue;
                }
            }

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
                System.err.println("Found multiple instances of component \"" +
                                   compName + "\" instance " + instNum +
                                   " bean \"" + beanName + "\"");
                continue;
            }

            compInst.create(beanName, names, section);
        }

        return list;
    }

    private void fillTemplatePanel(JTabbedPane topPane, JTabbedPane pane)
    {
        HashMap map = new HashMap();

        for (int i = 0; i < topPane.getTabCount(); i++) {
            if (topPane.getTitleAt(i).equals(TEMPLATE_TITLE)) {
                continue;
            }

            JTabbedPane instPane = (JTabbedPane) topPane.getComponentAt(i);
            for (int j = 0; j < instPane.getTabCount(); j++) {
                final String title = instPane.getTitleAt(j);

                HashMap nameMap = (HashMap) map.get(title);
                if (nameMap == null) {
                    nameMap = new HashMap();
                    map.put(title, nameMap);
                }

                JPanel subPane = (JPanel) instPane.getComponentAt(j);
                if (subPane.getComponentCount() != 2) {
                    System.err.println("Unexpected number of components in " +
                                       pane.getTitleAt(i) + "-" +
                                       instPane.getTitleAt(j) + " panel");
                    continue;
                }

                JPanel boxPane = (JPanel) subPane.getComponent(1);
                for (int k = 0; k < boxPane.getComponentCount(); k++) {
                    NameCheckBox ckBox =
                        (NameCheckBox) boxPane.getComponent(k);

                    String label = ckBox.getText();

                    List ckboxList = (List) nameMap.get(label);
                    if (ckboxList == null) {
                        ckboxList = new ArrayList();
                        nameMap.put(label, ckboxList);
                    }

                    ckboxList.add(ckBox);
                }
            }
        }

        List beanNames = new ArrayList(map.keySet());
        Collections.sort(beanNames);

        Iterator beanIter = beanNames.iterator();
        while (beanIter.hasNext()) {
            final String beanName = (String) beanIter.next();

            HashMap nameMap = (HashMap) map.get(beanName);

            List names = new ArrayList((nameMap).keySet());
            Collections.sort(names);

            final int cols = 3;
            final int rows = (names.size() + cols - 1) / cols;

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(rows, cols));

            Iterator iter = names.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();

                List list = (List) nameMap.get(name);

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
                ComponentInstance compInst = compData.list().get(0);

                addBeanPanel(compPane, compInst);
            } else {
                JTabbedPane allPane = new JTabbedPane();
                compPane.addTab(TEMPLATE_TITLE, allPane);

                for (ComponentInstance compInst : compData.list()) {
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

    private void logMessage(String msg)
    {
        System.out.println(msg);
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

    public static void main(String[] args)
    {
        new DAQMoniChart(args);
    }
}
