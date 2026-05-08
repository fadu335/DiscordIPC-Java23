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
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.Packet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;

public class UnixPipe extends Pipe
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixPipe.class);

    private final SocketChannel channel;

    UnixPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) throws IOException
    {
        super(ipcClient, callbacks);
        this.channel = SocketChannel.open(UnixDomainSocketAddress.of(Path.of(location)));
        this.channel.configureBlocking(true);
    }

    @Override
    public void write(byte[] b) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(b);
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    @Override
    public Packet read() throws IOException, JSONException {
        if (status == PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if (status == PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);

        ByteBuffer header = ByteBuffer.allocate(2 * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        readFully(header);
        header.flip();
        int op = header.getInt();
        int len = header.getInt();

        ByteBuffer payload = ByteBuffer.allocate(len);
        readFully(payload);

        Packet p = new Packet(Packet.OpCode.values()[op],
                new JSONObject(new String(payload.array(), StandardCharsets.UTF_8)));
        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        if (listener != null)
            listener.onPacketReceived(ipcClient, p);
        return p;
    }

    private void readFully(ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            int r = channel.read(buf);
            if (r == -1)
                throw new IOException("Pipe closed by peer");
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Closing IPC pipe...");
        send(Packet.OpCode.CLOSE, new JSONObject(), null);
        status = PipeStatus.CLOSED;
        channel.close();
    }

}
