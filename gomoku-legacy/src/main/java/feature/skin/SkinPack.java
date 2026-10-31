package feature.skin;

import java.util.List;
import ui.design.Skin;

public final class SkinPack {
    public static final List<Skin> ALL = List.of(
        ClassicSkin.INSTANCE,
        DarkWoodSkin.INSTANCE,
        JadeSkin.INSTANCE,
        NeonSkin.INSTANCE
    );

    public static Skin byId(String id) {
        for (Skin s : ALL) if (s.id.equals(id)) return s;
        return ClassicSkin.INSTANCE;
    }

    private SkinPack() {}
}
