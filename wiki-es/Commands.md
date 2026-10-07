# 🛠️ Comandos

Todos los comandos usan la raíz **`/msc`**. **Permiso:** `msc.admin` (OP del servidor por defecto). `msc.admin.bypass` (también OP) omite las reglas de construcción y teletransporte de la dimensión protegida del jefe.

```
/msc                           Muestra el menú de ayuda estructurado y categorizado
/msc spawn <tipo>              Invoca un mob, jefe, strike force militar, etc. (/msc spawn help [1-3])
/msc give <objeto> [cantidad] [jugador]  Entrega armas legendarias, armaduras o componentes (/msc give help [1-4])
/msc attack <nombre> [rango]   Dispara un ataque o mecánica del ArmorStandBoss (/msc attack help [1-7])
/msc music <play|stop|list|disc>  Reproduce / detiene canciones NBS, obtén un disco de jukebox
/msc dimtp <mundo>             Teletransporta entre mundos
/msc kill [tipo|all] [radio]   Purga criaturas de MSC de forma segura
/msc debug [jugador]           Desglosa el daño de cada jefe hacia y desde un jugador
/msc debug geometry [jefe]     Dibuja la hitbox y las articulaciones de un jefe, o reproduce su caminar
/msc reload                    Recarga config.yml, fusiona los defaults nuevos y sincroniza entidades y jefes
```

Cada comando se detalla abajo.

`spawn` y `give` aceptan un único nombre por entidad u objeto: el de las tablas de abajo, que es también lo que ofrece el autocompletado. Los nombres que aceptan `spawn`, `give` y `attack` se declaran en una sola tabla por comando (`SpawnCatalogue`, `GiveCatalogue` y `AttackCatalogue`): la misma tabla alimenta el ejecutor, el menú de ayuda y el tab-complete.

---

## /msc spawn <tipo>

Invoca una sola entidad (o una formación táctica) en la ubicación del ejecutor. Los siguientes tipos son compatibles:

| Tipo | Entidad |
|---|---|
| `merchant` | Comerciante Multiversal ("Shaggy" Comerciante Errante) |
| `mahoraga` | Minijefe Mahoraga |
| `kinger` | Minijefe Kinger |
| `garou` | Minijefe Garou [Hero Hunter] |
| `nix` | NIX - El Verdugo (jefe con modelo custom de 27 piezas) |
| `dio` | DIO con su Stand The World (jefe de JoJo's Bizarre Adventure) |
| `witherstorm` | WITHER STORM, nacido de su vórtice en su primera forma (Cracker's Wither Storm Mod) |
| `witherstorm2` … `witherstorm5` | WITHER STORM ya crecido: Joroba Creciente, Joroba Hinchada, Destructor, Devorador |
| `armorstand` | EL CENTINELA DE OBSIDIANA, jefe final |
| `jack` | JACK STAR — El Arquitecto del Sistema (5 fases, 3 vidas) |
| `creeperjr` | Creeper Jr. (×3 — aparece en trío) |
| `headslime` | Head Slime |
| `zombietrap` | Trampa de Caballo Zombie Militar (emboscada de ejército completo de 5 unidades) |
| `tank` | Zombie Tank (unidad única) |
| `duelist` | Skeleton Duelist militar |
| `lancer` | Zombie Lancer + ZombieHorse |
| `camel` | Camel del Ejército con jinetes |
| `sniper` | Skeleton Sniper |
| `boneshield` | Bone Shield |
| `chaosmage` | Mago del Caos |
| `enderknight` | Caballero Ender |
| `flameelemental` | Elemental de Llama |
| `frostgolem` | Gólem de Escarcha |
| `obsidianguard` | Guardia de Obsidiana |
| `shadowrogue` | Shadow Rogue |
| `soulreaper` | Segador de Almas |
| `stormcaller` | Invocador de Tormentas |
| `venomwitch` | Bruja de Veneno |
| `voidcrawler` | Void Crawler |
| `arrowskeleton` | Arquero de la Flecha (Flecha de Stand) |
| `warlord` | Orcish Warlord (furia berserker) |
| `disctrader` | Disc Trader — aldeano bibliotecario que vende discos de música |

Los detalles de cada entidad viven en [Jefes](./Bosses.md) y [Criaturas](./Creatures.md).

---

## /msc give <objeto> [cantidad] [jugador|@a|@p|@r|@s]

La cantidad por defecto es 1 y puede ser de 1 a 64. Sin destino el objeto va al ejecutor; si se indica, acepta el nombre exacto de un jugador o un selector (`@e` se rechaza a propósito).

### Armas

| Objeto |
|---|
| `excalibur` |
| `cindergreatsword` |
| `nullshearedge` |
| `soulreapscythe` |
| `venomfang` |
| `aetherpullshot` |
| `skyfiretalisman` |
| `sentinelgrimoire` |
| `chaosforge` |
| `executionerguillotine` |
| `architectdeployer` |

### Armaduras y reliquias

| Objeto |
|---|
| `eighthandledwheel` |
| `obsidianbastionhelmet` |
| `obsidianbastionchestplate` |
| `obsidianbastionleggings` |
| `obsidianbastionboots` |
| `marrowaegis` |
| `veilwalkermantle` |
| `frostheartoffhand` |

### Objetos varios

| Objeto |
|---|
| `icecrown` |
| `mantisclaws` |
| `wirtslantern` |
| `militarymine` |
| `scoobycookie` |
| `headslimegelatin` |
| `vampireblood` |
| `unstableblood` |
| `bearerelixir` |

### Componentes

| Objeto |
|---|
| `starcore` |
| `militarycomponent` |
| `swordmold` |
| `headslimeheart` |
| `chaosorb` |
| `chaospowder` |
| `chaosfragment` |
| `chaoscore` |
| `condensedchaosorb` |
| `enderfragment` |
| `frostheart` |
| `magmacore` |
| `obsidianshard` |
| `reaperessence` |
| `reinforcedbone` |
| `reinforcedboneblock` |
| `bonemarrow` |
| `ossifiedplate` |
| `moltenmarrow` |
| `shadowcloak` |
| `stormcrystal` |
| `venomgland` |
| `voidessence` |
| `wheelessence` |
| `executioneredge` |

### Catalizadores de jefe y bloques nucleares

| Objeto |
|---|
| `wheelcore` |
| `moltenwheelcore` |
| `refinedwheelcore` |
| `reapercore` |
| `sentinelcore` |
| `endercore` |
| `multiversalcore` |
| `compressedgoldblock` |
| `refinednetherite` |
| `moltennetherite` |
| `executionerwarrant` |
| `architectkernel` |
| `garoucosmiccore` |

---

## /msc attack <nombre> [rango]

Fuerza un ataque o mecánica del Centinela de Obsidiana más cercano dentro de `rango` bloques (100 por defecto). Cada ataque y mecánica tiene **un solo nombre** — los de abajo, que lista `/msc attack help [1-7]` y ofrece el autocompletado.

El comando se salta las reglas de distancia, enfriamiento y repetición de la IA, pero conserva las que protegen al jefe: nada empieza mientras otro ataque sigue en curso, un ataque aéreo necesita al jefe en el aire (primero `flyup`) y uno de suelo en el piso (primero `land`), y no se apila un sello, círculo de curación, defensa o tope de esbirros que ya esté activo. Cuando se niega, dice por qué.

### Ataques de suelo (24)

`groundslam`, `groundshatter`, `shieldbash`, `lancestorm`, `earthpillar`, `chaingrapple`, `warstomp`, `armorspikes`, `vortexpull`, `mirrorimage`, `doombeam`, `lanceflurry`, `whirlwindslash`, `executionsweep`, `obsidianspire`, `earthmaw`, `shadowstep`, `runeward`, `sunderingcharge`, `spearcyclone`, `cataclysm`, `tremorlance`, `aegisrush`, `gravecleaver`

### Ataques aéreos (20)

`starfall`, `aerialrush`, `sonicboom`, `lightningstorm`, `gravitywell`, `crossslash`, `novaburst`, `darkorb`, `windcutter`, `heavenlyjudgment`, `rainoflances`, `airslam`, `hoverbarrage`, `eclipsefall`, `bladering`, `obsidianwings`, `voidmeteor`, `phantomlegion`, `spiralstorm`, `chainhook`

### Ataques a distancia y mágicos (20)

`lancesnipe`, `meteorstorm`, `voidbeam`, `frostlance`, `lightningspear`, `shadowvolley`, `chainlightning`, `crystalbarrage`, `arcaneorb`, `voidrift`, `arcanemissiles`, `spiritbeam`, `soultethers`, `plaguebrand`, `runemines`, `obsidianprison`, `shardburst`, `gravityorb`, `javelinvolley`, `sweepinglaser`

### Defensas, sellos y curas (12)

`stoneskin`, `reflectbarrier`, `absorbshield`, `shieldseal`, `healingcircle`, `trianglecall`, `regeneration`, `soulsiphon`, `obsidiancocoon`, `bulwark`, `thornaura`, `afterimage`

### Ritos de invocación (10)

`lancesquires`, `obsidianmender`, `emberhounds`, `voidwisps`, `obsidianbrute`, `elementalconclave`, `shadowambush`, `necropolisrite`, `arcanecovenant`, `championcall`

### Cataclismos destructivos (10)

`orbitalstrike`, `meteorimpact`, `supernova`, `judgmentpillars`, `earthsplitter`, `voidcollapse`, `obsidiantsunami`, `solarlance`, `worldbreaker`, `apocalypserain`

### Mecánicas y transiciones de fase (7)

`flyup`, `land`, `reset`, `phaserage`, `phasebarrier`, `phasestorm`, `phasedespair`

La lista completa y los detalles están en la [página de Jefes](./Bosses.md).

---

## /msc debug [jugador]

Diagnostica el daño de los jefes. Sin argumento apunta al jugador al que estás mirando (hasta 30 bloques); también puedes indicar un nombre, la única forma de usarlo desde la consola.

El informe tiene una sección por jefe (el Obsidian Sentinel, Nix y Jack Star) y lee la **muestra más reciente** de ese jugador en ambos sentidos, así que deja que el jefe le golpee (o golpéale tú) una vez antes:

- **DEALT to player** — el ataque, el daño que pedía (`Intended`) y lo que el jugador recibió de verdad (`Applied`), tras su armadura y efectos.
- **TAKEN from player** — el golpe tal como llegó (`Hit`), la mecánica propia del jefe (`cap N`, el reparto del load balancer, los multiplicadores de defensa, un esquive…) y el daño que el jefe aplicó.

La entrada `DEALT` del Sentinel es el **desglose penetrante** (daño del evento, armadura/Protección/Resistencia devueltas, el cap por golpe y la perforación de Resistencia), porque todos sus golpes pasan por ese pipeline. La entrada `TAKEN` de Nix muestra su cap `max-damage-per-hit`; la de Jack Star, el reparto del Load Balancer.

```
OBSIDIAN SENTINEL
  ▸ DEALT to player · penetrating
      Event : 3.52 · armour : -17.60 · protection : 0.00 · resistance : -0.88
      Through armour : 22.00 · cap : 15.00 · pierce : 20% · Resistance : level 1
      Final damage dealt : 12.60
  ▸ TAKEN from player · Incoming hit
      Hit : 120.00 · stone skin ×0.5, cap 50.0 · Applied : 50.00

NIX - THE EXECUTIONER
  ▸ DEALT to player · Guillotine Cleave
      Intended : 22.00 · Applied : 9.90
  ▸ TAKEN from player · Incoming hit
      Hit : 300.00 · cap 100.0 · Applied : 100.00

JACK STAR - THE SYSTEM ARCHITECT
  ▸ DEALT to player · Three-Slash
      Intended : 16.00 · Applied : 8.40
  ▸ TAKEN from player · Incoming hit
      Hit : 134.00 · load balancer: 46.9 shared · Applied : 87.10
```

Las secciones sin datos se omiten; si aún no te ha tocado ningún jefe el comando lo dice en vez de mostrar un desglose vacío. Las muestras son transitorias: las de un jugador se pierden al desconectarse y las de un jefe al morir o despawnear, así que el informe siempre refleja la sesión actual. Los caps y multiplicadores están en `entities.<jefe>.*` en `config.yml`; mira [Instalación](./Installation.md) y [Jefes](./Bosses.md) para saber qué hace cada opción.

---

## /msc debug geometry [kinger|nix|jack|sentinel] [walk]

Dibuja en el mundo la **hitbox real** de un jefe y las **articulaciones** sobre las que giran sus extremidades durante diez segundos, siguiéndolo mientras se mueve. Apunta al jefe de ese tipo más cercano en 32 bloques (omite el nombre para cualquier jefe vestido); la geometría se dibuja con partículas, así que no aparece ni queda nada en el mundo.

- En **rojo**, las doce aristas de la caja del ArmorStand — la caja que recibe de verdad los golpes. Cada pieza visible tiene que quedar **dentro**; si no, un golpe a esa pieza se pierde.
- En **cian**, cada articulación: hombro, cadera, cintura, cuello y — para NIX y Jack Star, cuyos brazos y piernas se exportaron en dos segmentos — los **codos y las rodillas**. Una extremidad cuelga de su punto y su extremo lejano mantiene la distancia mientras gira.

El Centinela no tiene articulaciones propias (viste la armadura sobre el stand), así que solo dibuja la caja. Es la versión en juego de los tests de modelo: existe para revisar un modelo junto al jefe en vez de con un test temporal.

Añade **`walk`** (p. ej. `/msc debug geometry kinger walk`) para reproducir el **ciclo de caminar** de un modelo en vez de dibujar un jefe vivo: la misma hitbox roja y el esqueleto de las extremidades —pivotes y articulaciones en cian, huesos en azul, manos y pies en verde— caminando en el sitio sobre un rig a dos bloques y medio delante de ti, al ritmo al que camina el propio jefe. No se invoca ni se provoca a ningún jefe, así que el plegado de rodillas y codos se juzga por sí solo.

```
Drawing kinger for 10 s: hitbox 0.50 x 1.98 x 0.50 blocks (red), 8 joints (cyan).
Replaying kinger's walk in front of you for 10 s: hitbox (red), joints (cyan), bones (blue), hands and feet (green). Nothing was spawned.
```

---

## /msc music <play|stop|list|disc> [canción] [loop]

Reproduce cualquier archivo `.nbs` de `plugins/MultiverseCreatures/music/`. Las canciones se reproducen vía el `MusicManager` (paquetes de protocolo note-block-stub) para todos los jugadores cercanos dentro de un radio configurable.

```
/msc music list                Lista todas las canciones de la carpeta de música
/msc music play Undertale-Megalovania true   Reproduce (loop=true)
/msc music stop                Detiene la canción actual
/msc music disc Megalovania    Date el disco de jukebox de una canción
```

`/msc music disc <canción>` te da el disco de jukebox correspondiente — insértalo en un jukebox para reproducir la canción, click derecho con la mano vacía para expulsarlo. Ver [Música](./Music.md) para las canciones incluidas, créditos y el Disc Trader.

---

## /msc dimtp <mundo>

Teletransporta al ejecutor entre mundos/dimensiones. Se usa para probar el andamiaje de la dimensión del jefe y para saltar rápidamente entre overworld/nether/the_end.

---

## /wiki [página|hand|en|es]

Abre la wiki dentro del juego, para todos los jugadores (`msc.wiki`, concedido por defecto). Alias: `/mscwiki`, `/mwiki`.

- Nueve secciones: armas, armaduras, reliquias, comida y pociones, botín de criaturas, componentes fabricados, botín de jefes, Stands y jefes.
- Cada página muestra el ítem con su lore, su receta tal como la tiene el servidor (mesa de crafteo, horno, alto horno, soporte para pociones o intercambio), quién lo suelta con la probabilidad del propio servidor y en qué se usa. Haz clic en un ingrediente para abrir su página.
- Inglés y español: se abre en el idioma del cliente y la bandera de la esquina lo cambia (la elección se guarda en el jugador).
- `/wiki venomfang` abre una página directamente; `/wiki hand` abre la del ítem que sostienes; `/wiki es` o `/wiki en` cambia el idioma.

## Permisos

| Permiso | Por defecto | Descripción |
|---|---|---|
| `msc.admin` | Solo OP | Requerido para TODOS los subcomandos de `/msc` |
| *tus propios nodos* | — | Puertas opcionales por subcomando, declaradas en `commands.subcommand-permissions` |

Aún no hay permisos por objeto o por mob. Los administradores del servidor pueden restringir el comando detrás de un plugin de permisos (p. ej. LuckPerms) dando `msc.admin` solo al personal de confianza.

`commands.subcommand-permissions` asigna a cada subcomando un nodo extra. Un subcomando listado ahí necesita ese nodo **además de** `msc.admin`; lo que no aparezca, o esté en blanco, queda abierto para quien haya pasado la puerta principal, así que el `{}` que se envía no cambia nada:

```yaml
commands:
  permission: "msc.admin"
  op-only: true
  subcommand-permissions:
    debug: msc.debug
    attack: msc.attack
```