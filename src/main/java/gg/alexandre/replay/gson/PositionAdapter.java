package gg.alexandre.replay.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import gg.alexandre.replay.util.Position;

import java.io.IOException;

public class PositionAdapter extends TypeAdapter<Position> {

    @Override
    public void write(JsonWriter out, Position value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("x").value(value.x());
        out.name("y").value(value.y());
        out.name("z").value(value.z());
        out.name("yaw").value(value.yaw());
        out.name("pitch").value(value.pitch());
        out.endObject();
    }

    @Override
    public Position read(JsonReader in) throws IOException {
        double x = 0, y = 0, z = 0, yaw = 0, pitch = 0;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "x" -> x = in.nextDouble();
                case "y" -> y = in.nextDouble();
                case "z" -> z = in.nextDouble();
                case "yaw" -> yaw = in.nextDouble();
                case "pitch" -> pitch = in.nextDouble();
                default -> in.skipValue();
            }
        }
        in.endObject();

        return new Position(x, y, z, yaw, pitch);
    }
}