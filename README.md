Charting the DAQ monitoring values
==================================


Building the tool
-----------------
`daq-moni-tool` is not built as part of the normal pDAQ build so you first
need to build the code.  In the `daq-moni-tool` directory, run:

    mvn clean install

This builds the project and installs it in your local Maven repository


Extracting data
---------------
After the software is built, you need some data to view.  The easiest way to
get the data for a run is to bring up the run's I3Live page in your browser
then find the "download complete tarball" link (within the "pDAQ Details"
section) and save the tarball file to your local machine.

The pDAQ monitoring data is buried a couple of layers inside the downloaded
tarball.  The `unpack-download` script in the top-level daq-moni-tool
directory will unpack the file and remove the extra stuff:

    ./unpack-download ~/Downloads/SPS-pDAQ-run-121212.tar.gz

This will leave you with a `daqrun121212` subdirectory.

You can also cut out the middle man and grab recent log/moni files from
expcont:/mnt/data/pdaq/log on the South Pole System.

Running `chart`
---------------

FINALLY you're ready to chart some data!  The `chart` utility script will read
in all the specified files/directories and pop up a window you can use to
choose which quantities to display.

`chart` is currently pretty stupid, so if you run:

    ./chart daqrun121212

you will see MANY error messages as the program tries to read monitoring data
from the component log files.  To avoid this, explicitly specify the pDAQ
monitoring files:

    ./chart daqrun121212/*.moni

Long runs can produce a LOT of data, so you might even want to limit your data
set to only the components you're interested in:

    ./chart daqrun121212/inIceTrigger-0.moni daqrun121212/eventBuilder-0.moni


pDAQ monitoring files
---------------------
The monitoring files are produced by a thread within the pDAQ control server
which polls all the components at a regular interval for all the monitoring
data they can supply.

Each component's data is saved to a separate file.  Components are made up
of several subsystems, and each subsystem contains one or more quantities.
The output looks something like this:

    foo: 2012-02-12 01:23:45.678900:
        value1: 17
        list1: [12, 34, 56]
        dict1: {"abc": 54, "def": 32}

NOTE: the `chart` program doesn't know how to display that last format.  It'll
      present you with a "dict1" option but the resulting chart will be empty.

The `chart` program will organize this data into several levels, but it will
also group together similarly named files.

A single file like `inIceTrigger-0.moni` will be organized into an
"inIceTrigger" layer, a set of subsystem layers (like "jvm", "manager",
"stringHit", "system" and "trigger") and then individual data quantity choices.

A set of files like "stringHub-1.moni", "stringHub-2.moni", etc. will have an
intermediate layer where you can choose "All" stringHub components or choose
the single hub you're interested in.

Main window choices
-------------------
The main window allows you to choose which data you'd like to display and
how it should be displayed.

The top row of choices are:

* Show points - mark the points on each when the data sample was taken
* Hide legends - plots with multiple lines display a legend describing each line.
                 Choosing this option hides that legend.
* Filter uninteresting - omit charts for unchanging data
You can select of disable as many of these choices as you'd like.

The second row allows you to select how the data is displayed:

* Graph all - present charts for every piece of data.  This is almost always
              the wrong choice since the number of charts is overwhelming.
* Graph selected - only present charts of data which is selected.
* Delta selected - the value used to plot point N is N(t) - N(t-1)
* Combine selected - draw all selected values together in a single chart
* Combine scaled - overlay several charts, mapping them onto a scale from 0.0-1.0

The third row allows you to specify the component (or file)

The fourth row (or fifth row for components with multiple instances) allows you
to flip between subsystems.  Clicking on one shows all the quantities
supplied by that subsystem.

The final pane shows all the quantities, as well as a "Graph all XXX statistics"
choice.  You can choose individual statistics or just hit the "Graph all"
choice.

When you've chosen everything you wish to chart, hit the "Bar graphs" button
at the bottom of the window.

All choices are retained when flipping between components and/or subsystems.
If you select a StringHub quantity from the "rdoutReq" subsystem and then flip
over to the inIceTrigger pane and choose a "TotalRecordsReceived" quantity
from the "stringHit" subsystem, when you click on "Draw graphs" you'll see a
window with two plots, one for each chosen quantity.

Also, each set of plots is drawn in a separate window and you can have multiple
windows at the same time.

Handling new charts
-------------------
New quantities can be charted by extending BaseData and StatParent (or, even
better, by copying a similar *Stat.java file) to implement the parsing and
plotting logic, then adding the new Stat.save() method to BaseParser.match()
