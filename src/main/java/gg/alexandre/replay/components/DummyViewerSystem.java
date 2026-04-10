package gg.alexandre.replay.components;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Set;

public class DummyViewerSystem extends EntityTickingSystem<EntityStore> {

    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemGroupDependency<>(Order.AFTER, EntityTrackerSystems.FIND_VISIBLE_ENTITIES_GROUP),
            new SystemDependency<>(Order.BEFORE, EntityTrackerSystems.EnsureVisibleComponent.class)
    );

    private final ComponentType<EntityStore, EntityTrackerSystems.EntityViewer> viewerType;
    private final ComponentType<EntityStore, TargetWatcherTag> tagType;
    private final Query<EntityStore> query;

    public DummyViewerSystem(
            @Nonnull ComponentType<EntityStore, EntityTrackerSystems.EntityViewer> viewerType,
            @Nonnull ComponentType<EntityStore, TargetWatcherTag> tagType
    ) {
        this.viewerType = viewerType;
        this.tagType = tagType;
        this.query = Query.and(viewerType, tagType);
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> cb) {
        EntityTrackerSystems.EntityViewer viewer = chunk.getComponent(index, viewerType);
        TargetWatcherTag tag = chunk.getComponent(index, tagType);

        if (viewer != null && tag != null && tag.target != null && tag.target.isValid()) {
            viewer.visible.add(tag.target);

            TransformComponent targetTransform = store.getComponent(tag.target, TransformComponent.getComponentType());
            TransformComponent dummyTransform = chunk.getComponent(index, TransformComponent.getComponentType());

            if (targetTransform != null && dummyTransform != null) {
                dummyTransform.setPosition(targetTransform.getPosition());
            }
        }
    }
}