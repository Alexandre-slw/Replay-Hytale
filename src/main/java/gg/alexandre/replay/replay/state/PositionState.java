package gg.alexandre.replay.replay.state;

public class PositionState {

    public boolean sentInitialPosition;

    public double x, y, z;
    public float bodyPitch, bodyYaw, bodyRoll;
    public float headPitch, headYaw, headRoll;

    public boolean hasPendingTeleport;
    public int nextExpectedTeleportId;
    public double pendingTeleportX, pendingTeleportY, pendingTeleportZ;

    public int mountId;

}
