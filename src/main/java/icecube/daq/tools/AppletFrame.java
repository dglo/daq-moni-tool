package icecube.daq.tools;

// Adapted from:
//
//                core JAVA
//                by Gary Cornell and Cay S. Horstmann
//                Sunsoft Press, 1996
//                ISBN 0-13-565755-5
//                pp. 355-358
//
// To create an application from the Foo applet, add a main() method like:
//
//        public static void main(String args[])
//        {
//          new AppletFrame(new Foo(), 640, 480);
//        }
//
// If you don't want the main() method directly in the applet, simply create
// a FooApp class which extends Foo and has the code above as its only method
//

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.Event;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

class AppletFrame
    extends Frame
    implements AppletStub, AppletContext, WindowListener
{
    private File baseDir;
    private HashMap params;

    public AppletFrame(Applet a, int x, int y)
    {
        this(a, x, y, null);
    }

    public AppletFrame(Applet a, int x, int y, HashMap p)
    {
        baseDir = null;
        params = p;

        addWindowListener(this);
        setTitle(a.getClass().getName());
        resize(x, y);
        add("Center", a);
        a.setStub(this);
        a.init();
        show();
        a.start();
    }

    public boolean handleEvent(Event evt)
    {
        if (evt.id == Event.WINDOW_DESTROY) {
            System.exit(0);
        }
        return super.handleEvent(evt);
    }

    private void getBase()
    {
        if (baseDir == null) {
            File f = new File(new File(".").getAbsolutePath());
            baseDir = new File(f.getParent());
        }
    }

    // AppletStub methods
    public void appletResize(int width, int height)
    {
        resize(width, height);
        validate();
    }
    public AppletContext getAppletContext()
    {
        return this;
    }
    public URL getCodeBase()
    {
        getBase();
        try {
            return new URL("file:" + baseDir);
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public URL getDocumentBase()
    {
        getBase();
        try {
            return new URL("file:" + baseDir + "/foo.html");
        } catch (MalformedURLException e) {
            return null;
        }
    }
    public String getParameter(String name)
    {
        if (params == null) {
            return null;
        }
        return (String )params.get(name);
    }
    public boolean isActive()
    {
        return true;
    }

    // AppletContext methods
    public Image getImage(URL url) {
        String base = getCodeBase().toString();

        String name = url.toString();
        if (name.startsWith(base)) {
            name = name.substring(base.length());
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
        }

        return Toolkit.getDefaultToolkit().getImage(name);
    }
    public void showStatus(String status)
    {
        System.err.println(status);
    }

    // AppletContext stubs
    public Applet getApplet(String name) { return null; }
    public Enumeration getApplets() { return null; }
    public AudioClip getAudioClip(URL url) { return null; }
    public void showDocument(URL url) { }
    public void showDocument(URL url, String target) { }

    public void setStream(String key, InputStream stream)
        throws java.io.IOException
    {
        // do nothing
    }

    public InputStream getStream(String key) { return null; }
    public Iterator getStreamKeys() { return null; }

    // WindowListener methods
    public void windowOpened(WindowEvent evt) { }
    public void windowClosing(WindowEvent evt) { dispose(); }
    public void windowClosed(WindowEvent evt) { System.exit(0); }
    public void windowIconified(WindowEvent evt) { }
    public void windowDeiconified(WindowEvent evt) { }
    public void windowActivated(WindowEvent evt) { }
    public void windowDeactivated(WindowEvent evt) { }
}
