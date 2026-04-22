package gg.alexandre.replay.replay.state;

import gg.alexandre.replay.util.Position;

public class EditState {

    public Position cameraPosition = new Position(0, 0, 0, 0, 0);
    public double speed = 1.0;

    // TODO: track
    public Position playerPosition = new Position(0, 0, 0, 0, 0);
}
