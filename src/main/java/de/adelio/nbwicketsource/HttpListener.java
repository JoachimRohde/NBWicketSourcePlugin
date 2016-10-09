/**
 * Copyright 2016 Joachim F. Rohde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.adelio.nbwicketsource;

/**
 *
 * @author Joachim F. Rohde
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import static java.net.URLDecoder.decode;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;

/**
 * Listens on a socket for requests and opens a file in NetBeans if the request
 * is correctly formatted.
 *
 * NB: This code was heavily inspired by the Wicket-Source-IntelliJ plugin by
 * George Armhold
 *
 * @see
 * https://github.com/armhold/wicket-source-intellij/blob/master/src/com/armhold/wicketsource/Listener.java
 */
public class HttpListener implements Runnable
{

    private static final String CRLF = "\r\n";
    private static final String NL = "\n";
    private static final Logger LOG = Logger.getLogger(HttpListener.class.getName());

    private final ServerSocket serverSocket;
    private final String configuredPassword;

    public HttpListener(ServerSocket serverSocket, String submittedPassword)
    {
        this.serverSocket = serverSocket;
        this.configuredPassword = submittedPassword;
    }

    private void navigateTo(String name, int lineNumber)
    {
        GlobalPathRegistry gpr = GlobalPathRegistry.getDefault();
        name = name.replace(".", "/") + ".java";
        LOG.log(Level.FINE, "Navigate to {0} on line {1}", new Object[]
        {
            name, lineNumber
        });

        FileObject fo = gpr.findResource(name);
        try
        {
            DataObject d = DataObject.find(fo);
            EditorCookie ec = d.getCookie(EditorCookie.class);
            ec.open();
            StyledDocument doc = ec.openDocument();
            LineCookie lc = DataObject.find(fo).getLookup().lookup(LineCookie.class);
            final Line line = lc.getLineSet().getOriginal(lineNumber - 1);

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FRONT, 1);
                }
            });

        } catch (IOException | IndexOutOfBoundsException ex)
        {
            LOG.log(Level.SEVERE, "Error while opening file " + name, ex);
            Exceptions.printStackTrace(ex);
        }

    }

    public void run()
    {
        while (true)
        {
            Socket clientSocket;

            try
            {
                clientSocket = serverSocket.accept();
            } catch (IOException e)
            {
                if (serverSocket.isClosed())
                {
                    break;
                } else
                {
                    LOG.log(Level.SEVERE, "Error while accepting connection on socket: {0}", e.getMessage());
                    continue;
                }
            }

            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                StringBuilder buf = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null && !inputLine.equals(CRLF) && !inputLine.equals(NL) && !inputLine.isEmpty())
                {
                    buf.append(inputLine);
                }
                clientSocket.getOutputStream().write(("HTTP/1.1 200 OK" + CRLF + CRLF).getBytes());
                clientSocket.close();

                handleRequest(buf.toString());

            } catch (IOException e)
            {
                LOG.log(Level.SEVERE, "Error while handling request: {0}", e.getMessage());
            }
        }
    }

    private void handleRequest(String requestString) throws UnsupportedEncodingException
    {
        // a typical requestString looks like: GET /open?src=de.frs.flexways.presentation.content.customer%3ACustomerPanel.java%3A42&p= HTTP/1.1Host: localhost:9123Connection: keep-aliveOrigin: chrome-extension://ioeogpblhkcbghoggbnofoclgmpcphflUser-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36Accept: */*Accept-Encoding: gzip, deflate, sdchAccept-Language: en-US,en;q=0.8

        String[] requestParts = requestString.split(" ");
        if (requestParts.length > 1)
        {
            LOG.log(Level.FINE, "Received request: {0}", requestString);
            Map<String, String> parameters = getParametersFromUrl(requestParts[1]);
            String src = parameters.get("src") != null ? decode(parameters.get("src").trim(), "UTF-8") : "";
            String submittedPassword = parameters.get("p") != null ? decode(parameters.get("p").trim(), "UTF-8") : "";
            LOG.log(Level.FINEST, "src parameter: {0}", src);

            if (submittedPassword != null && !submittedPassword.isEmpty() && !submittedPassword.equals(configuredPassword))
            {
                LOG.info("passwords do not match}");
            } else
            {
                // split the src parameter (e.g.: com.example.mypackage:Foo:42) into it's parts
                String[] pieces = src.split(":");
                if (pieces.length == 3)
                {
                    String packageName = pieces[0];
                    String fileName = pieces[1].replace(".java", "").replace(".scala", "");
                    int lineNumber = 1;
                    try
                    {
                        lineNumber = Integer.parseInt(pieces[2]);
                    } catch (NumberFormatException numberFormatException)
                    {
                        // do nothing, jump to the first line
                    }

                    navigateTo(packageName + "." + fileName, lineNumber);
                }
            }
        }

    }

    private Map<String, String> getParametersFromUrl(String url)
    {
        String parametersString = url.substring(url.indexOf('?') + 1);
        Map<String, String> parameters = new HashMap<>();
        StringTokenizer tokenizer = new StringTokenizer(parametersString, "&");
        while (tokenizer.hasMoreElements())
        {
            String[] parametersPair = tokenizer.nextToken().split("=", 2);
            if (parametersPair.length > 1)
            {
                parameters.put(parametersPair[0], parametersPair[1]);
            }
        }

        return parameters;
    }

}
