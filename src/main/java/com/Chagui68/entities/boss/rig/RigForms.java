package com.Chagui68.entities.boss.rig;

import com.Chagui68.entities.boss.herobrine.HerobrineModel;
import com.Chagui68.entities.boss.malenia.MaleniaRig;
import com.Chagui68.entities.boss.radahn.RadahnModel;

import java.util.List;

/** Every form of every rigged boss: what {@code /msc debug model} previews and the web viewer shows. */
public final class RigForms {

    public static final List<RigForm> ALL = List.of(MaleniaRig.FORM, RadahnModel.FORM, HerobrineModel.FORM);

    private RigForms() {
    }

    /** The form called {@code key}, or null. */
    public static RigForm of(String key) {
        for (RigForm form : ALL) if (form.key().equals(key)) return form;
        return null;
    }

    public static List<String> keys() {
        return ALL.stream().map(RigForm::key).toList();
    }
}
