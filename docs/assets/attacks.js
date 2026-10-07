// The attack lab: a stylised top-down preview of how each attack plays out. Every attack has the
// same three beats the plugin gives its choreographies (a telegraph on the floor, the blow and the
// recovery); the shapes follow the descriptions in the wiki, not the exact particles.
(() => {
  const BOSSES = {
    sentinel: { name: "Obsidian Sentinel", color: "#ef4b5b" },
    nix: { name: "NIX", color: "#e0457b" },
    witherstorm: { name: "Wither Storm", color: "#8b5cf6" },
  };

  const ATTACKS = [
    { id: "orbital", boss: "sentinel", name: "Orbital Strike", kind: "beam", tag: "Destructive",
      text: "A targeting grid locks onto the arena, beams converge from orbit and a red dome closes over the impact." },
    { id: "meteor", boss: "sentinel", name: "Meteor Impact", kind: "meteor", tag: "Destructive",
      text: "A house-sized meteor falls for five seconds. The warning circle on the floor is where it lands." },
    { id: "supernova", boss: "sentinel", name: "Supernova", kind: "nova", tag: "Destructive",
      text: "A sphere of light swallows the arena. Only the eye of the storm, at his feet, is safe." },
    { id: "pillars", boss: "sentinel", name: "Judgment Pillars", kind: "pillars", tag: "Destructive",
      text: "Columns of light fall across the arena and under every player, after a circle that heats up from red to yellow." },
    { id: "earth", boss: "sentinel", name: "Earth Splitter", kind: "fissure", tag: "Destructive",
      text: "A cross of fissures is torn thirty blocks out from his feet. The world is never broken: craters are made of debris." },
    { id: "void", boss: "sentinel", name: "Void Collapse", kind: "pull", tag: "Destructive",
      text: "A black hole drags everyone toward it, then collapses in a burst." },
    { id: "tsunami", boss: "sentinel", name: "Obsidian Tsunami", kind: "wave", tag: "Destructive",
      text: "A wall of obsidian rolls over the arena. There is a gap in it: find it." },
    { id: "lance", boss: "sentinel", name: "Solar Lance", kind: "lance", tag: "Destructive",
      text: "A fourteen-block spear of sunlight, aimed first, then thrown." },
    { id: "breaker", boss: "sentinel", name: "World Breaker", kind: "shockwave", tag: "Destructive",
      text: "He leaps thirty blocks up and lands with three shockwaves rolling out from the crater." },
    { id: "apocalypse", boss: "sentinel", name: "Apocalypse Rain", kind: "rain", tag: "Destructive",
      text: "Meteors rain over the arena for five seconds, each one marked on the floor before it lands." },
    { id: "triangle", boss: "sentinel", name: "Triangle Call", kind: "summon", tag: "Summon",
      text: "He plants the spear and raises two seals of fire. Columns of light come down through them and reinforcements step out." },

    { id: "guillotine", boss: "nix", name: "Grand Guillotine", kind: "guillotine", tag: "Destructive",
      text: "A sixteen-block guillotine appears over the target. Step off the line before the blade falls." },
    { id: "bloodmoon", boss: "nix", name: "Blood Moon", kind: "wave", tag: "Destructive",
      text: "A red moon rises, then three jumpable waves of blood roll across the floor." },
    { id: "execday", boss: "nix", name: "Execution Day", kind: "spokes", tag: "Destructive",
      text: "Eight giant axes sweep in along their spokes, twice. Stand between them." },
    { id: "harvest", boss: "nix", name: "Blood Harvest", kind: "spiral", tag: "Signature",
      text: "Arms out, blood gathering in his hands, a warning ring closing. Then three full turns, every revolution cutting everyone within 4.5 blocks." },
    { id: "gallows", boss: "nix", name: "Gallows Leap", kind: "leap", tag: "Signature",
      text: "A deep crouch while the landing spot glows under the target, then a leap of up to 18 blocks and a two-handed slam with a shockwave." },
    { id: "condemn", boss: "nix", name: "Condemnation", kind: "pillars", tag: "Signature",
      text: "A gallows of particles stands over every player in range. 1.5 seconds later every blade falls on whoever is still in the circle." },
    { id: "chains", boss: "nix", name: "Chains of Judgment", kind: "pull", tag: "Ranged",
      text: "When a target tries to flee, spectral chains bind them and pull them violently toward him." },

    { id: "roar", boss: "witherstorm", name: "Cataclysmic Roar", kind: "shockwave", tag: "Special",
      text: "Every head roars at once, the sky goes dark and a shockwave rolls along the ground. Jump it: anyone in the air is spared." },
    { id: "skulls", boss: "witherstorm", name: "Skull Barrage", kind: "fan", tag: "Special",
      text: "Every head spits a fan of flaming skulls for two seconds." },
    { id: "debris", boss: "witherstorm", name: "Debris Rain", kind: "rain", tag: "Special",
      text: "The ground it ate comes back down: red circles mark where the rocks will land." },
    { id: "eruption", boss: "witherstorm", name: "Abyssal Eruption", kind: "pillars", tag: "Special",
      text: "The ground under its victims trembles and, a moment later, tentacles of its mass burst out and fling them." },
    { id: "singularity", boss: "witherstorm", name: "Singularity", kind: "pull", tag: "Special",
      text: "It holds still and drags the whole field into its core for four seconds, then the core bursts." },
    { id: "tractor", boss: "witherstorm", name: "Tractor Beam", kind: "cone", tag: "Beam",
      text: "A thin glow warns first, then a purple cone pulls everything in it toward the mouth. Run sideways across the cone to break free." },
  ];

  const clamp = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v));
  const lerp = (a, b, t) => a + (b - a) * t;
  const TAU = Math.PI * 2;
  const rng = (seed) => () => (seed = (seed * 1664525 + 1013904223) >>> 0) / 4294967296;

  // Phase boundaries of the loop: telegraph, blow, recovery.
  const T1 = 0.34, T2 = 0.76;

  function scene(c, w, h) {
    const cx = w / 2, cy = h / 2, R = Math.min(w, h) * 0.46;
    return { c, w, h, cx, cy, R };
  }
  function glow(c, x, y, r, color, a = 1) {
    const g = c.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, color); g.addColorStop(1, "transparent");
    c.globalAlpha = a; c.fillStyle = g; c.beginPath(); c.arc(x, y, r, 0, TAU); c.fill(); c.globalAlpha = 1;
  }
  function warn(c, x, y, r, heat, a = 1) {
    // the telegraph: red warming to yellow
    const col = `rgb(255,${Math.round(lerp(70, 210, heat))},60)`;
    c.globalAlpha = 0.16 * a; c.fillStyle = col; c.beginPath(); c.arc(x, y, r, 0, TAU); c.fill();
    c.globalAlpha = 0.9 * a; c.strokeStyle = col; c.lineWidth = 2; c.stroke(); c.globalAlpha = 1;
  }
  function ring(c, x, y, r, color, width, a = 1) {
    c.globalAlpha = clamp(a); c.strokeStyle = color; c.lineWidth = width; c.beginPath(); c.arc(x, y, Math.max(0, r), 0, TAU); c.stroke(); c.globalAlpha = 1;
  }
  function line(c, x1, y1, x2, y2, color, width, a = 1) {
    c.globalAlpha = clamp(a); c.strokeStyle = color; c.lineWidth = width; c.lineCap = "round";
    c.beginPath(); c.moveTo(x1, y1); c.lineTo(x2, y2); c.stroke(); c.globalAlpha = 1;
  }
  // Where the player stands: a slow wander, so the preview reads as "someone is being hunted".
  function player(s, t, ang = 0.7, dist = 0.55) {
    const a = ang + Math.sin(t * TAU) * 0.35;
    return [s.cx + Math.cos(a) * s.R * dist, s.cy + Math.sin(a) * s.R * dist * 0.9];
  }

  const KINDS = {
    shockwave(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      if (t < T1) warn(c, cx, cy, R * lerp(0.1, 0.3, tel), tel);
      else if (t < T2) for (let i = 0; i < 3; i++) {
        const p = clamp(hit * 1.5 - i * 0.22);
        if (p > 0 && p < 1) ring(c, cx, cy, p * R * 1.02, col, 7 - 4 * p, 1 - p * 0.7);
      } else glow(c, cx, cy, R * 0.25 * (1 - clamp((t - T2) / (1 - T2))), col, 0.5);
    },
    beam(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const [px, py] = player(s, 0.1);
      if (t < T1) {
        warn(c, px, py, R * lerp(0.38, 0.2, tel), tel);
        for (let i = 0; i < 6; i++) { const r = R * (0.1 + i * 0.16); ring(c, px, py, r * (1 - tel * 0.4), "#ffb54a", 1, 0.25); }
        line(c, px - R * 0.4, py, px + R * 0.4, py, "#ffb54a", 1, 0.5); line(c, px, py - R * 0.4, px, py + R * 0.4, "#ffb54a", 1, 0.5);
      } else if (t < T2) {
        for (let i = 0; i < 5; i++) {
          const a = -Math.PI / 2 + (i - 2) * 0.5, sx = cx + Math.cos(a) * R * 1.4, sy = cy + Math.sin(a) * R * 1.4;
          const p = clamp(hit * 2.2 - i * 0.12);
          line(c, sx, sy, lerp(sx, px, p), lerp(sy, py, p), "#fff", 4, 0.9); line(c, sx, sy, lerp(sx, px, p), lerp(sy, py, p), col, 12, 0.35);
        }
        if (hit > 0.45) { glow(c, px, py, R * 0.45 * (hit - 0.4) * 1.7, col, 0.85); ring(c, px, py, R * (hit - 0.4) * 0.9, "#fff", 3, 0.7); }
      } else glow(c, px, py, R * 0.35, col, 0.4 * (1 - clamp((t - T2) / (1 - T2))));
    },
    meteor(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const [px, py] = player(s, 0.3, 1.9, 0.5);
      if (t < T2) warn(c, px, py, R * lerp(0.1, 0.34, clamp(t / T2)), clamp(t / T2));
      if (t >= T1 && t < T2 - 0.05) {
        const p = clamp(hit / 0.85), y = lerp(-R * 0.4, py, p * p), sz = lerp(R * 0.05, R * 0.16, p);
        line(c, px - R * 0.15 * (1 - p), y - R * 0.5, px, y, "#ff8a3a", sz * 0.8, 0.35);
        glow(c, px, y, sz * 2.2, "#ff6a2a", 0.9);
        c.fillStyle = "#2b1d1a"; c.beginPath(); c.arc(px, y, sz, 0, TAU); c.fill();
      } else if (t >= T2 - 0.05) {
        const p = clamp((t - (T2 - 0.05)) / 0.3);
        glow(c, px, py, R * 0.5 * (0.4 + p), "#ffb54a", 1 - p); ring(c, px, py, R * 0.5 * p, "#fff", 4, 1 - p);
      }
    },
    nova(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      if (t < T1) { glow(c, cx, cy, R * lerp(0.1, 0.5, tel), "#ffe08a", 0.35 + tel * 0.5); warn(c, cx, cy, R * 0.98, tel, 0.6); }
      else {
        const k = t < T2 ? hit : 1, fade = t < T2 ? 1 : 1 - clamp((t - T2) / (1 - T2));
        c.save(); c.beginPath(); c.arc(cx, cy, R * 1.02 * Math.min(1, k * 1.3), 0, TAU); c.arc(cx, cy, R * 0.13, 0, TAU, true);
        c.clip("evenodd"); glow(c, cx, cy, R * 1.05, "#fff3c0", 0.95 * fade); c.restore();
        ring(c, cx, cy, R * 0.13, "#7fe3a3", 3, fade);
      }
    },
    pillars(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const r = rng(7), spots = [player(s, 0.1)];
      for (let i = 0; i < 7; i++) { const a = r() * TAU, d = 0.25 + r() * 0.65; spots.push([cx + Math.cos(a) * R * d, cy + Math.sin(a) * R * d]); }
      spots.forEach(([x, y], i) => {
        const rad = R * 0.115;
        if (t < T1) warn(c, x, y, rad, tel);
        else if (t < T2) {
          const p = clamp(hit * 1.6 - i * 0.05);
          if (p > 0) { glow(c, x, y, rad * 1.8, col, 0.5 * p); c.fillStyle = "#fff"; c.globalAlpha = 0.9 * (1 - p * 0.4); c.beginPath(); c.arc(x, y, rad * (0.4 + 0.4 * (1 - p)), 0, TAU); c.fill(); c.globalAlpha = 1; ring(c, x, y, rad * (0.8 + p), col, 3, 1 - p * 0.6); }
        } else glow(c, x, y, rad, col, 0.3 * (1 - clamp((t - T2) / (1 - T2))));
      });
    },
    fissure(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s, r = rng(3);
      for (let k = 0; k < 4; k++) {
        const a = k * Math.PI / 2 + Math.PI / 4;
        if (t < T1) line(c, cx, cy, cx + Math.cos(a) * R * tel, cy + Math.sin(a) * R * tel, "#ff5a4a", 2, 0.7);
        else {
          const len = t < T2 ? hit : 1, fade = t < T2 ? 1 : 1 - clamp((t - T2) / (1 - T2));
          let x = cx, y = cy, ang = a; c.beginPath(); c.moveTo(x, y);
          for (let i = 1; i <= 24 * len; i++) { ang += (r() - 0.5) * 0.45; x += Math.cos(ang) * R / 24; y += Math.sin(ang) * R / 24; c.lineTo(x, y); }
          c.globalAlpha = fade; c.strokeStyle = "#ff8a3a"; c.lineWidth = 9; c.lineJoin = "round"; c.stroke(); c.strokeStyle = "#ffe9a8"; c.lineWidth = 3; c.stroke(); c.globalAlpha = 1;
        }
      }
    },
    pull(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s, r = rng(11);
      const core = t < T1 ? R * 0.04 * tel : t < T2 ? R * (0.04 + 0.05 * hit) : R * 0.09 * (1 - clamp((t - T2) / 0.12));
      if (t >= T1 && t < T2) for (let i = 0; i < 90; i++) {
        const a0 = r() * TAU, d0 = 0.35 + r() * 0.75, p = (hit * 1.4 + r()) % 1;
        const d = d0 * (1 - p * p), a = a0 + p * 4;
        c.fillStyle = col; c.globalAlpha = 0.25 + 0.7 * p; c.fillRect(cx + Math.cos(a) * R * d, cy + Math.sin(a) * R * d, 3, 3);
      }
      c.globalAlpha = 1;
      if (t < T1) ring(c, cx, cy, R * 0.6 * (1 - tel * 0.5), col, 1.5, 0.5 * tel);
      glow(c, cx, cy, core * 3, col, 0.7); c.fillStyle = "#050308"; c.beginPath(); c.arc(cx, cy, core, 0, TAU); c.fill();
      if (t >= T2) { const p = clamp((t - T2) / (1 - T2)); ring(c, cx, cy, R * 0.9 * p, "#fff", 4, 1 - p); glow(c, cx, cy, R * 0.5 * p, col, 0.6 * (1 - p)); }
    },
    wave(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const gapY = cy + R * 0.25, gap = R * 0.2;
      if (t < T1) {
        line(c, cx - R, cy - R * 0.85, cx - R, gapY - gap, "#ff5a4a", 2, 0.5 * tel);
        line(c, cx - R, gapY + gap, cx - R, cy + R * 0.85, "#ff5a4a", 2, 0.5 * tel);
        line(c, cx - R, gapY - gap, cx - R, gapY + gap, "#7fe3a3", 3, 0.8 * tel);
      }
      else if (t < T2) {
        for (let w = 0; w < 3; w++) {
          const p = clamp(hit * 1.6 - w * 0.22); if (p <= 0 || p >= 1) continue;
          const x = cx - R + p * R * 2.05, half = Math.sqrt(Math.max(0, R * R - (x - cx) ** 2)) * 0.92;
          const g = c.createLinearGradient(x - 24, 0, x + 8, 0); g.addColorStop(0, "transparent"); g.addColorStop(1, col);
          c.fillStyle = g; c.globalAlpha = 0.95;
          c.fillRect(x - 24, cy - half, 32, gapY - gap - (cy - half)); c.fillRect(x - 24, gapY + gap, 32, cy + half - (gapY + gap)); c.globalAlpha = 1;
        }
      }
    },
    lance(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const [px, py] = player(s, 0.2, 0.3, 0.8), a = Math.atan2(py - cy, px - cx), ex = cx + Math.cos(a) * R * 1.05, ey = cy + Math.sin(a) * R * 1.05;
      if (t < T1) { line(c, cx, cy, lerp(cx, ex, tel), lerp(cy, ey, tel), "#ffb54a", 1.5, 0.6); warn(c, cx, cy, R * 0.12 * (0.5 + tel), tel, 0.8); }
      else if (t < T2) {
        const p = clamp(hit * 2); line(c, cx, cy, lerp(cx, ex, p), lerp(cy, ey, p), col, 22 * (1 - hit * 0.5), 0.45);
        line(c, cx, cy, lerp(cx, ex, p), lerp(cy, ey, p), "#fff7d0", 8 * (1 - hit * 0.5), 1); glow(c, lerp(cx, ex, p), lerp(cy, ey, p), R * 0.14, "#ffe08a", 0.9);
      } else { const f = 1 - clamp((t - T2) / (1 - T2)); line(c, cx, cy, ex, ey, col, 10 * f, 0.5 * f); }
    },
    rain(s, t, col) {
      const { c, cx, cy, R } = s, r = rng(5);
      for (let i = 0; i < 12; i++) {
        const a = r() * TAU, d = Math.sqrt(r()) * 0.9, x = cx + Math.cos(a) * R * d, y = cy + Math.sin(a) * R * d * 0.95, off = r() * 0.4;
        const local = clamp((t - off * 0.7) / 0.5);
        if (local <= 0) continue;
        if (local < 0.62) { warn(c, x, y, R * 0.075, local / 0.62); const p = clamp((local - 0.3) / 0.32); if (p > 0) { line(c, x + 14 * (1 - p), y - R * 0.5 * (1 - p) - 8, x, y - R * 0.5 * (1 - p), "#ff8a3a", 3, 0.8); glow(c, x, y - R * 0.5 * (1 - p), 7, "#ffb54a", 1); } }
        else { const p = clamp((local - 0.62) / 0.38); glow(c, x, y, R * 0.12 * (0.5 + p), "#ffb54a", 1 - p); ring(c, x, y, R * 0.1 * p, "#fff", 2, 1 - p); }
      }
    },
    summon(s, t, col) {
      const tel = clamp(t / T1), { c, cx, cy, R } = s;
      [[-0.45, -0.25], [0.45, -0.25], [0, 0.55]].forEach(([dx, dy], i) => {
        const x = cx + dx * R, y = cy + dy * R;
        if (t < T2) { ring(c, x, y, R * 0.11, col, 2, 0.8); ring(c, x, y, R * 0.07 + Math.sin(t * 30 + i) * 2, "#ffb54a", 1.5, 0.7); }
        if (t >= T1) { const p = clamp((t - T1) / 0.2); glow(c, x, y - R * 0.3 * (1 - p), R * 0.25, "#ffe9a8", 0.5 * (1 - clamp((t - T2) / 0.2))); if (p > 0.5) { c.fillStyle = col; c.fillRect(x - 7, y - 7, 14, 14); c.fillStyle = "#fff"; c.fillRect(x - 4, y - 3, 3, 3); c.fillRect(x + 1, y - 3, 3, 3); } }
      });
      if (tel > 0 && t < T1) glow(c, cx, cy, R * 0.15, col, 0.3);
    },
    guillotine(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const [px, py] = player(s, 0.2, 0.4, 0.5), ang = -0.5, dx = Math.cos(ang), dy = Math.sin(ang), L = R * 0.7;
      if (t < T1) { line(c, px - dx * L, py - dy * L, px + dx * L, py + dy * L, "#ff5a4a", 2 + tel * 6, 0.25 + tel * 0.4); }
      else if (t < T2) {
        const p = clamp(hit * 3);
        line(c, px - dx * L, py - dy * L, px + dx * L, py + dy * L, "#ff5a4a", 8, 0.3);
        const bx = px - dy * R * 0.5 * (1 - p), by = py + dx * R * 0.5 * (1 - p);
        line(c, bx - dx * L, by - dy * L, bx + dx * L, by + dy * L, "#e8ecf5", 9, 0.95); line(c, bx - dx * L, by - dy * L, bx + dx * L, by + dy * L, col, 18, 0.25);
        if (p >= 1) { ring(c, px, py, R * 0.3 * clamp((hit - 0.33) * 3), "#fff", 3, 1 - clamp((hit - 0.33) * 1.5)); }
      } else line(c, px - dx * L, py - dy * L, px + dx * L, py + dy * L, col, 6, 0.5 * (1 - clamp((t - T2) / (1 - T2))));
    },
    spokes(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      for (let k = 0; k < 8; k++) {
        const a = k * Math.PI / 4;
        if (t < T1) line(c, cx, cy, cx + Math.cos(a) * R * tel, cy + Math.sin(a) * R * tel, "#ff5a4a", 2, 0.7);
        else {
          line(c, cx, cy, cx + Math.cos(a) * R, cy + Math.sin(a) * R, "#ff5a4a", 1, 0.25);
          if (t < T2) {
            const sweep = Math.abs(Math.sin(hit * Math.PI * 2)), d = R * lerp(0.15, 1, sweep), x = cx + Math.cos(a) * d, y = cy + Math.sin(a) * d;
            c.save(); c.translate(x, y); c.rotate(a + hit * 8); c.fillStyle = "#e8ecf5"; c.fillRect(-4, -R * 0.07, 8, R * 0.14); c.fillStyle = col; c.beginPath(); c.moveTo(4, -R * 0.07); c.lineTo(R * 0.09, 0); c.lineTo(4, R * 0.07); c.fill(); c.restore();
            glow(c, x, y, R * 0.1, col, 0.35);
          }
        }
      }
    },
    spiral(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      if (t < T1) { ring(c, cx, cy, R * lerp(0.6, 0.3, tel), "#ff5a4a", 2, 0.9); glow(c, cx - R * 0.07, cy, R * 0.07, col, tel); glow(c, cx + R * 0.07, cy, R * 0.07, col, tel); }
      else if (t < T2) {
        const base = hit * 3 * TAU; warn(c, cx, cy, R * 0.3, 0.8, 0.5);
        for (let b = 0; b < 2; b++) for (let i = 0; i < 26; i++) {
          const a = base + b * Math.PI - i * 0.09, d = R * 0.3 * (1 - i / 60);
          c.fillStyle = col; c.globalAlpha = (1 - i / 26) * 0.9; c.beginPath(); c.arc(cx + Math.cos(a) * d, cy + Math.sin(a) * d, 5 - i * 0.14, 0, TAU); c.fill();
        }
        c.globalAlpha = 1;
      } else ring(c, cx, cy, R * 0.34 * (1 + clamp((t - T2) / (1 - T2)) * 0.5), col, 5, 1 - clamp((t - T2) / (1 - T2)));
    },
    leap(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const [px, py] = player(s, 0.2, 2.3, 0.62);
      if (t < T2) warn(c, px, py, R * 0.2, clamp(t / T2));
      if (t >= T1 && t < T2) {
        const p = clamp(hit / 0.7), x = lerp(cx, px, p), y = lerp(cy, py, p) - Math.sin(p * Math.PI) * R * 0.5;
        glow(c, x, lerp(cy, py, p), R * 0.06 * (1 + Math.sin(p * Math.PI)), "#000", 0.5);
        glow(c, x, y, R * 0.1, col, 0.9);
        if (hit > 0.7) { const q = (hit - 0.7) / 0.3; ring(c, px, py, R * 0.45 * q, "#fff", 4, 1 - q); ring(c, px, py, R * 0.3 * q, col, 6, 1 - q); }
      } else if (t >= T2) glow(c, px, py, R * 0.2, col, 0.4 * (1 - clamp((t - T2) / (1 - T2))));
    },
    fan(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s;
      const dir = -0.35;
      if (t < T1) { c.fillStyle = "#ff5a4a"; c.globalAlpha = 0.12 * tel; c.beginPath(); c.moveTo(cx, cy); c.arc(cx, cy, R, dir - 0.6, dir + 0.6); c.closePath(); c.fill(); c.globalAlpha = 1; }
      else if (t < T2) for (let i = 0; i < 18; i++) {
        const a = dir + ((i % 9) - 4) * 0.14, p = clamp(hit * 1.4 - Math.floor(i / 9) * 0.25 - (i % 3) * 0.04); if (p <= 0 || p >= 1) continue;
        const d = p * R * 1.05, x = cx + Math.cos(a) * d, y = cy + Math.sin(a) * d;
        line(c, x - Math.cos(a) * 24, y - Math.sin(a) * 24, x, y, "#ff7a2a", 5, 0.5); glow(c, x, y, 11, "#ffb54a", 1);
        c.fillStyle = "#e8ecf5"; c.fillRect(x - 3, y - 3, 6, 6);
        if (p > 0.92) ring(c, x, y, 16, "#ff7a2a", 2, 0.9);
      }
    },
    cone(s, t, col) {
      const tel = clamp(t / T1), hit = clamp((t - T1) / (T2 - T1)), { c, cx, cy, R } = s, r = rng(9);
      const dir = 0.25, wide = t < T1 ? 0.06 : 0.3;
      c.globalAlpha = t < T1 ? 0.2 + tel * 0.3 : t < T2 ? 0.45 : 0.45 * (1 - clamp((t - T2) / (1 - T2)));
      c.fillStyle = col; c.beginPath(); c.moveTo(cx, cy); c.arc(cx, cy, R * 1.05, dir - wide, dir + wide); c.closePath(); c.fill(); c.globalAlpha = 1;
      if (t >= T1 && t < T2) for (let i = 0; i < 40; i++) {
        const p = (hit * 2 + r()) % 1, a = dir + (r() - 0.5) * wide * 1.6, d = R * (1 - p);
        c.fillStyle = "#e9ddff"; c.globalAlpha = 0.8; c.fillRect(cx + Math.cos(a) * d, cy + Math.sin(a) * d, 3, 3);
      }
      c.globalAlpha = 1;
    },
  };

  /** Draws the arena, the boss and one frame of an attack. */
  function draw(c, w, h, attack, t) {
    const s = scene(c, w, h), col = BOSSES[attack.boss].color;
    c.clearRect(0, 0, w, h);
    // arena
    ring(c, s.cx, s.cy, s.R * 1.04, "#3a3560", 2);
    for (let i = 1; i <= 3; i++) ring(c, s.cx, s.cy, s.R * i / 3.1, "#26223f", 1);
    for (let i = 0; i < 12; i++) { const a = i * TAU / 12; line(c, s.cx + Math.cos(a) * s.R * 0.3, s.cy + Math.sin(a) * s.R * 0.3, s.cx + Math.cos(a) * s.R, s.cy + Math.sin(a) * s.R, "#201c36", 1); }
    KINDS[attack.kind](s, t, col);
    // the player and the boss on top
    const [px, py] = attack.kind === "beam" || attack.kind === "meteor" || attack.kind === "leap" || attack.kind === "guillotine" || attack.kind === "lance" ? player(s, 0.1) : player(s, t);
    const bx = attack.kind === "leap" && t >= T1 && t < T2 ? NaN : s.cx;
    if (!Number.isNaN(bx)) { glow(c, s.cx, s.cy, s.R * 0.16, col, 0.6); c.fillStyle = "#0b0a12"; c.beginPath(); c.arc(s.cx, s.cy, s.R * 0.065, 0, TAU); c.fill(); ring(c, s.cx, s.cy, s.R * 0.065, col, 3); }
    c.fillStyle = "#fff"; c.beginPath(); c.arc(px, py, Math.max(5, s.R * 0.028), 0, TAU); c.fill(); ring(c, px, py, Math.max(8, s.R * 0.045), "#ffffff55", 1.5);
  }

  window.MSC_ATTACKS = { BOSSES, ATTACKS, draw, T1, T2 };
})();
