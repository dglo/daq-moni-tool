package icecube.daq.tools;

/*
  This software will read, parse and deliver the contents of
  an APPLET tag in HTML.

  Copyright(C) 1998 Tom Whittaker.

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 1, or (at your option)
  any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License in file NOTICE for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Read, parse, and deliver the contents of the
 * APPLET tag in a file that contains HTML.  The 'delivery'
 * is in a HashMap with entries for each token and its
 * value.
 *
 * It is very unforgiving on formatting errors...
 *
 * The parsing is based on the = sign, outside of quoted fields.
 */
class AppletParams
    extends HashMap
{
    /**
     * @param fileName HTML file name
     */
    public AppletParams(String fileName)
        throws IOException
    {
        this(new FileInputStream(fileName), false);
    }

    /**
     * @param fileName HTML file name
     */
    public AppletParams(String fileName, boolean showValues)
        throws IOException
    {
        this(new FileInputStream(fileName), showValues);
    }

    /**
     * @param is the InputStream aimed at the HTML
     */
    public AppletParams(InputStream is)
        throws IOException
    {
        this(is, false);
    }

    /**
     * @param is the InputStream aimed at the HTML
     */
    public AppletParams(InputStream is, boolean showValues)
        throws IOException
    {
        // read input stream as bytes until done...

        byte[] b = new byte[1];
        StringBuffer sb = new StringBuffer();

        while (true) {
            try {
                int k = is.read(b);
                if (k == -1) {
                    break;
                }
            } catch (IOException er) {
                break;
            }

            // discard all 'control' characters

            if (b[0] > 0x1F) {
                char c = (char) b[0];
                sb.append(c);
            }
        }

        String app = null;

        // extract the parameter string

        String str = sb.toString();
        String lowStr = str.toLowerCase();
        int begTag = lowStr.indexOf("<applet");
        if (begTag >= 0) {
            int begParam = lowStr.indexOf(">", begTag);
            if (begParam > 0) {
                int endParam = lowStr.indexOf("</applet", begParam);
                if (endParam > 0) {
                    app = str.substring(begParam, endParam);
                }
            }
        }

        if (app == null) {
            throw new IOException("unable to find applet parameters");
        }

        // pointer to position in app
        int i = 0;

        // flag for name= value= pairing
        boolean gotNameWaitForValue = false;

        // to hold the name from one of these
        String holdName = " ";

        while (true) {

            // look for an = sign
            int inxEq = app.indexOf("=", i);
            if (inxEq < 0) {
                break;
            }

            // back up from inxEq looking for a blank...
            int inxB = inxEq;
            while (true) {
                inxB = inxB - 1;
                if (inxB == 0 || app.charAt(inxB) == ' ') {
                    break;
                }
            }

            // now look forward from the = sign
            // if you hit a " mark first, then look for it's match to
            // terminate the value; otherwise, stop at the first blank

            int inxAb = inxEq + 1;
            int inxAe = inxEq;
            boolean gotQuote = false;

            while (true) {
                inxAe = inxAe + 1;
                if (app.charAt(inxAe) == '"')  {
                    if (gotQuote) {
                        break;
                    } else {
                        inxAb = inxAe + 1;
                        gotQuote = true;
                    }
                } else if (app.charAt(inxAe) == ' ' && !gotQuote) {
                    break;
                } else if (app.charAt(inxAe) == '>' && !gotQuote) {
                    break;
                }
            }

            // got a name and value

            String name = app.substring(inxB, inxEq).trim();
            String value = app.substring(inxAb, inxAe).trim();

            // if the name is "name" then hold on for "value"

            if (name.compareTo("name") == 0) {
                if (gotNameWaitForValue) {
                    throw new IOException("Unable to find 'value=' for name=" + name);
                }

                gotNameWaitForValue = true;
                holdName = value;
            } else {
                if (gotNameWaitForValue) {
                    if (name.compareTo("value") != 0) {
                        throw new IOException("Unable to find 'value=' for name=" + name);
                    }

                    put(holdName, value);
                    gotNameWaitForValue = false;
                } else {
                    put(name, value);
                }
            }

            i = inxAe + 1;

            // this should (!) never (!!) happen...
            if (i > app.length()) {
                throw new IOException("Index i=" + i + " app.length=" + app.length());
            }
        }
    }

    public static void main(String[] args)
        throws IOException
    {
        // little testing program: java AppletParams <HTML filename>
        AppletParams ap = new AppletParams(args[0]);
        Iterator iter = ap.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            System.out.println(name + " = " + ap.get(name));
        }
    }
}
