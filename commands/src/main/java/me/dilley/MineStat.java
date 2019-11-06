/*
 * Copyright (C) 2019 FratikB0T Contributors
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

/*
  @author Lloyd Dilley, Arne Sacnussem
 */

package me.dilley;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.*;

@Getter
@Setter(AccessLevel.PRIVATE)
public class MineStat
{
    public static final byte NUM_FIELDS = 6;     // expected number of fields returned from server after query
    public static final int DEFAULT_TIMEOUT = 2500; // default TCP socket connection timeout in milliseconds

    /**
     * Hostname or IP address of the Minecraft server
     */
    private String address;

    /**
     * Port number the Minecraft server accepts connections on
     */
    private int port;

    /**
     * TCP socket connection timeout in milliseconds
     */
    private int timeout;

    /**
     * Is the server up? (true or false)
     */
    private boolean serverUp;

    /**
     * Message of the day from the server
     */
    private String motd;

    /**
     * Minecraft version the server is running
     */
    private String version;

    /**
     * Protocol version
     */
    private String protocolVersion;

    /**
     * Current number of players on the server
     */
    private String currentPlayers;

    /**
     * Maximum player capacity of the server
     */
    private String maximumPlayers;

    /**
     * Ping time to server in milliseconds
     */
    private long latency;

    public MineStat(String address, int port)
    {
        this(address, port, DEFAULT_TIMEOUT);
    }

    private MineStat(String address, int port, int timeout)
    {
        setAddress(address);
        setPort(port);
        setTimeout(timeout);
        refresh();
    }

    /**
     * Refresh state of the server
     * @return <code>true</code>; <code>false</code> if the server is down
     */
    private boolean refresh()
    {
        String[] serverData;
        String rawServerData;
        try
        {
            //Socket clientSocket = new Socket(getAddress(), getPort());
            try (Socket clientSocket = new Socket()) {
                long startTime = System.currentTimeMillis();
                clientSocket.connect(new InetSocketAddress(getAddress(), getPort()), timeout);
                setLatency(System.currentTimeMillis() - startTime);
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                byte[] payload = {(byte) 0xFE, (byte) 0x01};
                //dos.writeBytes("\u00FE\u0001");
                dos.write(payload, 0, payload.length);
                rawServerData = br.readLine();
            }
        }
        catch(Exception e)
        {
            serverUp = false;
            //e.printStackTrace();
            return false;
        }

        if(rawServerData == null)
            serverUp = false;
        else
        {
            serverData = rawServerData.split("\u0000\u0000\u0000");
            if(serverData.length >= NUM_FIELDS)
            {
                serverUp = true;
                setProtocolVersion(serverData[1].replace("\u0000", ""));
                setVersion(serverData[2].replace("\u0000", ""));
                setMotd(serverData[3].replace("\u0000", ""));
                setCurrentPlayers(serverData[4].replace("\u0000", ""));
                setMaximumPlayers(serverData[5].replace("\u0000", ""));
            }
            else
                serverUp = false;
        }
        return serverUp;
    }
}