package icecube.daq.tools;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

class GraphFrame
    extends JFrame
    implements WindowListener
{
    public GraphFrame(String title)
    {
        super(title);

        addWindowListener(this);
    }

    public void windowOpened(WindowEvent evt) { }
    public void windowClosing(WindowEvent evt) { dispose(); }
    public void windowClosed(WindowEvent evt) { }
    public void windowIconified(WindowEvent evt) { }
    public void windowDeiconified(WindowEvent evt) { }
    public void windowActivated(WindowEvent evt) { }
    public void windowDeactivated(WindowEvent evt) { }
}
