package mc.rpgstats.components;

import nerdhub.cardinal.components.api.util.sync.EntitySyncedComponent;

public interface IStatComponent extends EntitySyncedComponent {
    int getXP();
    void setXP(int newXP);
    int getLevel();
    void setLevel(int newLevel);
}