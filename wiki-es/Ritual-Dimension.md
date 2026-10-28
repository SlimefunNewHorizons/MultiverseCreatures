# 🌌 Dimensión del Ritual

La **Dimensión del Ritual** (mundo `boss_dimension`) es un mundo privado, solo para el jefe, donde se pelea contra el **Centinela de Obsidiana**. Es un campo de batalla calcinado de unos 1500 × 1500 bloques: una arena pavimentada en medio de un páramo de ríos y lagos de lava, cráteres, agujas de obsidiana, ruinas y los huesos de algo enorme, cerrado por una cordillera dentada de obsidiana, bajo un cielo rojo eterno. Sin clima, sin ciclo de día/noche y sin spawns de mobs naturales. La dimensión se genera automáticamente la primera vez que un jugador entra en ella.

> La dimensión está restringida a propósito **mientras el jefe está activo**: durante la pelea, los jugadores no pueden colocar ni romper bloques, y casi todos los comandos están bloqueados (solo `/say`, `/me`, `/help`, `/?` y `/msc dimtp` funcionan). Con el Centinela de Obsidiana inactivo, los jugadores vuelven a poder construir (p. ej. montar la Estructura del Ritual de salida) y usar todos los comandos con normalidad. Los administradores evitan las restricciones con el permiso `msc.admin.bypass`.

---

## 🏛️ El terreno

```
r ≤ 48       la arena: suelo pavimentado totalmente plano en y=40, un sigilo de obsidiana
             llorosa con anillos y radios, un borde cincelado; nunca hay nada encima
r ≤ 92       la entrada: escombros del pavimento que se pierden en el páramo, fáciles de caminar
r ≤ 110      sin lava, cráteres ni estructuras: la pelea siempre empieza en terreno seguro
más allá     el páramo, con tres tipos de suelo:
               valles de almas     arena y tierra de almas con fuego de almas azul
               llanuras calcinadas piedra negra y basalto con parches de magma (queman)
               campos ardientes    netherrack con fuego
             peligros: ríos y lagos de lava (superficie en y=34), cráteres (algunos inundados),
             agujas de obsidiana y basalto, columnatas en ruinas, costillares gigantes
borde ≥ 630  una cordillera dentada de obsidiana, basalto y piedra negra en franjas, picos de ~y=150
```

La lava solo llena una columna hasta y=34, y todas las columnas por debajo de ese nivel se llenan igual, así que la superficie de lava siempre toca más lava o roca y nunca se derrama. El borde del mundo (`boss-dimension.size`, 1500 por defecto, entre 100 y 1500) queda justo detrás de la cordillera.

**Regenerar.** El plugin deja un archivo `msc-generator.txt` en la carpeta del mundo con el generador que lo construyó. Una dimensión construida por un generador anterior se borra y se regenera una sola vez, en el siguiente arranque. `boss-dimension.reset-on-load: true` fuerza la regeneración; vuelve a ponerlo en false después. Ambas cosas destruyen todo lo construido en ese mundo.

---

## 🕯️ Entrar: la Estructura del Ritual

Para entrar en la dimensión debes construir y encender la **Estructura del Ritual** en el overworld.

### Distribución (7×7, a nivel del suelo)

```
. S S S S S .        S = escaleras de piedra negra pulida (borde)
S S C K C S S        C = piedra negra pulida cincelada
S C O B O C S        O = obsidiana llorosa
S K B X B K S        B = ladrillos de piedra negra pulida
S C O B O C S        K = ladrillos de piedra negra pulida agrietados
S S C K C S S        X = obsidiana (centro)
. S S S S S .
```

- El **bloque central** debe ser **obsidiana**.
- En la **segunda capa** (1 bloque por encima del suelo), coloca **12 velas** en un anillo alrededor del interior del borde:

```
. . c c c . .        velas (c) en la capa y+1:
. c . . . c .        3 en el borde superior (z=1, x=2-4)
. c . . . c .        2 en cada lado (x=1 y x=5, z=2-4)
. c . . . c .        3 en el borde inferior (z=5, x=2-4)
. . c c c . .
```

### Activación

1. Construye la estructura y coloca las 12 velas.
2. Enciende **cada vela** con pedernal y acero, una carga de fuego u otro objeto de vela.
3. Aparecen partículas de círculo de fuego rojo/azul y partículas de portal alrededor de la estructura.
4. Tras **~5 segundos**, cualquier jugador dentro del círculo (radio 5 desde el centro) es teletransportado a la Dimensión del Ritual (recibe un efecto breve de ceguera — *"There is no escape."*).

> Solo puede haber un ritual activo por mundo a la vez. Si la estructura se rompe o las velas se apagan, el ritual se detiene.

---

## ⚔️ Invocar al jefe: el Círculo de Invocación

Una vez dentro de la dimensión, el **Centinela de Obsidiana** debe invocarse manualmente.

### Distribución (5×5, anillo de velas rojas)

```
_ R R R _        (anillo de 12 velas rojas — 3 por borde — interior vacío)
R _ _ _ R        centro: vacío — suelta el Fragmento de Eco aquí
R _ _ _ R
R _ _ _ R
_ R R R _
```

1. Coloca **12 velas rojas** en un anillo de 5×5 (las esquinas y bordes de un cuadrado, dejando el centro vacío).
2. Enciende **todas** con pedernal y acero o una carga de fuego.
3. Una animación de **pentagrama** en llamas aparece en el medio mientras la invocación está activa.
4. **Suelta un Fragmento de Eco** (`echo_shard`) en el centro del círculo.
5. El fragmento se consume, las velas se apagan y el **Centinela de Obsidiana** despierta en el centro.

> Matar al Centinela dropea un **Núcleo Centinela** (probabilidad configurable, `armor-stand-boss.sentinel-core-drop-chance`, por defecto 100%) — un ingrediente clave para el **Grimorio Centinela** y otras recetas cumbre.

---

## 🪓 Invocación de NIX: El Cadalso del Verdugo

**NIX - El Verdugo** solo puede ser invocado de forma exclusiva dentro de la **Dimensión del Jefe** (`boss_dimension`). Los jugadores deben erigir **El Cadalso del Verdugo** y ofrecer un sacrificio de sangre sobre el yunque.

> **Las coordenadas son relativas**: marcan la posición de cada bloque respecto a la **esquina suroeste del 5×5** (tu punto de origen). No hay que construirlo en un lugar fijo del mundo: el ritual detecta el patrón en cualquier sitio.
>
> **El suelo no es específico**: el plugin no comprueba el bloque del suelo ni los alrededores, solo los bloques de la estructura. Puedes montarlo sobre obsidiana llorosa, tierra, lo que sea.

### Distribución (Huella 5×5)

```
P . . . P        P = Poste de Cadalso (3 bloques de alto)
. . c . .        c = Vela Roja (encendida)
. c Y c .        Y = Yunque Central (El Tajo de Decapitación)
. . c . .
P . . . P
```

### Materiales Requeridos
- **1 Yunque Central** (`anvil`, `chipped_anvil` o `damaged_anvil`) en el centro: `(2, 0, 2)`.
- **4 Velas Rojas** (`red_candle`) a nivel del suelo, una a cada lado del yunque:
  - Norte `(2, 0, 1)`, Sur `(2, 0, 3)`, Oeste `(1, 0, 2)`, Este `(3, 0, 2)`.
- **4 Postes de Cadalso en las Esquinas**: `(0, 0)`, `(4, 0)`, `(0, 4)` y `(4, 4)`. Cada poste son **3 bloques de alto**:
  - `Y=0` — Base: ladrillos de piedra negra pulida, piedra negra pulida, ladrillos de pizarra profunda, pizarra pulida, obsidiana llorosa o bloque de hierro.
  - `Y=1` — Cadena: `iron_chain` o cualquier bloque con nombre que termine en `chain`.
  - `Y=2` — Calavera: de esqueleto, wither, jugador o zombie (normal o de pared).

### Procedimiento de Invocación
1. Construye el cadalso en cualquier superficie de la `boss_dimension`.
2. Enciende las **4 velas rojas** con un mechero o carga ígnea.
3. **Activa el ritual**: haz clic derecho sobre cualquiera de las velas rojas (con el mechero en la mano o con cualquier objeto). Será entonces cuando el plugin compruebe que la estructura está completa y las 4 velas encendidas.
4. **Efecto de Invocación Activa** (mientras dura):
   - Rayos y partículas de sangre carmesí (`#8B0000`) se conectan desde las 4 calaveras de los postes hacia el yunque central.
   - Suenan cadenas pesadas a intervalos mientras una densa humareda oscura brota del yunque.
   - Si rompes la estructura o las velas se apagan, el ritual se **cancela** y debes empezar de nuevo.
5. **La Ofrenda de Sangre**: suelta el objeto sobre el yunque central (a menos de **3 bloques** de él y a la altura del suelo):
   - Una **`Sentencia de Muerte`** (`Executioner's Warrant`, con `/msc give executionerwarrant` o crafteable).
   - *(También se aceptan como ofrendas alternativas un `Hacha de Netherita` o una `Calavera de Wither Skeleton`)*.
6. **Aparición**:
   - El sacrificio es consumido al instante.
   - Un rayo carmesí azota el yunque con un impacto ensordecedor de guillotina (`Sound.BLOCK_ANVIL_LAND`).
   - Las velas se apagan y **NIX - El Verdugo** se materializa sobre el yunque desatando el combate.

> Mientras NIX esté activo dentro de la Dimensión del Jefe, la colocación y rotura de bloques, así como los comandos de escape, permanecen bloqueados.

---

## ⏱️ Invocación de DIO: El Trono de The World

**DIO** solo se puede invocar dentro de la **Boss Dimension** (`boss_dimension`): las velas y la ofrenda no hacen nada en ningún otro sitio. Las coordenadas son relativas a la esquina suroeste del 5×5 y el suelo no se comprueba.

### Diseño (5×5)

```
S . . . S        S = Pilar de esquina (3 bloques de alto)
. . c . .        c = Vela Amarilla (encendida)
. c G c .        G = Bloque de Oro (el trono)
. . c . .
S . . . S
```

### Materiales necesarios
- **1 Bloque de Oro** en el centro `(2, 0, 2)`: el trono.
- **4 Velas Amarillas** (`yellow_candle`) a su lado: Norte `(2, 0, 1)`, Sur `(2, 0, 3)`, Oeste `(1, 0, 2)`, Este `(3, 0, 2)`.
- **4 Pilares de esquina** en `(0, 0)`, `(4, 0)`, `(0, 4)` y `(4, 4)`, cada uno de **3 bloques de alto**:
  - `Y=0` — Bloque de Oro.
  - `Y=1` — Bloque de Esmeralda.
  - `Y=2` — cualquier cabeza o calavera (de jugador, esqueleto, esqueleto wither o zombi; de pie o en pared). La cabeza de The World también sirve.

### Procedimiento
1. Construye el trono en cualquier lugar de `boss_dimension`.
2. Enciende las **4 velas amarillas** y haz clic derecho en una de ellas.
3. **El trono despierta**: luz dorada y verde va de las cuatro cabezas al trono, y aparece encima una esfera de reloj dorada cuya aguja avanza una vez por segundo con un tic-tac. Si rompes la estructura o se apaga una vela, se cancela.
4. **La ofrenda**: suelta un **Reloj** sobre el trono (a menos de **3 bloques**).
5. **ZA WARUDO**: el reloj se consume, una esfera de tiempo detenido sale del trono y todos a menos de 20 bloques quedan retenidos un momento. Dos segundos y medio después cae un rayo y **DIO** aparece sobre el trono con The World detrás.

> Solo una invocación por mundo a la vez, y el trono no despierta si DIO ya está vivo allí. Mientras DIO pelea, se aplican los bloqueos de construcción y comandos de la Boss Dimension como con los otros jefes.

---

## 🌺 Invocación de Malenia: La Flor Escarlata

**Malenia, Espada de Miquella** se invoca dentro de la **Boss Dimension** (`boss_dimension`) en la Flor Escarlata, el corazón de la putrefacción. Las coordenadas son relativas a la esquina suroeste del 5×5 y el suelo no se comprueba. `/mscwiki malenia` muestra la estructura delante de ti.

### Estructura (5×5)

```
P . . . P        P = Pilar de esquina (2 bloques de alto)
. . c . .        c = Vela magenta (encendida)
. c W c .        W = Bloque de verruga del Nether (el corazón)
. . c . .
P . . . P
```

### Materiales
- **1 Bloque de verruga del Nether** (`nether_wart_block`) en el centro `(2, 0, 2)`.
- **4 Velas magenta** (`magenta_candle`) a su lado: Norte `(2, 0, 1)`, Sur `(2, 0, 3)`, Oeste `(1, 0, 2)`, Este `(3, 0, 2)`.
- **4 Pilares** en `(0, 0)`, `(4, 0)`, `(0, 4)` y `(4, 4)`, cada uno de **2 bloques**:
  - `Y=0` — Hifas carmesí (`crimson_hyphae`).
  - `Y=1` — Brillongo (`shroomlight`).

### Procedimiento
1. Construye la estructura en cualquier lugar de `boss_dimension`.
2. Enciende las **4 velas magenta** y haz clic derecho en una.
3. **La putrefacción despierta**: hilos escarlata van de los cuatro brillongos al corazón y un capullo de podredumbre gira encima. Si rompes la estructura o se apaga una vela, se cancela.
4. **La ofrenda**: suelta una **Antorchaflor** (`torchflower`, la Aeonia Escarlata) sobre el corazón (a menos de **3 bloques**).
5. **La floración**: la flor se consume y una flor escarlata se abre sobre el corazón durante dos segundos y medio; todo jugador a menos de 60 bloques ve *Malenia — Blade of Miquella*. Luego estalla y **Malenia** aparece en ella.

> Solo una invocación por mundo a la vez, y el corazón no despierta si Malenia ya está viva allí. Mientras pelea, se aplican los bloqueos de construcción y comandos de la Boss Dimension. Los admins pueden seguir llamándola en cualquier sitio con `/msc spawn malenia`.

---

## 💻 Invocación de Jack Star: La Terminal del Multiverso

**JackStar — El Arquitecto del Sistema** se invoca dentro de la **Boss Dimension** en la Terminal del Multiverso. Las coordenadas son relativas a la esquina suroeste del 5×5 y el suelo no se comprueba. `/mscwiki jack` muestra la estructura.

### Estructura (5×5)

```
L . . . L        L = Pilar de esquina (2 bloques de alto)
. . c . .        c = Vela cian (encendida)
. c T c .        T = Núcleo de la terminal
. . c . .
L . . . L
```

### Materiales
- **1 Núcleo** en `(2, 0, 2)`: Nexo de reaparición, Magnetita, Mesa de encantamientos o Faro.
- **4 velas** a su lado (Norte, Sur, Oeste, Este): cian, azul claro, azul o normales, o fogatas de almas.
- **4 Pilares** en las esquinas, cada uno de **2 bloques**:
  - `Y=0` — Obsidiana llorosa, Obsidiana, Ladrillos de piedra negra pulida, Piedra negra pulida cincelada, Ladrillos de pizarra profunda o Bloque de hierro.
  - `Y=1` — Pararrayos.

### Procedimiento
1. Construye la terminal en `boss_dimension`, enciende las **4 velas** y haz clic derecho en una.
2. La terminal se autentica y empieza su secuencia de hackeo.
3. **La ofrenda**: suelta una **Estrella del Nether**, un **Fragmento de Eco**, un **Corazón del Mar**, un **Faro** o un **Architect Kernel** sobre el núcleo.
4. **JackStar** arranca sobre la terminal.

---

## 🏛️ Dioses de DrakesBosses: el Altar del Panteón

Con **DrakesBosses** instalado (dependencia suave), sus dioses también se pueden invocar dentro de `boss_dimension`. Sin DrakesBosses el altar no responde. DrakesBosses spawnea al dios con sus propias stats, habilidades, loot y recompensas.

### Diseño (5×5)

```
P . . . P        P = Pilar de esquina (2 bloques de alto)
. . c . .        c = Vela del panteón (encendida)
. c A c .        A = Núcleo del altar
. . c . .
P . . . P
```

| Panteón | Velas | Núcleo `(2, 0, 2)` | Pilares |
|---|---|---|---|
| **Olimpo** | `white_candle` | `chiseled_quartz_block` | `quartz_pillar` |
| **Asgard** | `green_candle` | `chiseled_deepslate` | `spruce_log` |
| **Duat** | `orange_candle` | `chiseled_sandstone` | `smooth_sandstone` |
| **el Vacío** | `purple_candle` | `end_stone_bricks` | `purpur_pillar` |

### Ofrendas (por defecto, configurables en `drakes-bosses.offerings`)

| Panteón | Dios ← ofrenda |
|---|---|
| Olimpo | Zeus ← `lightning_rod` · Poseidón ← `heart_of_the_sea` · Hades ← `wither_skeleton_skull` · Ares ← `netherite_sword` · Artemisa ← `spectral_arrow` · Prometeo ← `fire_charge` · Circe ← `amethyst_shard` · Polifemo ← `fermented_spider_eye` · Kratos ← `netherite_axe` · Tifón ← `magma_block` · Hidra ← `prismarine_shard` · Cerbero ← `bone_block` |
| Asgard | Thor ← `iron_block` · Odín ← `gold_block` · Loki ← `ender_pearl` · Heimdall ← `blaze_rod` |
| Duat | Ra ← `golden_carrot` · Isis ← `feather` · Anubis ← `rotten_flesh` · Set ← `redstone_block` |
| el Vacío | Coloso del End ← `echo_shard` · Garou Cósmico ← `nether_star` · Dios Corrupto ← `totem_of_undying` · Wither Storm ← `wither_rose` · Dragón Ancestral ← `dragon_breath` · Jax ← `lantern` |

### Procedimiento
1. Construye el altar del panteón en cualquier lugar de `boss_dimension` (coordenadas relativas a la esquina suroeste; el suelo no se comprueba).
2. Enciende las **4 velas** y haz clic derecho en una de ellas. El altar despierta: luz desde los pilares, un pentagrama girando en el suelo, y el chat lista las ofrendas que acepta.
3. **Suelta (Q) la ofrenda** del dios sobre el núcleo (a menos de **3 bloques**). Se consume **una** unidad; el resto del stack queda en el suelo. Una ofrenda de otro panteón se rechaza sin consumirse.
4. Tras `drakes-bosses.arrival-delay-ticks` (40 por defecto) cae un rayo y el dios aparece sobre el núcleo. Si DrakesBosses no puede spawnearlo (p. ej. Jax desactivado), la ofrenda se devuelve.

> Un altar por mundo a la vez, y no despierta si ya hay un jefe peleando en la dimensión. Mientras el dios vive se aplican los bloqueos de construcción y comandos. Un dios sin jugadores cerca se retira como cualquier otro jefe (`boss-balance.despawn`: 50 bloques, 5 s) para no bloquear la dimensión.

---

## ⚖️ Reglas de supervivencia dentro de la dimensión

Sobrevivir en la dimensión es más duro que en el overworld. Cada regla es un interruptor bajo `boss-dimension` en `config.yml`; los espectadores no se ven afectados.

| Regla | Por defecto | Qué hace |
|---|---|---|
| `natural-regeneration` | `false` | Solo curan los **efectos de poción**: curación instantánea y regeneración (las manzanas doradas y los tótems dan regeneración, así que siguen sirviendo). La barra de comida llena, las curaciones de otros plugins y cualquier otra fuente no curan. |
| `saturation-effect` | `false` | El efecto **Saturación** no hace nada ahí: el hambre solo se recupera comiendo. |
| `flight` | `false` | **Nadie vuela**: el `/fly` de otros plugins, el vuelo en creativo y las élitras quedan desactivados, y si otro plugin lo devuelve se quita en menos de un cuarto de segundo. Los jugadores en creativo recuperan el vuelo al salir. |
| `fall-damage-multiplier` | `1.7` | Las caídas hacen un **70 % más** de daño. |
| `fall-true-damage-share` | `0.2` | **El 20 % del daño de una caída es daño verdadero**, como el que hace Mahoraga: ni la armadura, ni Protección, ni Caída de pluma, ni Resistencia, ni los topes de otros plugins lo frenan. El otro 80 % se reduce como siempre. |
| `gravity-multiplier` | `0.7` | Los jugadores tienen un **30 % menos de gravedad**: saltan más alto y caen más despacio. Se quita al salir. |
| `resistance-effectiveness` | `0.9` | El efecto de **Resistencia** bloquea un 10 % menos: Resistencia I bloquea un 18 % en vez de un 20 %. También cuenta contra el daño verdadero de los jefes. |
| `regeneration-speed` | `0.5` | El efecto de **Regeneración** cura la mitad de rápido. |

Todos los jefes, donde sea que se peleen, reciben como máximo `max-damage-per-hit` (**10**) por golpe.

---

## 🚪 Salir

Con el jefe inactivo puedes salir directamente con **`/msc dimtp`** (los comandos solo se bloquean durante la pelea). Si quieres salir por medios del juego, o si el jefe sigue activo, la única salida es **el mismo ritual usado para entrar**:

1. Construye la **Estructura del Ritual** (la distribución 7×7 de piedra negra pulida con 12 velas blancas descrita arriba) dentro de la dimensión.
2. Enciende **las 12 velas**.
3. Tras ~5 segundos, los jugadores dentro del círculo son teletransportados de vuelta al **spawn del overworld**.

> Si el plugin se desactiva/recarga con jugadores dentro, todos son enviados de vuelta al spawn del overworld automáticamente.

---

## 🧰 Notas técnicas

- Nombre del mundo: `boss_dimension` (se crea en la primera entrada, se descarga al desactivar el plugin).
- Si otro plugin (como Multiverse) carga `boss_dimension` sin el generador de este plugin, al arrancar el plugin lo guarda, lo descarga y lo vuelve a cargar con el generador del páramo, para que los chunks nuevos nunca sean terreno vanilla. Los chunks generados antes se quedan como están. También se le puede dar a un gestor de mundos el generador `MultiverseCreatures`.
- Punto de spawn: `0.5, 41, 0.5`, el centro de la arena.
- Reglas del mundo: sin ciclo de día/noche, sin ciclo de clima, sin spawns de mobs, reaparición inmediata, sin anuncios de avances.
- El cielo se fuerza a rojo vía un override de bioma; `boss-dimension.red-sky: false` lo deja como está (el tinte se aplica a un bioma vanilla, así que es global).
- Config: `boss-dimension.red-sky`, `boss-dimension.size` y `boss-dimension.reset-on-load`.
- Clases relevantes: `BossDimensionManager`, `Wasteland`, `WastelandGenerator`, `BossInvocationManager`, `RitualManager`, `RitualStructure`, `BossInvocationStructure`, `PantheonInvocationManager`, `PantheonAltarStructure`, `PantheonGod` — ver [Arquitectura](./dev/Architecture.md) y [Tests](./dev/Tests.md).