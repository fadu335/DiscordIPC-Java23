/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jagrosh.discordipc.entities.pipe;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Pipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pipe.class);
    private static final int VERSION = 1;
    PipeStatus status = PipeStatus.CONNECTING;
    IPCListener listener;
    private DiscordBuild build;
    final IPCClient ipcClient;
    private final HashMap<String,Callback> callbacks;

    Pipe(IPCClient ipcClient, HashMap<String, Callback> callbacks)
    {
        this.ipcClient = ipcClient;
        this.callbacks = callbacks;
    }

    public static Pipe openPipe(IPCClient ipcClient, long clientId, HashMap<String,Callback> callbacks,
                                DiscordBuild... preferredOrder) throws NoDiscordClientException
    {

        if(preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};

        Pipe pipe = null;

        // store some files so we can get the preferred client
        Pipe[] open = new Pipe[DiscordBuild.values().length];
        for(int i = 0; i < 10; i++)
        {
            try
            {
                pipe = null;
                IOException lastEx = null;
                for(String location : getPipeLocations(i))
                {
                    try
                    {
                        pipe = createPipe(ipcClient, callbacks, location);
                        break;
                    }
                    catch(IOException ex)
                    {
                        lastEx = ex;
                    }
                }
                if(pipe == null)
                    throw lastEx != null ? lastEx : new IOException("No IPC pipe at index " + i);

                pipe.send(Packet.OpCode.HANDSHAKE, new JSONObject().put("v", VERSION).put("client_id", Long.toString(clientId)), null);

                Packet p = pipe.read(); // this is a valid client at this point

                pipe.build = DiscordBuild.from(p.getJson().getJSONObject("data")
                        .getJSONObject("config")
                        .getString("api_endpoint"));

                //LOGGER.debug(String.format("Found a valid client (%s) with packet: %s", pipe.build.name(), p.toString()));
                // we're done if we found our first choice
                if(pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0])
                {
                    //LOGGER.info(String.format("Found preferred client: %s", pipe.build.name()));
                    break;
                }

                open[pipe.build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[DiscordBuild.ANY.ordinal()] = pipe; // also store in 'any' for use later

                pipe.build = null;
                pipe = null;
            }
            catch(IOException | JSONException ex)
            {
                pipe = null;
            }
        }

        if(pipe == null)
        {
            // we already know we don't have our first pick
            // check each of the rest to see if we have that
            for(int i = 1; i < preferredOrder.length; i++)
            {
                DiscordBuild cb = preferredOrder[i];
                //LOGGER.debug(String.format("Looking for client build: %s", cb.name()));
                if(open[cb.ordinal()] != null)
                {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;
                    if(cb == DiscordBuild.ANY) // if we pulled this from the 'any' slot, we need to figure out which build it was
                    {
                        for(int k = 0; k < open.length; k++)
                        {
                            if(open[k] == pipe)
                            {
                                pipe.build = DiscordBuild.values()[k];
                                open[k] = null; // we don't want to close this
                            }
                        }
                    }
                    else pipe.build = cb;

                    //LOGGER.info(String.format("Found preferred client: %s", pipe.build.name()));
                    break;
                }
            }
            if(pipe == null)
            {
                throw new NoDiscordClientException();
            }
        }
        // close unused files, except skip 'any' because its always a duplicate
        for(int i = 0; i < open.length; i++)
        {
            if(i == DiscordBuild.ANY.ordinal())
                continue;
            if(open[i] != null)
            {
                try {
                    open[i].close();
                } catch(IOException ex) {
                    // This isn't really important to applications and better
                    // as debug info
                    //LOGGER.debug("Failed to close an open IPC pipe!", ex);
                }
            }
        }

        pipe.status = PipeStatus.CONNECTED;

        return pipe;
    }

    private static Pipe createPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win"))
        {
            return new WindowsPipe(ipcClient, callbacks, location);
        }
        else if (osName.contains("mac") || osName.contains("darwin")
                || osName.contains("linux") || osName.contains("nix") || osName.contains("nux"))
        {
            return new UnixPipe(ipcClient, callbacks, location);
        }
        else
        {
            throw new RuntimeException("Unsupported OS: " + osName);
        }
    }

    /**
     * Sends json with the given {@link Packet.OpCode}.
     *
     * @param op The {@link Packet.OpCode} to send data with.
     * @param data The data to send.
     * @param callback callback for the response
     */
    public void send(Packet.OpCode op, JSONObject data, Callback callback)
    {
        try
        {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce",nonce));
            if(callback!=null && !callback.isEmpty())
                callbacks.put(nonce, callback);
            write(p.toBytes());
            LOGGER.debug(String.format("Sent packet: %s", p.toString()));
            if(listener != null)
                listener.onPacketSent(ipcClient, p);
        }
        catch(IOException ex)
        {
            LOGGER.error("Encountered an IOException while sending a packet and disconnected!");
            status = PipeStatus.DISCONNECTED;
        }
    }

    /**
     * Blocks until reading a {@link Packet} or until the
     * read thread encounters bad data.
     *
     * @return A valid {@link Packet}.
     *
     * @throws IOException
     *         If the pipe breaks.
     * @throws JSONException
     *         If the read thread receives bad data.
     */
    public abstract Packet read() throws IOException, JSONException;

    public abstract void write(byte[] b) throws IOException;

    /**
     * Generates a nonce.
     *
     * @return A random {@link UUID}.
     */
    private static String generateNonce()
    {
        return UUID.randomUUID().toString();
    }

    public PipeStatus getStatus()
    {
        return status;
    }

    public void setStatus(PipeStatus status)
    {
        this.status = status;
    }

    public void setListener(IPCListener listener)
    {
        this.listener = listener;
    }

    public abstract void close() throws IOException;

    public DiscordBuild getDiscordBuild()
    {
        return build;
    }

    // a list of system property keys to get IPC file from different unix systems.
    private final static String[] unixPaths = {"XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP"};

    // sub-directories Discord may live in on Linux (Flatpak, Snap, native)
    private final static String[] unixSubdirs = {
            "",
            "/app/com.discordapp.Discord",
            "/app/com.discordapp.DiscordCanary",
            "/snap.discord",
            "/snap.discord-canary"
    };

    /**
     * Returns all candidate IPC socket/pipe locations for the given index.
     * The first one that opens successfully is used.
     *
     * @param i Index of the IPC slot.
     * @return Candidate locations in priority order.
     */
    private static String[] getPipeLocations(int i)
    {
        String osName = System.getProperty("os.name");
        if(osName.contains("Win"))
            return new String[]{ "\\\\.\\pipe\\discord-ipc-" + i };

        java.util.LinkedHashSet<String> bases = new java.util.LinkedHashSet<>();
        for(String key : unixPaths)
        {
            String v = System.getenv(key);
            if(v != null && !v.isEmpty())
                bases.add(v);
        }
        bases.add("/tmp");

        java.util.List<String> result = new java.util.ArrayList<>();
        for(String base : bases)
        {
            for(String sub : unixSubdirs)
                result.add(base + sub + "/discord-ipc-" + i);
        }
        return result.toArray(new String[0]);
    }
}
