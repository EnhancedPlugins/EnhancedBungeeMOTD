package de.blocki.enhancedplugins.enhancedbungeemotd.utils.pinger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API for pinging and obtaining info about a Minecraft server.
 */
public final class MCServerPing {

  private MCServerPing() {
  }


  /**
   * Pings a Minecraft Server to obtain server info.
   *
   * @param address server address
   * @return MCServerPingResponse
   * @throws IOException
   */
  public static MCServerPingResponse getPing(final String address) throws IOException, TimeoutException {
    return getPing(address, 25565);
  }

  /**
   * Pings a Minecraft Server to obtain server info.
   *
   * @param address server address
   * @param port    server port
   * @return MCServerPingResponse
   * @throws IOException
   */
  public static MCServerPingResponse getPing(final String address, final int port)
          throws IOException, TimeoutException {

    if (address == null) {
      throw new IOException("Hostname cannot be null!");
    }

    String serverHost = address;
    int serverPort = port;

    Record[] srvRecords = new Lookup(String.format("_minecraft._tcp.%s", address), Type.SRV).run();

    if (srvRecords != null) {
      for (Record srvRecord : srvRecords) {
        SRVRecord srv = (SRVRecord) srvRecord;


        serverHost = srv.getTarget().toString().replaceFirst("\\.$", "");
        serverPort = srv.getPort();

      }
    }

    String json;

    long ping = System.currentTimeMillis();

    try (Socket socket = new Socket()) {

      socket.connect(new InetSocketAddress(serverHost, serverPort), 5000);
      ping = System.currentTimeMillis() - ping;

      ByteArrayOutputStream handshakeStream = new ByteArrayOutputStream();
      DataOutputStream handshake = new DataOutputStream(handshakeStream);

      handshake.write(0x00); // Handshake Packet
      writeVarInt(handshake, 4); // Protocol Version
      writeVarInt(handshake, address.length());
      handshake.writeBytes(address);
      handshake.writeShort(port);
      writeVarInt(handshake, 1); // Status Handshake

      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      writeVarInt(out, handshakeStream.size());
      out.write(handshakeStream.toByteArray());

      // STATUS REQUEST ->
      out.writeByte(0x01); // Packet Size
      out.writeByte(0x00); // Packet Status Request

      // <- STATUS RESPONSE
      DataInputStream in = new DataInputStream(socket.getInputStream());
      readVarInt(in);
      int id = readVarInt(in);

      io(id == -1, "Server ended data stream unexpectedly.");
      io(id != 0x00, "Server returned invalid packet.");

      int length = readVarInt(in);
      io(length == -1, "Server ended data stream unexpectedly.");
      io(length == 0, "Server returned unexpected value.");

      byte[] data = new byte[length];
      in.readFully(data);
      json = new String(data, StandardCharsets.UTF_8);

      // Ping ->
      out.writeByte(0x09); // Packet Size
      out.writeByte(0x01); // Ping Packet
      out.writeLong(System.currentTimeMillis());

      // Ping <-
      readVarInt(in);
      id = readVarInt(in);
      io(id == -1, "Server ended data stream unexpectedly.");
      io(id != 0x01, "Server returned invalid packet"); // Check Ping Packet

    }

    JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
    JsonElement descriptionJsonElement = jsonObj.get("description");

    if (descriptionJsonElement.isJsonObject()) {
      // TextComponent MOTDs

      JsonObject descriptionJsonObject = descriptionJsonElement.getAsJsonObject();

      if (descriptionJsonObject.has("extra")) {
        descriptionJsonObject.addProperty("text",
                new TextComponent(descriptionJsonObject.get("text").getAsString())
                        .toLegacyText() +
                new TextComponent(ComponentSerializer.parse(
                        descriptionJsonObject
                                .get("extra")
                                .getAsJsonArray()
                                .toString()
                )).toLegacyText()
        );

        jsonObj.add("description", descriptionJsonObject);
      }

    } else {

      // String MOTDs
      String description = descriptionJsonElement.getAsString();
      JsonObject descriptionJsonObj = new JsonObject();
      descriptionJsonObj.addProperty("text", description);
      jsonObj.add("description", descriptionJsonObj);

    }

    jsonObj.addProperty("ping", ping);

    return MCServerPingResponse.serverPingFromJsonObj(jsonObj);
  }


  /**
   * Throws IOException when condition is false.
   *
   * @param b Condition
   * @param m Exception cause
   * @throws IOException Exception
   */
  public static void io(final boolean b, final String m) throws IOException {
    if (b) {
      throw new IOException(m);
    }
  }

  public static int readVarInt(DataInputStream in) throws IOException, TimeoutException {
    int i = 0;
    int j = 0;

    while (true) {
      AtomicInteger k = new AtomicInteger();

      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<?> future = executor.submit(() -> {
        try {
          k.set(in.readByte());
        } catch (IOException e) {
          k.set(Integer.MAX_VALUE);
        }
      });

      try {
        future.get(3, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        future.cancel(true);
        throw e;
      } catch (ExecutionException | InterruptedException e) {
        throw new IOException(e);
      } finally {
        executor.shutdownNow();
      }

      if (k.get() == Integer.MAX_VALUE) throw new IOException();

      i |= (k.get() & 0x7F) << j++ * 7;

      if (j > 5) {
        throw new IOException("VarInt too big");
      }

      if ((k.get() & 0x80) != 128) {
        break;
      }
    }

    return i;
  }

  public static void writeVarInt(DataOutputStream out, int inputParamInt) throws IOException {
    int paramInt = inputParamInt;
    while (true) {
      if ((paramInt & 0xFFFFFF80) == 0) {
        out.writeByte(paramInt);
        return;
      }

      out.writeByte(paramInt & 0x7F | 0x80);
      paramInt >>>= 7;
    }
  }


}