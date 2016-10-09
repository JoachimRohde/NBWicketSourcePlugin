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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;

@ActionID(
        category = "Debug",
        id = "de.adelio.nbwicketsource.ActionListener"
)
@ActionRegistration(
        iconBase = "de/adelio/nbwicketsource/wicketsourcestart.png",
        displayName = "#CTL_ActionListener"
)
@ActionReference(path = "Toolbars/WicketSource", position = 0)
@Messages("CTL_ActionListener=WicketSource")
public final class WicketSourceActionListener extends AbstractAction implements ActionListener
{

    public static final int DEFAULT_PORT = 9123;

    private static final Logger LOG = Logger.getLogger(WicketSourceActionListener.class.getName());

    private static ServerSocket serverSocket;
    private static Thread listener;
    private static boolean running = false;

    @Override
    public void actionPerformed(ActionEvent e)
    {
        toggleRunningState();
    }

    /**
     * Either starts listening, if the plugin is not yet listening, or stops
     * listening, if we are currently listening.
     */
    private void toggleRunningState()
    {
        int port = DEFAULT_PORT;
        String password = null;
        InetSocketAddress inetSocketAddress = null;
        try
        {
            if (!running)
            {
                Preferences pref = NbPreferences.forModule(WicketSourcePanel.class);
                String customPort = pref.get("port", ""+DEFAULT_PORT);
                password = pref.get("password", "");
                try
                {
                    port = Integer.parseInt(customPort);
                } catch (NumberFormatException numberFormatException)
                {
                    port = DEFAULT_PORT;
                }

                serverSocket = new ServerSocket();
                inetSocketAddress = new InetSocketAddress("localhost", port);
                serverSocket.bind(inetSocketAddress);
                running = true;
                putValue("iconBase", "de/adelio/nbwicketsource/wicketsourcestop.png");
                LOG.info("Started listening on port " + port);
            } else
            {
                disposeListenerAndSocket();
                running = false;
                putValue("iconBase", "de/adelio/nbwicketsource/wicketsourcestart.png");
                LOG.info("Stopped listening");
            }
        } catch (IOException e)
        {
            String errorMessage = "Could not start listener";
            if (inetSocketAddress != null)
            {
                errorMessage = errorMessage + " on " + inetSocketAddress.toString();
            }
            errorMessage = errorMessage + ": ";
            NotifyDescriptor.Message msg = new NotifyDescriptor.Message(errorMessage + e.getMessage(), NotifyDescriptor.Message.ERROR_MESSAGE);
            msg.setTitle("WicketSource: An error occurred");
            DialogDisplayer.getDefault().notify(msg);
            return;
        }

        HttpListener messageNotifier = new HttpListener(serverSocket, password);
        listener = new Thread(messageNotifier);
        listener.start();
    }

    private void disposeListenerAndSocket()
    {
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }

            if (listener != null)
            {
                listener.interrupt();
            }
        } catch (IOException e)
        {
            NotifyDescriptor.Message msg = new NotifyDescriptor.Message("Unable to close connection: " + e.getMessage(), NotifyDescriptor.Message.ERROR_MESSAGE);
            msg.setTitle("WicketSource: An error occurred");
            DialogDisplayer.getDefault().notify(msg);
        }
    }

}
