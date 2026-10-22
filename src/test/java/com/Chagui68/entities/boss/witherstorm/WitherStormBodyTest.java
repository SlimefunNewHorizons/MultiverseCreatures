package com.Chagui68.entities.boss.witherstorm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the body sends: every box that moved past {@link WitherStormBody#same} between two frames is a
 * packet to every player watching, so a hovering storm must leave most of its body alone.
 */
class WitherStormBodyTest {

    /** Boxes resent per frame over two seconds of hovering in place, its tentacles swaying. */
    static double resentPerFrame(WitherStormForm form, boolean pitchFollowsBob) {
        WitherStormModel model = form.model();
        WitherStormModel.Placement[] sent = null;
        int resent = 0, frames = 0;
        float pitch = 0;
        for (int age = 0; age <= 40; age++) {
            // The bob of WitherStorm.move: 1.8 blocks up and down over a 30-tick period.
            double bob = 1.8 / 30 * Math.cos(age / 30.0);
            pitch += ((float) Math.max(-15, Math.min(15, -bob * 60)) - pitch) * 0.05f;
            if (age % WitherStorm.FRAME != 0) continue;
            WitherStormModel.Pose pose = new WitherStormModel.Pose();
            pose.ticks = age;
            pose.tentacleTicks = age;
            pose.bodyPitch = pitchFollowsBob ? pitch : 0;
            WitherStormModel.Placement[] now = model.place(pose, 1f);
            if (sent != null) {
                for (int i = 0; i < now.length; i++) {
                    if (now[i] != null && !WitherStormBody.same(sent[i], now[i])) {
                        resent++;
                        sent[i] = now[i];
                    }
                }
                frames++;
            } else {
                sent = now;
            }
        }
        return resent / (double) frames;
    }

    @Test
    @DisplayName("A hovering colossal storm resends only a small part of its body each frame")
    void hoveringIsCheap() {
        for (WitherStormForm form : new WitherStormForm[]{WitherStormForm.DESTROYER, WitherStormForm.DEVOURER}) {
            int boxes = form.model().boxes().size();
            double still = resentPerFrame(form, false), bobbing = resentPerFrame(form, true);
            System.out.printf("%s: %d boxes, %.1f resent a frame hovering level, %.1f tilting with the bob%n",
                    form, boxes, still, bobbing);
            assertTrue(still < boxes * 0.5, form + " resends " + still + " of " + boxes + " boxes a frame while hovering");
        }
    }
}
