package com.salwyrr.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class TargetWatcherTag implements Component<EntityStore> {
    public Ref<EntityStore> target;

    public TargetWatcherTag(Ref<EntityStore> target) {
        this.target = target;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new TargetWatcherTag(target);
    }
}