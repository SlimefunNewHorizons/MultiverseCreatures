# 🐉 Jefes

MultiverseCreatures incluye **un jefe final** y múltiples y formidables **minijefes y jefes con modelos personalizados**. Todos los jefes se invocan con `/msc spawn <tipo>` (solo OP) y tienen salud/daño/cooldowns configurables en `config.yml`.

> **Jefes abandonados:** cualquier jefe (Centinela, Nix, Jack, DIO, Kinger, Mahoraga, Garou, la Tormenta Wither y los dioses del Panteón) sin un jugador a menos de `boss-balance.despawn.radius` (50 bloques; `large-radius` 100 para el Centinela y las formas colosales de la Tormenta Wither) durante `delay-ticks` (100, 5 s) desaparece con todo lo que invocó, para que una pelea abandonada no siga gastando el servidor. Los espectadores no cuentan. `enabled: false` lo desactiva.

---

## 🛡️ EL CENTINELA DE OBSIDIANA — Jefe final

Un ArmorStand animado gigante de escala 7.5×. El clímax del plugin. A diferencia de los jefes vestidos, el Centinela **es** el stand, así que su escala es a la vez el tamaño del modelo y la caja que golpean los jugadores.

| Estadística | Valor por defecto |
|---|---|
| Salud | `armor-stand-boss.health` (por defecto 3200) |
| Hitbox | `armor-stand-boss.hitbox-scale` (7.5) — escala del propio stand del jefe: tamaño del modelo y hitbox a la vez, unos catorce bloques de guerrero (acotado a 0.25–8) |
| Barra de jefe | `SEGMENTED_6`, roja → azul a lo largo de las fases (ver `phase-thresholds`) |
| Música | `Undertale — Megalovania` (radio de 60 bloques, se detiene al morir) |
| Equipamiento | Netherita completa (trim Amatista/Silencio) + Lanza de Netherita + Escudo irrompible |
| Invocación | `/msc spawn armorstand` |

### Fases (las transiciones ocurren según el umbral de HP)

| Fase | % HP | Efecto de transición |
|---|---|---|
| 0 — Roja | >80% | Ira: knockback + Debilidad I a los jugadores cercanos, sello de pentagrama grande |
| 1 — Púrpura | >60% | Barrera: invulnerable 100t, cura +30 HP, sello celestial |
| 2 — Amarilla | >40% | Tormenta: 15 rayos en un radio de 12 bloques, Lentitud II + Debilidad II |
| 3 — Verde | >20% | Desesperación: invulnerable 80t, daño AoE ×1.5 + Oscuridad II + Ceguera I + Lentitud III |
| 4 — Azul | ≤20% | fase final |

La escalera es datos, no código: `armor-stand-boss.phase-thresholds` contiene las fracciones de salud en las que empieza cada fase siguiente, de mayor a menor. El número de fases es uno más que las entradas de esa lista, y es también cuántos cuadrados muestra el título de la barra de jefe — uno por fase, rojo para las que quedan y gris para las ya gastadas. Las entradas fuera de `(0, 1]` se descartan y los duplicados se colapsan, así que una errata no puede dejar al jefe con una sola fase.

### Comportamiento de la IA

- **Un solo reloj de ataque** — todos los ataques comparten la misma pausa: `attack-interval-ticks` (50, 2.5 s) después de que termine el anterior, el tick de ataque sortea el *tipo* de ataque con `attack-type-weights` (cuerpo a cuerpo 30, a distancia 20, vuelo 10, bombardeo flotante 8, sello de escudo 8, Juicio de la Égida 8, defensa 12) y luego un ataque de ese tipo. Un ataque no puede volver hasta que se hayan lanzado otros tres, sean del tipo que sean.
- **Modo vuelo** sortea entre ataques aéreos (`aerial`, 80) y a distancia (`ranged`) con el mismo reloj; tras cinco aéreos distintos (y 10–20 s en el aire) aterriza o cae en picado con AirSlam, y nunca pasa más de 40 s arriba.
- **Tras el sello de escudo** sortea entre ataques a distancia, el Juicio de la Égida y la Llamada del Triángulo (`summon`, 25).
- **Un ataque a la vez** — mientras un ataque se anima la IA ni lo gira ni empieza el siguiente; los tiempos de espera solo cuentan cuando está libre, así que un ataque largo nunca se come la pausa que le sigue.
- **Defensas** (por debajo del 90% de HP, en el suelo, un estado a la vez): **Piel de Piedra** (×0.5 daño recibido), **Barrera Reflectante** (×0.7 daño + 30% reflejado), **Escudo Absorbente** (absorbedor de 100 HP que visualmente cambia de azul a rojo), **Baluarte** (×0.35 daño, pero queda inmóvil tras sus murallas), **Aura de Espinas** (×0.8 daño, cada golpe pincha al atacante por 3 y quien esté a menos de 6 bloques recibe un pinchazo cada segundo) y **Imagen Residual** (el 35% de los golpes fallan). Sus duraciones salen de `defense-duration-*-ticks`: piel de piedra 200, barrera reflectante 160, escudo absorbente 300, baluarte 120, aura de espinas 200, imagen residual 160.
- **Defensas de curación** (por debajo del 70% de HP, una a la vez): **Círculo de Curación**, **Regeneración** (cura `regeneration-heal-percent` (5%) en 8 s mientras sigue peleando), **Sifón de Almas** (ata a hasta tres jugadores a menos de 16 bloques y los drena, curándose cuatro veces lo que quita; alejarse 20 bloques rompe la atadura) y **Capullo de Obsidiana** (invulnerable 3 s dentro de pilares de obsidiana mientras cura `obsidian-cocoon-heal-percent` (4%), y luego una onda expansiva).
- **Recuperación de suelo** — un jefe en modo suelo solo ataca mientras `isOnGround` es cierto. Si se queda sin bloque sólido debajo (vacío, agua, un agujero, un borde), flotaba en silencio para siempre. Tras `ground-recovery-grace-ticks` (40) ticks sin suelo se teletransporta a la columna más cercana con piso y espacio libre, prefiriendo la zona de su objetivo actual y recurriendo al spawn del mundo si no encuentra nada, y reanuda el ataque con los cooldowns reiniciados.
- **Despawn** — sin nadie en un radio de 100 bloques (`boss-balance.despawn.large-radius`) durante `delay-ticks` (100, 5 s) despawnea y limpia sus tareas, sellos, música y barra de jefe. Con `delay-ticks: 0` se va en cuanto la arena se vacía.
- **Daño penetrante** — con `penetrating-damage: true` los golpes del jefe ignoran armadura y encantamientos de Protección (se reaplican como daño `OUT_OF_WORLD`, con cap de `max-damage-dealt` (15) por golpe). La Resistencia solo se perfora *en parte*: `penetrating-resistance-pierce: 0.2` hace que el jefe ignore el 20% de la mitigación de la poción, así que un jugador con Resistencia I (20% de reducción) sigue bloqueando el 16% del golpe. `0.0` deja la Resistencia totalmente efectiva y `1.0` la ignora por completo. Usa `/msc debug [jugador]` tras un golpe para ver el desglose completo (daño del evento, las reducciones devueltas, la perforación aplicada y el valor final); el mismo comando también informa de lo que Nix y Jack Star hacen y reciben de ese jugador.

### Mecánicas especiales

- **Juicio de la Égida** (`groundslam`) — un tipo más del tick de ataque: lanza el escudo al cielo, donde gira y proyecta un pentagrama ardiente bajo cada jugador durante dos segundos; cuando baja la lanza, columnas de luz caen del escudo sobre cada pentagrama — `seal-damage` (15) en un radio de 4 bloques + empuje hacia arriba — y el escudo vuelve a su mano.
- **Alas** — alas ardientes construidas como unas de verdad: un hueso oscuro que sale de la espalda entre los omóplatos y sube por un codo y una muñeca, plumas largas colgando de él que brillan de rojo a naranja hacia las puntas, y una fila más corta de coberteras. Aletean despacio, girando toda el ala desde su raíz.
- **Sello de Escudo** — seis grandes escudos de luz giran a su alrededor a la altura del pecho durante 200 ticks, ×0.7 daño entrante; al final se pliegan de nuevo en uno.
- **Círculo de Curación** — se arrodilla en un círculo de runas verdes durante 200 ticks mientras hilos de luz suben hacia él, curando `healing-circle-heal-percent` (5%) de su HP máxima repartido por igual durante todo el arrodillamiento. Golpearlo dentro del círculo es la respuesta.
- **Bombardeo Flotante ("CrossBarrage")** — sube si está en el suelo y dispara ráfagas de rayos en forma de X desde el aire, `hover-barrage-damage` (12) + knockback.
- **Llamada del Triángulo** — clava la lanza y alza dos sellos de fuego de pie; columnas de luz bajan a través de ellos y salen los refuerzos (escala con el número de jugadores):
  - Modo aéreo: Ghast Infernal + Fantasma Acechador Nocturno (que lleva un Esqueleto Francotirador con arco Power V / Infinity).
  - Modo suelo: Bestia de Guerra Ravager (300 HP, 24 de daño) que lleva un Evocador Sacerdote Oscuro (40 HP, Velocidad I).
  - Las invocaciones llevan la etiqueta `MSC_ArmorBossSummoned`; el fuego amigo entre el jefe y sus invocaciones está desactivado.

### Pasivas por fase y el cambio de fase

Cada cambio de fase es una escena: el Centinela cae sobre una rodilla, el poder sube en espiral desde el suelo, se alza rugiendo dentro de un pilar de luz (intocable hasta el clímax), estalla la explosión propia de la fase y todos los jugadores a menos de 60 bloques ven la pasiva que acaba de ganar. Las pasivas se acumulan:

| Fase | Pasiva | Efecto |
|---|---|---|
| 2 (≤80%) | **Furia** | ataca un 15% antes |
| 3 (≤60%) | **Piel de Obsidiana** | recibe un 15% menos de daño |
| 4 (≤40%) | **Tempestad** | otro 15% más rápido; un rayo avisado sobre un jugador cada 10 s |
| 5 (≤20%) | **Voluntad Inmortal** | regenera un 0.25% de su vida por segundo, hace un 20% más de daño y sus ataques destructivos salen el doble |

### Ritos de invocación (10)

Se sortean como tipo propio (`attack-type-weights.summoning`, 10). Las invocaciones llevan la etiqueta de invocación, nunca dañan al jefe ni entre ellas, y se disuelven al acabar su tiempo o al caer el Centinela; no lanza otro rito mientras haya `max-summons` (8) vivas.

- **Criaturas nuevas:** `lancesquires` (dos escuderos de obsidiana que embisten con su lanza), `obsidianmender` (cura al jefe un 0.4% por segundo hasta que lo maten), `emberhounds` (tres lobos en llamas que te prenden fuego), `voidwisps` (tres fuegos fatuos que atraviesan paredes y estallan), `obsidianbrute` (un bruto del doble de tamaño que golpea el suelo cada cuatro segundos).
- **Criaturas del plugin:** `elementalconclave` (Elemental de Fuego, Gólem de Escarcha, Invocador de Tormentas), `shadowambush` (Pícaros Sombríos y Reptadores del Vacío alrededor del objetivo), `necropolisrite` (Segador de Almas y dos Escudos de Hueso), `arcanecovenant` (Mago del Caos, Bruja Venenosa, Caballero del End).
- **`championcall`** — una vez por fase se abre un portal y entra un jefe del plugin al azar (NIX, DIO, Garou, Mahoraga o Kinger). Pelea hasta caer; si el Centinela cae antes, el portal se lo lleva. JackStar queda fuera: al aparecer invoca a otro jefe.

### Ataques destructivos (10)

Se sortean como tipo propio (`attack-type-weights.destructive`, 6), con al menos `destructive-gap-ticks` (400, 20 s) entre uno y otro. Cada uno tiene una carga larga y ruidosa — una cuadrícula de objetivo en el suelo y un aviso en la pantalla de todos los que estén cerca — y un golpe de alcance enorme. El mundo nunca se rompe; los cráteres son de escombros.

`orbitalstrike` (la cuadrícula fija el objetivo, rayos convergen desde la órbita, cúpula roja), `meteorimpact` (un meteoro del tamaño de una casa cayendo durante cinco segundos), `supernova` (solo el ojo de la tormenta a sus pies es seguro), `judgmentpillars` (columnas de luz por la arena y bajo cada jugador), `earthsplitter` (una cruz de grietas de treinta bloques), `voidcollapse` (un agujero negro arrastra a todos y colapsa), `obsidiantsunami` (un muro de obsidiana barre la arena; busca el hueco), `solarlance` (una lanza de luz solar de catorce bloques), `worldbreaker` (salta treinta bloques y aterriza con tres ondas expansivas), `apocalypserain` (lluvia de meteoros durante cinco segundos).

### Ataques animados

Cada ataque es una **coreografía**: un aviso en el suelo que dice *dónde* (de rojo a amarillo según se calienta), una preparación del cuerpo que dice *cuándo*, el golpe y una recuperación de vuelta a la guardia. Brazos, piernas, cabeza y torso del Centinela pasan por poses reales calculadas a partir del modelo del armor stand, así que la punta de la lanza, la cara del escudo y las manos son de donde salen los efectos. Mientras un ataque se reproduce es dueño del cuerpo: la IA no lo gira ni empieza otro ataque hasta que termina. Los objetos (escudos, lanzas, pilares de obsidiana, meteoros) son display entities con la etiqueta `MSC_AttackProp` y se eliminan al terminar el ataque o al reiniciar el servidor.

### Registro de ataques — 96 ataques en total

Todos los ataques son clases que extienden `ChoreographedAttack` bajo `entities/boss/attack/<aerial|ground|ranged|defensive|summon|destructive>/`, registrados en `ArmorStandBoss.initAttacks()` y despachados polimórficamente vía `attackRegistry.get(name).execute(instance)`. Activa cualquiera manualmente:

```
/msc attack <nombre-del-ataque> [rango]
```

Los 96 nombres los lista `/msc attack help` (seis páginas, una por categoría) y los ofrece el autocompletado; una séptima página lista las mecánicas (`flyup`, `land`, `reset`, las cuatro transiciones `phase*`). Cada uno tiene un solo nombre.

| Suelo (24) | Aéreos (20) | A distancia (20) | Defensivos (12) |
|---|---|---|---|
| groundslam | starfall | lancesnipe | stoneskin |
| groundshatter | aerialrush | meteorstorm | reflectbarrier |
| shieldbash | sonicboom | voidbeam | absorbshield |
| lancestorm | lightningstorm | frostlance | shieldseal |
| earthpillar | gravitywell | lightningspear | healingcircle |
| chaingrapple | crossslash | shadowvolley | trianglecall |
| warstomp | novaburst | chainlightning |  |
| armorspikes | darkorb | crystalbarrage |  |
| vortexpull | windcutter | arcaneorb |  |
| mirrorimage | heavenlyjudgment | voidrift |  |
| doombeam | rainoflances | arcanemissiles |  |
| lanceflurry | airslam | spiritbeam |  |
| whirlwindslash | hoverbarrage (crossbarrage) | soultethers |  |
| executionsweep | eclipsefall | plaguebrand | regeneration |
| obsidianspire | bladering | runemines | soulsiphon |
| earthmaw | obsidianwings |  | obsidiancocoon |
| shadowstep | voidmeteor | obsidianprison | bulwark |
| runeward | phantomlegion |  | thornaura |
| sunderingcharge |  |  | afterimage |
| spearcyclone | spiralstorm | shardburst |  |
| cataclysm | chainhook | gravityorb |  |
| tremorlance |  | javelinvolley |  |
| aegisrush |  | sweepinglaser |  |
| gravecleaver |  |  |  |

Objetivos adicionales de `/msc attack` para **mecánicas y transiciones de fase**: `flyup`, `land`, `reset`, `phaserage`, `phasebarrier`, `phasestorm`, `phasedespair`.

### Segunda tanda — diez ataques más y cómo reconocerlos

Cada uno de los diez añadidos está animado alrededor de una seña inequívoca, para que el jugador pueda nombrar el ataque desde el primer segundo del preaviso. Los overrides de daño viven bajo `entities.armor-stand-boss.*-damage`.

| Ataque | Categoría | Seña de la animación | Clave de daño (por defecto) |
|---|---|---|---|
| `obsidianspire` | Suelo | Los dos brazos suben rectos sobre la cabeza y el cuerpo se echa atrás, y luego una línea de pilares volcánicos erupciona uno a uno hacia delante. Cada pilar lanza a su víctima **hacia arriba**, y la Cruz del Verdugo se dibuja a lo largo del recorrido. | `obsidian-spire-damage` (11) |
| `earthmaw` | Suelo | Se agacha mientras dos filas de dientes se abren sobre un cono frontal de 34°, el suelo se agrieta con un Sello de Seísmo y entonces las filas **se cierran de golpe** y arrastran al centro a todo el que pillan dentro. | `earth-maw-damage` (9) |
| `shadowstep` | Suelo | Se encorva con los brazos cruzados, un pentagrama se lo traga y **desaparece** — el único ataque que se teletransporta. Reaparece 1,35 bloques por detrás del objetivo con un segundo sello y gira 360° para apuñalarlo. | `shadow-step-damage` (15) |
| `runeward` | Suelo | Un **lanzamiento de rodillas** dentro de un pentagrama grande, y después planta una runa de amatista que se queda atrás. Cada 25 ticks la runa late y empuja un anillo de runas (Debilidad + Lentitud) durante 150 ticks antes de romperse. | `rune-ward-damage` (4) |
| `eclipsefall` | Aéreos | Los dos brazos suben rectos mientras un **disco oscuro** se forma tres bloques por encima, rodeado por un pentagrama. El disco cae con estela de cometa y detona como una onda negra que ciega a los que están cerca. | `eclipse-fall-damage` (16) |
| `bladering` | Aéreos | Ocho lanzas de netherita aparecen en un **anillo visible orbitando su cuerpo** bajo un sello de alas. Gira con el anillo, lo ensancha y lanza las lanzas una a una con vuelo teledirigido. | `blade-ring-damage` (7) |
| `obsidianwings` | Aéreos | Cuatro paneles de alas le crecen en los hombros y se despliegan. Tres **aleteos** bajan los paneles y lanzan anillos de esquirlas de obsidiana hacia fuera; después las alas se pliegan y ambas golpean a la vez en una onda expansiva. | `obsidian-wings-damage` (8), `obsidian-wings-slam-damage` (15) |
| `soultethers` | A distancia | Los brazos suben sobre la cabeza y las brasas de alma de todos los jugadores cercanos drenan hacia su pecho. Una **atadura luminosa queda dibujada** de su pecho hasta cuatro jugadores mientras los arrastra y los drena, y al final tira con fuerza y los reúne de golpe. | `soul-tether-damage` (3), `soul-tether-snap-damage` (13) |
| `plaguebrand` | A distancia | Abre los brazos con las palmas hacia arriba y un círculo verde enfermizo se cierra sobre la víctima elegida. La marca cae como un estallido de esporas, drena a su huésped cada segundo y **salta a quien esté a su lado**; su cabeza sigue al jugador marcado tras el impacto. | `plague-brand-hit-damage` (9), `plague-brand-damage` (4) |
| `runemines` | A distancia | Un barrido bajo del brazo por el suelo bajo un triángulo rúnico, y luego seis runas salen **lanzadas alrededor del objetivo**. Tardan 25 ticks en armarse (apagadas, planas), se vuelven violetas y laten, y estallan hacia arriba cuando alguien las pisa — o se apagan solas a los 140 ticks. | `rune-mine-damage` (8) |

Las rotaciones aleatorias también los usan: `obsidianspire`, `earthmaw`, `shadowstep` y `runeward` en las tablas de suelo, `eclipsefall`, `bladering` y `obsidianwings` en las aéreas, y `soultethers`, `plaguebrand` y `runemines` en la de distancia. Los tres a distancia disparan tanto en suelo como en vuelo; los otros siete exigen el estado correspondiente.

### Tercera tanda — seis ataques más

| Ataque | Categoría | Seña de la animación | Clave de daño (por defecto) |
|---|---|---|---|
| `sunderingcharge` | Suelo | Baja la punta de la lanza al suelo, carga el peso atrás y el carril brilla por delante; luego un sprint que **abre una grieta fundida** tras la punta. Al final un tajo ascendente lanza un abanico de hojas de obsidiana desde el suelo y, un instante después, toda la grieta estalla en fuego. | `sundering-charge-damage` (14) |
| `spearcyclone` | Suelo | Hace girar la lanza sobre la cabeza cada vez más rápido dentro de una espiral de viento, y la baja de golpe: un **ciclón** rueda tras el objetivo, arrastrando y levantando a quien esté cerca, y revienta al final de su recorrido. | `spear-cyclone-damage` (4 por golpe, ×3 en el estallido) |
| `cataclysm` | Suelo | Definitivo: clava la lanza en la tierra y tres franjas del suelo brillan, con **anillos seguros** de suelo oscuro entre ellas. De dentro hacia fuera cada franja revienta en muros de fuego y púas de obsidiana. Quédate en los huecos. | `cataclysm-damage` (20) |
| `voidmeteor` | Aéreo | Con ambos brazos en alto se forma sobre su cabeza una roca de obsidiana llorosa envuelta en vacío; la lanza contra el objetivo, cae con una onda expansiva y deja un **cráter de vacío** que ciega y debilita. | `void-meteor-damage` (18) |
| `phantomlegion` | Aéreo | Cuatro **copias espectrales** suyas salen del suelo alrededor del objetivo. Una tras otra marcan su línea y embisten atravesando el anillo; las dos últimas golpean a la vez. | `phantom-legion-damage` (11) |
| `obsidianprison` | A distancia | Apunta la lanza a cada jugador: se abre un círculo de runas bajo él, luego una **jaula de púas de obsidiana** brota a su alrededor, inclinada hacia dentro, y se derrumba sobre sí misma. Sal del círculo antes de que se cierren los barrotes. | `obsidian-prison-damage` (16) |

También entran en las rotaciones aleatorias: `sunderingcharge` en las tablas de suelo media y lejana, `spearcyclone` y `cataclysm` en la cercana y la media, `voidmeteor` y `phantomlegion` en las aéreas y `obsidianprison` en la de distancia.

### Drops

1000 XP al morir, más el broadcast del título "THE OBSIDIAN SENTINEL / Has been defeated!". Rayo + sonido de muerte de wither al morir.

---

## ⚙️ Mahoraga — Minijefe (Jujutsu Kaisen)

"Divino General de la Espada de Ocho Manos Divergente Sila" — la adaptación hecha realidad.

| Estadística | Valor por defecto |
|---|---|
| Salud | 250 |
| Invocación | `/msc spawn mahoraga` |
| Spawn natural | `mahoraga.spawn-chance` (2%) — reemplaza Zombies |

**Lógica de adaptación (escaneo por tick de la armadura y armas del objetivo):**

| Atributo del objetivo | Mahoraga gana |
|---|---|
| Armadura de Diamante/Netherita + niveles de Protección | bonificación de **Daño de Ataque** escalada |
| Armadura de Slimefun / Tinker (`mahoraga.slimefun-adaptation`, dependencia suave) | bonificación de **Daño de Ataque** por nivel del material (0.3 blando → 3.0 Singularidades/Infinity) |
| Set completo de Mail Links de Infinity Singularity (`mahoraga.instakill-infinity-armor`) | **Muerte instantánea** — atraviesa el trait "Infinite Defence" (daño = 1) |
| Mejora de **Diamante** de Tinker en el arma empuñada (`mahoraga.ignore-diamond-mod`) | **Siempre ignorada** — el reflejo y la cancelación de cada golpe nunca aplican |
| Espada Tinker con material **Infinity Singularity** (`mahoraga.infinity-weapon-adaptation`) | Mahoraga solo recibe **1 de daño** por golpe |
| Protección total > 5 (Diamante/Netherita) | amplificador de **Fuerza** = total/5 |
| Nitidez **o Castigo** totales máximos en cualquier arma (5 niveles por rango) | nivel de **Resistencia** (máx. 4) = floor((Nitidez + Castigo)/5) |
| Encantamientos de Knockback | **Resistencia al Knockback** = totalKnockback × 0.3 |
| Objetivo a > 4 bloques | **Velocidad I** durante 30 ticks |
| Objetivo cercano | Se elimina la Velocidad |

Atuendo: casco de vidrio blanco + armadura de cuero blanca (irrompible).

**Protección de fuego amigo MSC:** si tanto el atacante como el objetivo llevan cualquier etiqueta de scoreboard `MSC_`, el evento de daño se cancela — Mahoraga no puede dañar a otros mobs MSC (y viceversa).

**Drops:** 75% de probabilidad de **Esencia de Rueda**. 150 XP. Mensajes de muerte de jugador personalizados vía `mahoraga.death-messages`.

---

## ♟️ Kinger — Minijefe (The Amazing Digital Circus)

Un rey de ajedrez viviente: un ArmorStand invisible vestido con un traje de 15 piezas ItemDisplay — una cabeza de madera (cuello, cara, corona y cruz) con dos ojos, una túnica morada en dos piezas, dos brazos con manga y dos piernas que se doblan en la rodilla. El esqueleto es jerárquico: las piernas oscilan desde la cadera, el torso se inclina sobre la línea de la cadera y arrastra la cabeza y los brazos, y la cabeza gira sobre el cuello con los ojos y la corona como una sola pieza. El stand no está escalado — un hitbox normal de 0.5 × 1.975 bloques que cubre el cuerpo; solo las manos asoman unos centímetros por los lados.

| Estadística | Valor por defecto |
|---|---|
| Salud | `kinger.health` (120) |
| Hitbox | `kinger.hitbox-scale` (1.0) — tamaño del stand invisible por el que se golpea el traje; 1.0 es un stand normal y ya cubre todo el modelo (acotado a 0.25–8) |
| Rango de agresión | `kinger.aggro-range` (25 bloques) |
| Velocidad de movimiento | `kinger.move-speed` (0.32) |
| Rango / radio / daño cuerpo a cuerpo | `kinger.melee-range` (3) · `kinger.melee-radius` (3.5) · `kinger.melee-damage` (8) |
| Rango / daño a distancia | `kinger.ranged-range` (30) · `kinger.ranged-damage` (6) |
| Cooldowns | cuerpo a cuerpo 25 ticks · a distancia 45 ticks |
| Invocación | `/msc spawn kinger` — **o** coloca un ArmorStand |
| Reemplazo de ArmorStand | `kinger.spawn-on-armorstand-chance` (0.01 = 1% de los ArmorStands colocados se convierten en Kinger; pon 0 para desactivar) — respeta `kinger.enabled` |

### Comportamiento de la IA

- **Persigue** al jugador más cercano dentro del rango de agresión (camina a `move-speed`, se ancla al suelo) y **mira** al objetivo mientras sigue su inclinación de cabeza.
- **Cuerpo a cuerpo** (≤3 bloques): ráfaga de partículas púrpuras + humo, `melee-damage` a todos los jugadores dentro del radio cuerpo a cuerpo, con knockback de velocidad 1.3.
- **A distancia** (>3 y ≤30 bloques): levanta el brazo derecho hasta apuntar al objetivo y dispara una **ShulkerBullet** desde esa mano (`MSC_KingerBullet`) — daño y una Oscuridad breve, sin levitación.
- **Animaciones**: caminata con los brazos en contra de las piernas y rodillas que se doblan; melee *Decreto Real* (ambos brazos suben y luego barren hacia abajo y al frente mientras embiste — el golpe cae en ese momento); disparo *Orden Real* (brazo levantado apuntando mientras dispara). La cabeza sigue al objetivo desde la altura de los ojos: arriba si el jugador está más alto, abajo si está más bajo.

**Barra de jefe:** barra púrpura "Kinger", siempre actualizada con la salud actual.

**Persistencia:** etiquetado `MSC_Kinger`, por lo que sobrevive a recargas del plugin y se retoma al iniciar — al recargar se vuelven a enganchar las piezas que ya había creado, en vez de construir un segundo cuerpo superpuesto, y se reconstruye su barra de jefe.

**Muerte:** elimina todos los displays del traje y transmite uno de los `kinger.death-messages` temáticos de ajedrez ("checked by the King", "knocked off the board", "lost the game"...).

---

## 🪓 NIX - El Verdugo

Un verdugo colosal e implacable construido a partir de un **modelo personalizado de 27 piezas ItemDisplay** utilizando cabezas de jugador con texturas y transformaciones matriciales. NIX posee IA avanzada, movimiento fluido de extremidades mediante animaciones procedurales con cuaterniones JOML y brutales mecánicas de ejecución.

| Estadística | Valor por defecto |
|---|---|
| Salud | `nix-executioner.health` (2000.0) |
| Hitbox | `nix-executioner.hitbox-scale` (1.9) — tamaño del stand invisible por el que se golpea el traje (acotado a 0.25–8) |
| Rango de agresión | `nix-executioner.aggro-range` (28.0 bloques) |
| Velocidad de movimiento | `nix-executioner.move-speed` (0.30) |
| Rango cuerpo a cuerpo / Daño de tajo | `nix-executioner.melee-range` (3.5) · `nix-executioner.cleave-damage` (22.0) |
| Rango de atracción con cadenas | `nix-executioner.chain-range` (24.0 bloques) |
| Cap de daño recibido | `nix-executioner.max-damage-per-hit` (100.0 por golpe; `0` desactiva el cap) |
| Cooldowns | cuerpo a cuerpo 20 ticks · cadenas 80 ticks |
| Ritual de Invocación | **El Cadalso del Verdugo** en la Boss Dimension (sacrificando `Sentencia de Muerte`) · `/msc spawn nix` (OP) |

### Daño verdadero y ataques destructivos

Todos los golpes de NIX son **daño verdadero**, el mismo que hace el Centinela de Obsidiana: ignora armadura y Protección, cada golpe tiene un tope de `max-damage-dealt` (15) y la Resistencia solo conserva parte de su efecto (`true-damage-pierce`, 0.2). Además de sus especiales tiene tres **ataques destructivos**, como mucho uno cada `destructive-cooldown-ticks` (600): **Gran Guillotina** (una guillotina de dieciséis bloques sobre el objetivo; sal de la línea), **Luna de Sangre** (sube una luna roja y luego tres olas de sangre que se saltan) y **Día de Ejecución** (ocho hachas gigantes barren hacia el centro por sus radios, dos veces; quédate entre ellas).

### Habilidades y Mecánicas

- **Tajo de Guillotina (Cuerpo a cuerpo en área):**
  Al estar a distancia de golpe, Nix levanta ambos brazos y descarga un tajo descendente aplastante. Inflige `cleave-damage` (22) en un radio frontal de 3.2 bloques, empuja a los jugadores y les aplica **Wither II (Sangrado)** y **Lentitud II**.
- **Cadenas del Juicio (Atracción a distancia):**
  Cuando un objetivo intenta huir (a entre 5 y 24 bloques de distancia), Nix lanza cadenas de hierro espectrales (`Sound.BLOCK_CHAIN_PLACE`) que aprisionan a la víctima, atrayéndola con violencia hacia él e infligiéndole **Oscuridad** y **Lentitud III**.
- **Cosecha de Sangre (Firma):**
  Abre los brazos a los lados mientras la sangre se acumula en sus manos y un anillo de aviso se cierra en el suelo, y luego gira tres vueltas completas con hojas de sangre saliendo de sus manos. Cada vuelta corta a todos en 4.5 bloques por `harvest-damage` (9) con Wither y los arrastra hacia dentro; termina en un anillo de sangre.
- **Salto del Patíbulo (Firma):**
  Una sentadilla profunda con los brazos atrás mientras el punto de aterrizaje brilla bajo el objetivo, y un salto de hasta 18 bloques con los dos brazos sobre la cabeza. Cae con un golpe a dos manos: `gallows-damage` (18) en 4 bloques más un empuje hacia arriba, y una onda expansiva que golpea a la mitad al salir.
- **Condena (Firma):**
  Alza el brazo derecho y señala a sus víctimas: un patíbulo de partículas se alza sobre cada jugador en su rango (hasta cuatro), con la hoja temblando arriba y un círculo rojo en el suelo. 1.5 segundos después el brazo cae y todas las hojas bajan — `condemn-damage` (20), Wither II y Lentitud para quien siga dentro del círculo.
  Un movimiento de firma cada `special-cooldown-ticks` (160) + hasta 2 s, elegido según la distancia; mientras se reproduce es dueño del cuerpo.
- **El Hacha del Verdugo:**
  Nix lleva un hacha de netherita en la mano derecha que sigue su antebrazo y su codo. El ataque básico es un tajo de guillotina: el hacha sube sobre su cabeza y baja delante de él, y el golpe cae cuando baja el hacha (al 60% del movimiento). Su cabeza sigue los ojos del jugador desde los suyos en vez de mirar al suelo.
- **Frenesí de Ejecución (Pasiva):**
  Cuando la salud del jugador objetivo cae por debajo del **25%**, Nix entra en frenesí de ejecución: su velocidad de movimiento aumenta un +30%, sus ojos emiten partículas de polvo carmesí y el compás de sus zancadas se acelera.
- **Animaciones Procedurales del Modelo:**
  Las 27 piezas (Cabeza, Torso Superior, Pelvis, Brazo Derecho de 6 piezas, Brazo Izquierdo de 6 piezas, Pierna Derecha de 6 piezas, Pierna Izquierda de 6 piezas) cuentan con contra-rotaciones de marcha sincronizadas, preparación de ataques y seguimiento del cabeceo de la mirada del jugador. Siguen a un **ArmorStand invisible** (`MSC_NixBoss`) que carga la vida real y la hitbox, y las articulaciones viven en `NixModel` (hombros en x = ±0.3514, caderas en ∓0.1171, cuello en 1.650, torso en 1.171), así que cada extremidad gira sobre su propia articulación. Cada brazo y cada pierna es una pila de dos segmentos, así que los **codos y las rodillas también se pliegan**: la pieza `_4` lleva la articulación y las cinco piezas de debajo articulan sobre ella al caminar y durante el tajo de cleave (los codos se flexionan en la preparación y se extienden en el golpe, mientras las rodillas flexan en una ligera sentadilla). El export queda 0.066 bloques fuera de la columna, así que las piezas se recentran sobre la hitbox, y el stand se escala 1.9 para que su caja (0.95 de ancho, 3.75 de alto) cubra todo el modelo en vez de dejar la cabeza fuera de una caja vanilla.

**Barra de jefe:** Barra segmentada de color rojo oscuro que muestra `NIX - El Verdugo` con niebla y cielo oscurecido.

**Persistencia:** Etiquetado `MSC_NixBoss` y `MSC_NixPart`, y cada pieza lleva además su propia etiqueta más una de propietario del stand al que pertenece — una recarga **adopta** las piezas que el jefe vivo ya tiene en vez de crear un segundo cuerpo encima, y las huérfanas se limpian.

**Muerte:** Desencadena truenos, sonido de muerte de wither, una explosión de partículas carmesí, suelta 450 XP y muestra un título de condena finalizada a los jugadores cercanos.

---

## 👨‍💻 JACK STAR — El Arquitecto del Sistema

Cinco fases, tres vidas y un cuerpo construido con once cabezas de skin.

### Estadísticas del jefe

| Campo | Valor |
|---|---|
| Vida | `jackstar-architect.health` (1000.0) |
| Hitbox | `jackstar-architect.hitbox-scale` (1.2) — tamaño del stand invisible por el que se golpea el traje (acotado a 0.25–8) |
| Vidas | 3 — las dos primeras "muertes" ejecutan un reinicio **Watchdog** que restaura el 50% de la vida, y la última el 40% |
| Fases | 1 >80% · 2 >60% · 3 >40% · 4 >20% · 5 (kernel panic) ≤20% |
| Daño | `melee-damage` (16) · `slam-damage` (20) · `sigkill-damage` (35) |
| Defensas | esquive Ultra Instinct `dodge-chance` (0.22) · `packet-loss-chance` (0.25) descarta proyectiles · muros cortafuegos, firejail y telarañas |
| Agro / velocidad / alcance | `aggro-range` (32.0) · `move-speed` (0.32) · `melee-range` (3.8) |
| Ritual de invocación | Ritual **The System Architect** en la Dimensión del Jefe · `/msc spawn jack` (OP) |

### Modelo

Once cabezas de skin (`ItemDisplay`, etiqueta `msc_jackstar_part`) forman la cabeza, el torso y dos segmentos por brazo y pierna. Siguen a un **ArmorStand invisible** (`msc_jackstar_boss`) que carga la vida real y la hitbox, así que el cuerpo visible es lo que apuntan los jugadores mientras el stand lleva la contabilidad. Las articulaciones viven en `JackModel` (hombros en x = ±0.35, caderas en ∓0.12, cuello en 1.87) y cada extremidad gira sobre su propia articulación, con contra-rotaciones al caminar. Los dos brazos y las dos piernas se exportaron en dos segmentos, así que los **codos y las rodillas se pliegan además de ese balanceo**: el antebrazo y la espinilla articulan sobre su propia articulación al caminar y durante tajos cuerpo a cuerpo (los codos articulan en el arco de tajo y las rodillas flexan en pose de combate). Las piezas se recentran sobre la hitbox, que es lo que hace que el cuerpo coincida con el stand en vez de desplazarse casi un bloque hacia un lado.

### Movimientos de firma

Uno cada `special-cooldown-ticks` (200) + hasta 2 s, un 10% antes en la fase 5, elegido según la distancia y anunciado en el chat de la arena como una línea de código. Mientras se reproduce es dueño del cuerpo y la rutina normal espera.

| Movimiento | Seña de la animación | Clave de daño (por defecto) |
|---|---|---|
| **fork()** | Echa atrás el brazo derecho con un cubo de alambre girando en la mano, la izquierda apuntando, y lo lanza. Cada vez que el cubo cae revienta y **se bifurca en dos** que saltan a los lados, tres generaciones: 1 + 2 + 4 explosiones, cada una con su anillo de caída visible antes. | `fork-bomb-damage` (10; ×0.6 en las bifurcaciones) |
| **Lluvia Binaria** | Las dos manos en alto, tecleando hacia el cielo mientras una lámina de código verde se desplaza sobre la arena. Bajo y delante de los jugadores se encienden celdas y en cada una cae un **1 o un 0**. | `binary-rain-damage` (8) |
| **Stack Overflow** | Una postura baja de carrera con las dos hojas hacia atrás, y **cuatro tajos en carrera** a través del objetivo, cada uno apilado como un marco "[ ]" que queda dibujado en el suelo. Cuando la pila se llena se desborda: cada marco detona a lo largo de su línea en orden inverso. | `stack-overflow-damage` (14; la mitad en la propia carrera) |

### Daño verdadero y ataques destructivos

Todos los golpes de Jack Star son **daño verdadero**, como los del Centinela (`max-damage-dealt` 15, `true-damage-pierce` 0.2). Solo empieza un especial cada `special-attack-gap-ticks` (112, 5.6 s). Desde la fase 2 añade tres **ataques destructivos**, como mucho uno cada `destructive-cooldown-ticks` (500): **Kernel Nuke** (cuenta atrás de cinco segundos, cuadrícula fijada y una cúpula de once bloques), **Disk Format** (la arena se vuelve una cuadrícula de sectores y se borran todos menos los verdes) y **sudo laser** (un rayo que gira a su alrededor; solo sus pies son seguros).

### Subprocesos

Tres segundos después de aparecer, y de nuevo en cada cambio de fase — cinco veces como máximo — Jack Star invoca a otro jefe a 14 bloques: Garou, Mahoraga, Chaos Mage, Obsidian Guard, Soul Reaper o NIX, elegido al azar hasta que uno acepte. Él se queda en el campo todo el tiempo: un subproceso es presión extra, **nunca un escudo**, así que sigue peleando y sigue recibiendo daño mientras esté vivo.

### Daño recibido

Los golpes pasan por una sola puerta: primero el esquive **Ultra Instinct** (0.22, elevado a 0.45 en forma comprimida) y después el **Load Balancer**, que deja el 65% en el jefe y reparte el 35% entre cada jugador no creativo en 14 bloques. Un golpe que acierta siempre llega al jefe; `/msc debug` imprime el golpe previsto, el reparto y el valor aplicado.

**Botín:** 950 XP y el `ArchitectKernel`, con un título final para cada jugador en 60 bloques.


---

## ⏱️ DIO — JoJo's Bizarre Adventure

DIO camina amenazante (letras ゴゴゴ suben a su alrededor) con su Stand **The World** flotando tras su hombro derecho. DIO es un armor stand visible con su ropa amarilla de la Parte 3 y su propia cara; The World es un stand más grande con su propia cabeza, armadura de oro, adornos de esmeralda y un aura dorada. La vida es virtual, como en Nix y Jack Star. Se invoca en **El Trono de The World** dentro de la Boss Dimension (ver [Ritual Dimension](Ritual-Dimension)) o con `/msc spawn dio` (OP).

| Campo | Valor |
|---|---|
| Vida | `dio-brando.health` (1500) — por debajo del 50% se enfurece ("WRYYYY!"): pausas más cortas, tiempo detenido más largo y más cuchillos |
| Detección / velocidad | `aggro-range` (32) · `move-speed` (0.26) |
| Tope de daño recibido | `max-damage-per-hit` (100) |

| Ataque | Qué pasa | Clave de daño (por defecto) |
|---|---|---|
| **ZA WARUDO** | The World se eleva con los brazos abiertos y una esfera de tiempo detenido se expande `time-stop-radius` (40) bloques. Durante `time-stop-ticks` (100) los jugadores no pueden moverse, atacar, usar objetos ni disparar; mobs y proyectiles también se congelan, y él cuenta los segundos ("1-byō keika..."). Lanza cuchillos que se quedan quietos en el aire rodeando a cada jugador. "Toki wa ugokidasu": el tiempo vuelve a moverse y todos los cuchillos salen disparados. Espera `time-stop-cooldown-ticks` (700). | `knife-damage` (5) por cuchillo |
| **MUDA MUDA MUDA** | The World avanza hasta 10 bloques y entierra al objetivo bajo una ráfaga de puñetazos, con un último "MUDAAA!" a dos puños que lo lanza lejos. | `barrage-damage` (2.5 cada 3 ticks) · `barrage-finisher-damage` (14) |
| **ROAD ROLLER DA!** | Salta muy alto sobre el objetivo, aparece una apisonadora y cae con él encima; The World la golpea contra el suelo ("MUDA MUDA") y explota. El círculo de caída se ve todo el tiempo. | `road-roller-damage` (26; la mitad en la explosión) |
| **Abanico de cuchillos** | Cuchillos detrás de la cabeza lanzados en abanico de 7 (9 enfurecido). | `knife-damage` (5) |
| **Space Ripper Stingy Eyes** | Sus ojos brillan en rojo y dos chorros de líquido a presión barren la arena. | `eye-beam-damage` (5 cada 3 ticks) |
| **Puñetazo de The World** | Su ataque básico a corta distancia: un puñetazo fuerte con retroceso. | `punch-damage` (12) |

Saluda a quien se le acerca ("¿Oh? ¿Te estás acercando a mí?"). Sus cuchillos, la apisonadora y las letras amenazantes son objetos de display que se borran al terminar el ataque, al morir el jefe o al reiniciar; `/msc kill` elimina a DIO y a The World.
### Daño verdadero y La Hora Final

Todos los golpes de DIO y The World son **daño verdadero**, como los del Centinela (`max-damage-dealt` 15, `true-damage-pierce` 0.2). Su ataque destructivo, **La Hora Final**, llega como mucho una vez cada `final-hour-cooldown-ticks` (900): bajo él se extiende una esfera de reloj dorada de dieciséis bloques, la aguja gira y se detiene en una hora que brilla en verde, y entonces "ZA WARUDO" — The World golpea una a una todas las demás horas. Corre a la hora iluminada.

---

## 🌪️ WITHER STORM — Cracker's Wither Storm Mod

La Tormenta Wither del mod de nonamecrackers2, dibujada solo con *display entities*: cada caja del modelo del mod es un `BlockDisplay` (obsidiana, hormigón negro, obsidiana llorosa, el bloque de comandos en las costillas...) y las tres cabezas del Wither original son `ItemDisplay` con calaveras de esqueleto wither. La geometría, las posiciones de las cabezas, la apertura de las mandíbulas y el balanceo de los tentáculos salen del propio código del modelo del mod, así que cada pieza se mueve donde el mod la pone.

### Invocación

Se construye **la estructura del Wither** (4 bloques de arena o tierra de almas en T y 3 calaveras de esqueleto wither) con el pie de la T apoyado sobre el **bloque núcleo**: `wither-storm.summon.core-block` (por defecto `CRYING_OBSIDIAN`, obsidiana llorosa). No hace falta ningún bloque de comandos. Al poner la tercera calavera, el Wither que iba a nacer es absorbido junto con el núcleo y la tormenta se forma en su lugar: un vórtice de materia oscura, un rayo y once segundos invulnerable (`forming-ticks`, 220) mientras crece y se llena la barra, y al final la explosión del nacimiento (`explosion-power`, 7, como la de un Wither). Construida sobre cualquier otro bloque, la estructura sigue dando un Wither normal.

También: `/msc spawn witherstorm` (OP), y `witherstorm2`…`witherstorm5` para empezar directamente en una forma posterior.

### Las cinco formas

Crece **comiendo**: lo que arrancan y tragan sus rayos tractores suma puntos (`consume-points`: bloque 1, mob 6, jugador mordido 10, objeto 1) y al llegar a `forms.<forma>.evolve-at` evoluciona — ruge con todas las cabezas, el cuerpo viejo se encoge, el nuevo crece y la vida máxima sube (el daño ya hecho se conserva).

| Forma | Modelo del mod | Tamaño (escala 1) | Vida | Cuerpo recibe | Rayo | Vuelo | Evoluciona en |
|---|---|---|---|---|---|---|---|
| **La Joroba** | Fase 1: el Wither con el bloque de comandos y el primer bulto | 3 × 3,5 | ×1 | 100% | 20 | 5 | 200 |
| **La Joroba Creciente** | Fase 2: la masa se tragó la cabeza central y le sale una mandíbula | 3,3 × 3,7 | ×1,5 | 90% | 26 | 6 | 600 |
| **La Joroba Hinchada** | Fase 3: los tres primeros tentáculos | 11,5 × 11,7 | ×2 | 80% | 32 | 8 | 1500 |
| **El Destructor** | Fase 4: tres cabezas sobre una masa voladora | 61 × 62 | ×3,5 | 60% | 64 | 28 | 6000 |
| **El Devorador** | Fase 5: la masa colosal y nueve tentáculos | 110 × 114 | ×5 | 50% | 96 | 40 | — |

La vida base es `wither-storm.health` (800) multiplicada por `forms.<forma>.health-multiplier`. El tamaño se ajusta por forma con `forms.<forma>.scale` (1,0 es el tamaño del mod; el Devorador mide más de cien bloques).

### Cómo se alimenta

- **Las jorobas (formas 1–3) son un agujero negro:** cada 3 s arrancan a su alrededor 3, 9 o 18 trozos de suelo (según la forma) y los atraen girando hasta su masa, como la fuente de racimos del mod. Así crecen hasta el Destructor.
- **Del Destructor en adelante comen con los rayos:** donde un rayo toca el suelo arranca bloques que vuelan hasta la boca.
- Respeta `mobGriefing` y los plugins de protección (cada bloque pasa por `EntityChangeBlockEvent`), solo arranca bloques expuestos y nunca toca bloques con inventario, roca madre, portales ni bloques de comandos. Con `grief-blocks: false` el suelo se queda donde está pero la tormenta sigue creciendo.
- Solo come mientras tiene víctimas cerca: abandonada, no devora el mundo.

### Rayo tractor

Como en el mod: **la primera forma no tiene rayo**, en las dos jorobas siguientes solo lo tiene la cabeza central, y desde el Destructor las tres.

1. El rayo solo se enciende cuando la cabeza apunta a una víctima que **ve** (sin bloques en medio).
2. Se ilumina **fino y sin tirar** durante `beam.charge-ticks` (30): es el aviso.
3. **Tira** durante `beam.hold-ticks` (120) a la velocidad del mod, `beam.pull-speed` (0,2 bloques/tick), más despacio en el último bloque.
4. **Descansa** `beam.rest-ticks` (100) y también al morder.

Mientras el rayo está encendido la cabeza gira **más despacio que un jugador corriendo**: cruza el cono corriendo de lado y te sueltas. Las jorobas solo arrastran a la víctima que fijaron; el Destructor y el Devorador arrastran todo lo que entre en el cono. El rayo se ve como un cono translúcido de cristal morado, igual que en el mod.

El cono se dibuja estable: se redibuja en los mismos fotogramas de animación que la cabeza de la que sale, así que en el cliente se mueven juntos; la cabeza apunta a un punto que sigue con suavidad los ojos de la víctima en vez de cada paso y cada salto, y gira al revés cuando gira el cuerpo para que el rayo no se desvíe; su largo no se deja llevar por el suelo que arranca debajo (los cambios de menos de un bloque se ignoran y los mayores se suavizan); y mientras sujeta a una víctima la cabeza ignora los vaivenes pequeños dentro del cono (3 grados de margen) y solo sigue, con suavidad, a una víctima que se está saliendo.

### Ataques

Ataca a **todo ser vivo** a su alrededor, jugadores primero (`attack-mobs`).

| Ataque | Qué pasa | Clave (por defecto) |
|---|---|---|
| **Mordisco** | Lo que llega a la boca: un jugador es mordido (daño verdadero + Wither II) y escupido, y esa cabeza descansa; un mob o un objeto es devorado. Las mascotas y los mobs con nombre se escupen. | `bite-damage` (14) |
| **Rugido + calavera ardiente** | Cada 20–50 s (`roar-interval-*`, como el mod) una cabeza ruge (lentitud alrededor) y escupe una **calavera ardiente** que explota con fuego. | potencia por forma (2,5 → 5) |
| **Calaveras wither** | Las jorobas disparan calaveras de Wither normales (10% cargadas). | — |
| **Tentáculos** | Las puntas de los tentáculos golpean y lanzan a quien barren. | `tentacle-damage` (10) |
| **Enfermedad wither** | Cerca de la tormenta, hambre (20 s), luego debilidad (40 s) y luego Wither y fatiga (80 s). Los mobs hostiles cercanos se vuelven **Sickened** y pelean por ella. | `sickness.*` |

### Ataques especiales

Uno cada `specials.cooldown-ticks` (500, +0–10 s), nunca el mismo dos veces seguidas, anunciado en la barra de acción:

| Ataque | Desde | Qué pasa | Clave |
|---|---|---|---|
| **Rugido Cataclísmico** | Joroba | Todas las cabezas rugen a la vez, el cielo se oscurece (oscuridad y náusea) y una **onda expansiva** rueda por el suelo. **Sáltala**: a quien esté en el aire no le alcanza. | `roar-damage` (10) |
| **Andanada de Calaveras** | Joroba Creciente | Cada cabeza escupe un abanico de calaveras ardientes durante dos segundos. | — |
| **Lluvia de Escombros** | Joroba Hinchada | El suelo que se comió vuelve a caer: círculos rojos marcan dónde caerán las rocas. | `debris-damage` (8) |
| **Erupción Abisal** | Joroba Hinchada | El suelo bajo sus víctimas tiembla y, segundos después, revientan **tentáculos** de su masa que lanzan por los aires. | `eruption-damage` (12) |
| **Singularidad** | Destructor | Se queda quieta y arrastra todo el campo hacia su núcleo durante 4 s; luego el núcleo estalla. | `singularity-damage` (16) |

Además, alrededor de las formas colosales caen rayos y truena.

### Invocaciones

Una llamada cada `summons.cooldown-ticks` (700, +0–10 s), con como mucho `summons.max` (8) vivas a la vez. Todas mueren con ella.

| Invocación | Desde | Qué es |
|---|---|---|
| **Horda Enferma** | Joroba | `horde-size` (4, +2 en las formas colosales) zombis, esqueletos, arañas, vindicadores, husks y strays enfermos que salen de grietas moradas alrededor de una víctima. |
| **Simbionte Marchito** | Joroba Hinchada | El esbirro del mod: un esqueleto wither gigante (escala 1,5) con armadura morada y espada de netherite, `symbiont-health` (220) de vida, que dispara calaveras wither. Solo uno a la vez. |
| **Enjambre de Fantasmas** | Destructor | Tres fantasmas enfermos que caen en picado sobre una víctima. |

Todos sus golpes directos son **daño verdadero** (`max-damage-dealt` 20, `true-damage-pierce` 0.2). Como todos los jefes, su daño se **adapta** a lo que cada jugador ha invertido (`boss-balance.adaptive-damage`): sus golpes directos dentro del daño verdadero, y sus explosiones, calaveras e invocaciones como daño normal. Atraviesa el set Infinity y la espada Infinity le hace la mitad, igual que a los demás jefes.

### Cómo se pelea

- **Hitboxes:** la masa y cada cabeza son entidades `Interaction` que siguen al modelo. Las espadas usan el daño del arma, el enfriamiento, Afilado y **Castigo** (la tormenta es no-muerta como cualquier Wither). Los proyectiles que entran en una caja también cuentan.
- **Herir las cabezas:** cada proyectil que alcanza una cabeza cuenta; tras unos cuantos (1–2 en las jorobas, 3–8 en las formas colosales) la cabeza queda **herida** `injury-ticks` (200): su rayo se apaga, escupe una calavera azul y recibe ×1,5 de daño. Quien la hirió **escapa**: la tormenta lo deja en paz `escape-ticks` (800, los 40 s del mod). Con **todas** las cabezas heridas a la vez la tormenta queda **expuesta** `exposed-ticks` (160) y recibe ×`exposed-damage-multiplier` (2).
- **Hacerse el muerto:** el Destructor y el Devorador, al bajar a `play-dead-threshold` (15%), caen al suelo con las mandíbulas rotas durante `play-dead-ticks` (200) recibiendo ×1,5... y se levantan con todas las cabezas rugiendo, una onda que daña (`revive-damage` 12) y lanza a todos, y un 10% de vida recuperada. Solo una vez.
- **Muerte:** se deshace en rayos de luz mientras sus bloques revientan uno a uno; suelta estrellas del Nether (1 a 5 según la forma) y experiencia, y cura la enfermedad de todos.

### Rendimiento

El cuerpo viaja como pasajero del ancla (un armor stand marcador invisible, `MSC_WitherStorm`), que guarda la forma, la vida y lo comido: tras un reinicio la tormenta se reconstruye donde estaba, y ninguna pieza del cuerpo es persistente. Moverla cuesta un solo paquete; las piezas animadas se reenvían cada 3 ticks y solo si se movieron más de 3 cm o 1°, y el cuerpo gira en pasos que el cliente suaviza durante medio segundo. El Destructor usa la masa de baja resolución del mod (180 displays); `high-detail: true` dibuja la completa (548). Sin jugadores a menos de 50 bloques (100 en el Destructor y el Devorador) durante 5 s se va con sus invocaciones (`boss-balance.despawn`).

Todas las formas, poses y animaciones se pueden ver en el visor de modelos del sitio del proyecto (`docs/viewer/`). Desde una copia del repositorio se abre directamente en el navegador (doble clic en `docs/viewer/index.html`) después de `mvn test`; `tools/stand-viewer/textures.ps1` le añade las texturas de tu Minecraft.
