(() => {
  const $ = (s, el = document) => el.querySelector(s);
  const A = window.MSC_ATTACKS;

  // ---------------------------------------------------------------- data (from the wiki)
  const BOSSES = [
    { icon: "🛡️", name: "Obsidian Sentinel", src: "Final boss", color: "#ef4b5b", boss: "sentinel",
      text: "A 7.5×-scale animated armor stand and the climax of the plugin: five phases, 96 choreographed attacks, wings of fire and defences that change how you fight it.",
      tags: ["96 attacks", "5 phases", "Summons"] },
    { icon: "🌪️", name: "Wither Storm", src: "Cracker's Wither Storm Mod", color: "#8b5cf6", boss: "witherstorm", viewer: "wither-storm-destroyer",
      text: "Built from nothing but display entities. It grows by eating the world through five forms, from a hunchback to a hundred-block Devourer.",
      tags: ["5 forms", "Tractor beams", "Display entities"] },
    { icon: "🪓", name: "NIX", src: "The Executioner", color: "#e0457b", boss: "nix",
      text: "A towering executioner with a 27-piece model, procedural limb animation and an axe that follows his forearm. Every hit is true damage.",
      tags: ["True damage", "3 signatures", "Chains"] },
    { icon: "👨‍💻", name: "Jack Star", src: "The System Architect", color: "#38d6e6",
      text: "Five phases, three lives and a body built from eleven skin heads. He summons other bosses as subprocesses, but never hides behind them.",
      tags: ["3 lives", "5 phases", "Subprocesses"] },
    { icon: "⏱️", name: "DIO", src: "JoJo's Bizarre Adventure", color: "#f2c230", viewer: "the-world",
      text: "Walks in with ゴゴゴ letters drifting up and The World floating behind him. Below half health he enrages: time stops for longer and the knives multiply.",
      tags: ["Time stop", "Stand", "Final Hour"] },
    { icon: "⚙️", name: "Mahoraga", src: "Jujutsu Kaisen", color: "#9aa3b2",
      text: "The Divine General adapts to whatever kills it. A miniboss that learns from every fight.", tags: ["Adaptive", "Miniboss"] },
    { icon: "♟️", name: "Kinger", src: "The Amazing Digital Circus", color: "#d94fd5",
      text: "A chess-piece king with a fully animated custom model that walks, cleaves and guards.", tags: ["Custom model", "Miniboss"] },
    { icon: "👊", name: "Garou", src: "One-Punch Man", color: "#ff7a2a",
      text: "A martial-arts miniboss, and one of the bosses Jack Star and the Sentinel can call through the gate.", tags: ["Miniboss", "Summonable"] },
  ];

  const STANDS = [
    { name: "Star Platinum", key: "star-platinum", face: "star-platinum", color: "#7c8cff", text: "Precise and overwhelmingly strong." },
    { name: "The World", key: "the-world", color: "#f2c230", text: "Stops time. DIO's Stand, drawn with eleven player heads." },
    { name: "Killer Queen", key: "killer-queen", face: "killer-queen", color: "#ff7ac3", text: "Turns anything it touches into a bomb, and brings Sheer Heart Attack." },
    { name: "Crazy Diamond", key: "crazy-diamond", face: "crazy-diamond", color: "#ff9bd6", text: "Restores what was broken." },
    { name: "Magician's Red", key: "magicians-red", face: "magicians-red", color: "#ff5a3a", text: "A bird-headed Stand that commands fire." },
    { name: "DIO Brando", key: "dio-brando", face: "dio-brando", color: "#ffd24a", text: "DIO's own skin set, on the same eleven heads." },
  ];

  const CREATURES = [
    ["👻", "Head Slime", "Half-Life", "A parasitic slime that leaps onto its target and rides on its head.", "#7bd88f"],
    ["💥", "Creeper Jr.", "Original", "A swarm of three tiny, fast creepers that leap-fuse and deal true damage.", "#4caf50"],
    ["🐴", "ZombieHorseTrap", "Military Army", "A rare Full Moon trap: step too close and a coordinated five-unit army appears.", "#9aa3b2"],
    ["⛑️", "Obsidian Guard", "Heavy tank", "A full-netherite tank zombie.", "#6b5b95"],
    ["🌌", "Ender Knight", "Original", "An Enderman-themed knight with a Sharp V Ender Blade.", "#b266ff"],
    ["❄️", "Frost Golem", "Original", "An iron golem reimagined as a winter guardian.", "#7fd6ff"],
    ["🔥", "Flame Elemental", "Original", "A blaze with homing meteor projectiles.", "#ff7a2a"],
    ["⚡", "Storm Caller", "Original", "A witch that bends lightning.", "#ffe14a"],
    ["🧪", "Venom Witch", "Original", "A poison specialist.", "#7bd800"],
    ["⚰️", "Soul Reaper", "Original", "A wither skeleton wielding a lifestealing scythe.", "#8a8fb8"],
    ["🌀", "Chaos Mage", "Original", "An evoker that casts one of six random chaotic spells.", "#e060e0"],
    ["🕳️", "Void Crawler", "Original", "A phase-shifting ambush spider.", "#5b4fcf"],
    ["🗡️", "Shadow Rogue", "Original", "A fast skeleton assassin.", "#4a4a6a"],
    ["🦴", "Bone Shield", "Original", "A defensive skeleton with a recharging bone wall.", "#d8d2bd"],
    ["🛒", "Multiverse Merchant", "Shaggy", "A wandering trader with custom trades from across the multiverse.", "#f2c230"],
  ];

  const FORMS = [
    { key: "wither-storm-hunchback", n: 1, name: "The Hunchback", size: "3 × 3.5", hp: "×1", beam: 20, at: "200" },
    { key: "wither-storm-growing", n: 2, name: "Growing Hunchback", size: "3.3 × 3.7", hp: "×1.5", beam: 26, at: "600" },
    { key: "wither-storm-pregnant", n: 3, name: "Swollen Hunchback", size: "11.5 × 11.7", hp: "×2", beam: 32, at: "1500" },
    { key: "wither-storm-destroyer", n: 4, name: "The Destroyer", size: "61 × 62", hp: "×3.5", beam: 64, at: "6000" },
    { key: "wither-storm-devourer", n: 5, name: "The Devourer", size: "110 × 114", hp: "×5", beam: 96, at: "—" },
  ];

  const esc = s => String(s).replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const viewerUrl = (key, extra = "") => `viewer/?stand=${key}${extra}`;

  // ---------------------------------------------------------------- bosses
  $("#bossGrid").innerHTML = BOSSES.map(b => `
    <article class="card reveal" style="--accent:${b.color}">
      <div class="ico">${b.icon}</div>
      <h3>${esc(b.name)}</h3><div class="src">${esc(b.src)}</div>
      <p>${esc(b.text)}</p>
      <div class="tags">${b.tags.map(t => `<span class="tag">${esc(t)}</span>`).join("")}</div>
      ${b.boss ? `<button class="go" data-boss="${b.boss}">See its attacks ↓</button>` : ""}
      ${b.viewer ? `<a class="go" href="${viewerUrl(b.viewer)}" style="margin-left:${b.boss ? 14 : 0}px">View in 3D →</a>` : ""}
    </article>`).join("");

  // ---------------------------------------------------------------- stands
  $("#standGrid").innerHTML = STANDS.map(s => `
    <article class="card reveal" style="--accent:${s.color}">
      ${s.face ? `<div class="face" style="background-image:url(viewer/skins/${s.face}/00_head.png)"></div>` : `<div class="ico">✋</div>`}
      <h3>${esc(s.name)}</h3><div class="src">JoJo's Bizarre Adventure</div>
      <p>${esc(s.text)}</p>
      <a class="go" href="${viewerUrl(s.key, s.face ? "&painted" : "")}">View in 3D →</a>
    </article>`).join("");

  // ---------------------------------------------------------------- creatures
  $("#creatureGrid").innerHTML = CREATURES.map(([icon, name, src, text, color]) => `
    <article class="card reveal" style="--accent:${color}">
      <div class="ico">${icon}</div><h3>${esc(name)}</h3><div class="src">${esc(src)}</div><p>${esc(text)}</p>
    </article>`).join("");

  // ---------------------------------------------------------------- attack lab
  const state = { boss: "all", id: A.ATTACKS[0].id };
  const filters = $("#filters"), list = $("#alist"), canvas = $("#stage"), c = canvas.getContext("2d");
  const phases = { t: $(".phase.t"), h: $(".phase.h"), r: $(".phase.r") };
  let t0 = performance.now(), reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;

  function renderFilters() {
    const chips = [["all", "All"], ...Object.entries(A.BOSSES).map(([k, v]) => [k, v.name])];
    filters.innerHTML = chips.map(([k, n]) => `<button class="chip" data-f="${k}" aria-pressed="${state.boss === k}">${esc(n)}</button>`).join("");
  }
  function renderList() {
    const items = A.ATTACKS.filter(a => state.boss === "all" || a.boss === state.boss);
    if (!items.some(a => a.id === state.id)) state.id = items[0].id;
    list.innerHTML = items.map(a => `<button class="aitem" data-id="${a.id}" style="--c:${A.BOSSES[a.boss].color}" aria-pressed="${a.id === state.id}"><i></i>${esc(a.name)}<small>${esc(a.tag)}</small></button>`).join("");
    renderInfo();
  }
  function renderInfo() {
    const a = A.ATTACKS.find(x => x.id === state.id);
    $("#aname").textContent = a.name;
    $("#aby").textContent = `${A.BOSSES[a.boss].name} · ${a.tag}`;
    $("#atext").textContent = a.text;
    t0 = performance.now();
  }
  filters.addEventListener("click", e => { const b = e.target.closest("[data-f]"); if (!b) return; state.boss = b.dataset.f; renderFilters(); renderList(); });
  list.addEventListener("click", e => { const b = e.target.closest("[data-id]"); if (!b) return; state.id = b.dataset.id; renderList(); });
  document.addEventListener("click", e => {
    const b = e.target.closest("[data-boss]"); if (!b) return;
    state.boss = b.dataset.boss; renderFilters(); renderList(); $("#lab").scrollIntoView({ behavior: reduced ? "auto" : "smooth" });
  });

  const LOOP = 5200;
  function fit() {
    const dpr = Math.min(2, devicePixelRatio || 1), w = canvas.clientWidth, h = canvas.clientHeight;
    if (canvas.width !== Math.round(w * dpr)) { canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr); }
    c.setTransform(dpr, 0, 0, dpr, 0, 0);
    return [w, h];
  }
  let visible = true;
  new IntersectionObserver(es => { visible = es[0].isIntersecting; }, { threshold: 0.05 }).observe(canvas);
  function frame(now) {
    requestAnimationFrame(frame);
    if (!visible) return;
    const [w, h] = fit(), a = A.ATTACKS.find(x => x.id === state.id);
    const t = reduced ? 0.55 : ((now - t0) % LOOP) / LOOP;
    A.draw(c, w, h, a, t);
    phases.t.classList.toggle("on", t < A.T1);
    phases.h.classList.toggle("on", t >= A.T1 && t < A.T2);
    phases.r.classList.toggle("on", t >= A.T2);
  }
  renderFilters(); renderList(); requestAnimationFrame(frame);

  // ---------------------------------------------------------------- wither storm
  const hp = [1, 1.5, 2, 3.5, 5];
  $("#forms").innerHTML = FORMS.map((f, i) => `
    <button class="form reveal" data-key="${f.key}" aria-pressed="${i === 3}">
      <div class="bars">${FORMS.map((_, j) => `<i style="height:${j <= i ? 12 + j * 22 : 6}%;opacity:${j === i ? 1 : j < i ? .5 : .2}"></i>`).join("")}</div>
      <div class="n">FORM ${f.n}</div><h4>${esc(f.name)}</h4>
      <dl><dt>Size</dt><dd>${esc(f.size)}</dd><dt>Health</dt><dd>${esc(f.hp)}</dd><dt>Beam reach</dt><dd>${f.beam}</dd><dt>Evolves at</dt><dd>${esc(f.at)}</dd></dl>
    </button>`).join("");
  let formKey = "wither-storm-destroyer", anim = "roar";
  const frameBox = $("#preview");
  function loadPreview() {
    frameBox.innerHTML = `<iframe title="Wither Storm 3D viewer" loading="lazy" src="${viewerUrl(formKey, "&anim=" + anim)}"></iframe>`;
  }
  $("#forms").addEventListener("click", e => {
    const b = e.target.closest("[data-key]"); if (!b) return;
    formKey = b.dataset.key;
    document.querySelectorAll("#forms .form").forEach(x => x.setAttribute("aria-pressed", x === b));
    if (frameBox.querySelector("iframe")) loadPreview();
  });
  $("#load3d").addEventListener("click", loadPreview);

  // ---------------------------------------------------------------- reveal on scroll
  const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); } }), { threshold: 0.08 });
  document.querySelectorAll(".reveal").forEach(el => io.observe(el));
  new MutationObserver(() => document.querySelectorAll(".reveal:not(.in)").forEach(el => io.observe(el))).observe(document.body, { childList: true, subtree: true });
})();
