/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.zh32.slp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 *
 * @author zh32 <zh32 at zh32.de>
 */
public class ServerListPing17 {

    private InetSocketAddress host;
    private int timeout = 7000;
    private Gson gson;

    public ServerListPing17(InetSocketAddress host) {
        this.host = host;
    }

    public void setAddress(InetSocketAddress host) {
        this.host = host;
    }

    public InetSocketAddress getAddress() {
        return this.host;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    int getTimeout() {
        return this.timeout;
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    private void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public StatusResponse fetchData() throws IOException {

        StatusResponse response;
        try (Socket socket = new Socket()) {
            OutputStream outputStream;
            DataOutputStream dataOutputStream;
            InputStream inputStream;
            InputStreamReader inputStreamReader;

            socket.setSoTimeout(this.timeout);

            socket.connect(host, timeout);

            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            inputStream = socket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(b);
            handshake.writeByte(0x00); //packet id for handshake
            writeVarInt(handshake, -1); //protocol version
            writeVarInt(handshake, this.host.getHostString().length()); //host length
            handshake.writeBytes(this.host.getHostString()); //host string
            handshake.writeShort(host.getPort()); //port
            writeVarInt(handshake, 1); //state (1 for handshake)

            writeVarInt(dataOutputStream, b.size()); //prepend size
            dataOutputStream.write(b.toByteArray()); //write handshake packet


            dataOutputStream.writeByte(0x01); //size is only 1
            dataOutputStream.writeByte(0x00); //packet id for ping
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int size = readVarInt(dataInputStream); //size of packet
            int id = readVarInt(dataInputStream); //packet id

            if (id == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (id != 0x00) { //we want a status response
                throw new IOException("Invalid packetID");
            }
            int length = readVarInt(dataInputStream); //length of json string

            if (length == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (length == 0) {
                throw new IOException("Invalid string length.");
            }

            byte[] in = new byte[length];
            dataInputStream.readFully(in);  //read json string
            String json = new String(in);


            dataOutputStream.writeByte(0x09); //size of packet
            dataOutputStream.writeByte(0x01); //0x01 for ping
            long now = System.currentTimeMillis();
            dataOutputStream.writeLong(now); //time!?

            readVarInt(dataInputStream);
            id = readVarInt(dataInputStream);
            if (id == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (id != 0x01) {
                throw new IOException("Invalid packetID");
            }
            dataInputStream.readLong(); //read response
            long peng = System.currentTimeMillis();

            int protocol = new Gson().fromJson(json, JsonObject.class).get("version").getAsJsonObject().get("protocol").getAsInt();
            if (protocol >= 735)
                gson = GsonComponentSerializer.colorDownsamplingGson().populator().apply(new GsonBuilder().disableHtmlEscaping()).create();
            else gson = GsonComponentSerializer.gson().populator().apply(new GsonBuilder().disableHtmlEscaping()).create();

            response = gson.fromJson(json, StatusResponse.class);
            response.setTime((int) (peng - now));

            dataOutputStream.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
        }

        return response;
    }


    public static class StatusResponse {
        private TextComponent description;
        private Players players;
        private Version version;
        private String favicon;
        private int time;

        public TextComponent getDescription() {
            return description;
        }

        public Players getPlayers() {
            return players;
        }

        public Version getVersion() {
            return version;
        }

        public String getFavicon() {
            return favicon;
        }

        public int getTime() {
            return time;
        }

        void setTime(int time) {
            this.time = time;
        }

    }

    public static class Players {
        private int max;
        private int online;
        private List<Player> sample;

        public int getMax() {
            return max;
        }

        public int getOnline() {
            return online;
        }

        public List<Player> getSample() {
            return sample;
        }
    }

    public static class Player {
        private String name;
        private String id;

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

    }

    public static class Version {
        private String name;
        private String protocol;

        public String getName() {
            return name;
        }

        public String getProtocol() {
            return protocol;
        }
    }
}