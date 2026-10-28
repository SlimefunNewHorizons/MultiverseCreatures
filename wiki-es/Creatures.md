# 🧟 Criaturas

Todas las criaturas de abajo **reemplazan los spawns naturales** de mobs vanilla con una probabilidad configurable. El enrutamiento de tiradas vive en `MobHandler` (`entities/handler/MobHandler.java`). Cada valor es configurable en `config.yml`. Cada criatura también se puede invocar con `/msc spawn <tipo>` (solo OP).

---

## 👻 Head Slime (Half-Life)

Un slime parásito que salta sobre su objetivo y **se adhiere como pasajero** a la cabeza del objetivo.

| Objetivo | Efecto mientras está adherido |
|---|---|
| **Jugador** | Ceguera II + Lentitud I + daño verdadero periódico (`damage-per-interval` cada `damage-interval-ticks`); se desprende automáticamente tras `max-attach-ticks` (200). Se desprende si el jugador recibe daño. |
| **Mob MSC** (modo buff) | Cada `buff-interval-ticks` (40t): Fuerza II + Velocidad II + Resistencia I + Resistencia al Fuego. Re-apunta el mob al jugador más cercano. Interacciones por mob: duplica el radio de explosión del creeper, hace que los esqueletos disparen 3 flechas críticas, da invisibilidad al Shadow Rogue, da absorción al Guardia de Obsidiana, etc. |
| **Creeper con Head Slime adherido** | La explosión inflige **24.0 de daño verdadero** escalado por distancia a los jugadores (en lugar del daño normal del creeper). |

No se adherirá a Mahoraga ni a una criatura que ya lleve uno.

**Sistema de inmunidad:** los jugadores que sostienen Gelatina de Head Slime son empujados a `immunePlayers` durante 10 s; un anillo de partículas de bruja se renderiza alrededor de los jugadores inmunes y el slime se desprende automáticamente.

- **Spawn natural:** `head-slime.spawn-chance` (10%) de los spawns de Slime
- **Drops:** Corazón de Head Slime (siempre)
- **Comando:** `/msc spawn headslime`

---

## 💥 Creeper Jr.

Un enjambre de tres creepers pequeños y rápidos que se funden al saltar e infligen **daño verdadero** (OUT_OF_WORLD, ignora armadura).

| Estadística | Valor por defecto |
|---|---|
| Escala | 0.6× el creeper normal |
| Velocidad | 0.5 (vs 0.2 vanilla — mucho más rápido) |
| Radio de explosión | 2 (vs 3 vanilla) |
| Ticks de mecha | 25 (muy cortos) |
| Daño verdadero máx. | 12 (escala por distancia al centro) |
| Daño a bloques | Ninguno (yield = 0) |

Al explotar se aplica knockback a las entidades no jugador cercanas, y otros Creeper Jr. en el área se ven obligados a soltar su objetivo.

- **Spawn natural:** `creeper-jr.spawn-chance` (15%) — aparece en trío
- **Drops:** ninguno especial
- **Comando:** `/msc spawn creeperjr`
- **Mensajes de muerte:** de `creeper-jr.death-messages`, con `%player%` reemplazado

---

## 🐴 ZombieHorseTrap (Ejército Militar — Evento de Luna Llena)

Una trampa rara de Luna Llena. Durante `MoonPhase.FULL_MOON`, el 0.1% de los spawns naturales de zombie se convierten en un **Caballo Zombie Militar** que deambula; cuando un jugador (no creativo/espectador) se acerca a menos de 6 bloques, desaparece y aparece un ejército coordinado de 5 unidades:

| Unidad | Etiqueta | Estadísticas y comportamiento |
|---|---|---|
| **Zombie Tank** (centro) | `MSC_ZombieTank` | Zombie escala 1.5, 350 HP, 10 de daño, armadura de hierro completa + casco de cuero lima. Resistencia I permanente + Lentitud I + Resistencia al Fuego. Recibe 50% menos daño de proyectiles. Su muerte activa la IA de posicionamiento a distancia del Duelista. |
| **Skeleton Duelist** (×2) | `MSC_Duelist` | Esqueleto 50 HP, cota de malla + casco de cuero púrpura. Lleva "Arco del Duelista" (Llama I / Poder III) para el rango y cambia a "Espada del Duelista" (Nitidez III / Knock II) a ≤6 bloques. Mantiene posiciones de flanco detrás del tanque mientras vive. |
| **Zombie Lancer** (montado) | `MSC_Lancer` sobre `MSC_LancerHorse` | Zombie con armadura de hierro + casco de cuero gris sosteniendo "Lanza de Hierro". Caballo: Resistencia I + Velocidad III + Resistencia al Fuego. Si el caballo muere, el Lancer recibe Velocidad III + Fuerza I permanentes. |
| **Camel del Ejército** (×2) | `MSC_ArmyCamel` con jinetes | Camello (Husk): Velocidad II + Resistencia II + Resistencia al Fuego. Jinete 1 — `MSC_CamelZombie` (Zombie, casco naranja, armadura de cobre, **Lanza de Diamante**) Resistencia I + Resistencia al Fuego. Jinete 2 — `MSC_CamelSkeleton` (Bogged, arco Poder II / Punch II). Si el camello muere, los jinetes ganan Velocidad (y el zombie Resistencia / el esqueleto Velocidad II). |
| **Skeleton Sniper** (retaguardia) | `MSC_Sniper` | Wither Skeleton 40 HP, armadura de cuero verde completa, "Arco del Francotirador" (Poder V / Infinito). **Predice el movimiento del jugador** (velocidad + arco de gravedad) y dispara flechas críticas cada 30 ticks hasta a 50 bloques. Las flechas aplican Wither I (100t) + Debilidad I (100t). |

**Comandos:**
- Ejército completo: `/msc spawn zombietrap`
- Unidades individuales: `/msc spawn tank`, `duelist`, `lancer`, `camel`, `sniper`

**Drops:** cada uno de Tank/Duelist/Lancer/CamelZombie/CamelSkeleton/Sniper tiene un 30% de probabilidad (`zombie-horse-trap.military-component-drop-chance`) de dropear un **Componente Militar**.

---

## ⛑️ Guardia de Obsidiana — Zombie tanque pesado

Zombie tanque pesado de netherita completa.

| Estadística | Valor |
|---|---|
| Base | Zombie (adulto, escala 1.8) |
| Salud | 300 |
| Daño | 8 |
| Velocidad | 0.15 (lento) |
| Armadura | Netherita completa + casco de obsidiana + Espada de Netherita ("Hoja de Obsidiana", Nitidez III / Knock II) |
| Buffs permanentes | Resistencia II, Resistencia al Fuego, Resistencia al Knockback completa |

**Habilidades:**
- **Provocación** (cd 100): apunta a todos los jugadores dentro de 20 bloques → Lentitud II + Debilidad I (60 ticks) + mensaje de chat "Face me!" + sonido de puerta de hierro.
- **Autocuración** (cuando HP < 100 Y cd > 200): Regeneración III 60 ticks + partículas de corazón.
- **Golpe cuerpo a cuerpo:** Debilidad II (60t) + Lentitud II (40t) al jugador.

- **Spawn natural:** `obsidian-guard.spawn-chance` (2%) de los spawns de Zombie
- **Drops:** 85% de probabilidad de **Fragmento de Obsidiana** + 100 XP
- **Comando:** `/msc spawn obsidianguard`

---

## 🌌 Caballero Ender

Caballero temático de enderman con una "Hoja Ender" de Espada de Diamante (Nitidez V, Knock II).

| Estadística | Valor |
|---|---|
| Base | Enderman |
| Salud | 120 |
| Daño | 14 |
| Velocidad | 0.3 |
| Rango de seguimiento | 30 |

**Habilidades (contra jugadores):**
- **Tirón Ender** (dist 5–25, cd 60): atrae al objetivo hacia el caballero (velocidad), 4 de daño + partículas de portal/end-rod + grito de enderman.
- **Carga Ender** (dist >8, cd 40): se teletransporta detrás del objetivo y apuñala por la espalda con 8 de daño + Lentitud II (60t) + ráfagas dobles de partículas de portal.
- **Toque de Levitación** (dist <4): Levitación I (20t). El cuerpo a cuerpo también aplica Levitación I (30t).

- **Spawn natural:** `ender-knight.spawn-chance` (4%) de los spawns de Enderman
- **Drops:** 55% de **Fragmento Ender** + 70 XP
- **Comando:** `/msc spawn enderknight`

---

## ❄️ Gólem de Escarcha

Gólem de Hierro reimaginado como guardián invernal. Lleva peto de cuero aguamarina.

| Estadística | Valor |
|---|---|
| Base | Gólem de Hierro |
| Salud | 200 |
| Daño | 12 |
| Velocidad | 0.18 |

**Habilidades:**
- **Aura de Hielo** (dist < 8, cd 40): anillo de 16 puntos de partículas DUST azul claro + SNOWFLAKE, Lentitud IV (60t) + Debilidad I (60t) + 4 de daño.
- **Rayo Congelante** (dist 5–20, cd 80): un rayo de DUST + SNOWFLAKE del gólem al objetivo, 10 de daño + Lentitud VI (100t) + Salto Mejorado -4 (bloqueo al suelo) (100t).
- **Cuerpo a cuerpo:** Lentitud III (80t) + Debilidad II (80t).
- Inmune a Lentitud / Debilidad / Salto Mejorado aplicados sobre sí mismo.

**Construcción crafteable:** coloca una Calabaza Tallada o Jack o'Lantern sobre 2 Bloques de Hielo Compactado/Azul con "brazos" de Hielo/Hielo Compactado/Azul a izquierda y derecha (se consume al activarse).

- **Spawn natural:** `frost-golem.spawn-chance` (8%) de los spawns de Gólem de Hierro
- **Drops:** 75% de **Corazón de Escarcha** + 80 XP
- **Comando:** `/msc spawn frostgolem`

---

## 🔥 Elemental de Llama

Un blaze con proyectiles meteoro buscadores.

| Estadística | Valor |
|---|---|
| Base | Blaze |
| Salud | 80 |
| Velocidad | 0.25 |

**Habilidades:**
- **Proyectil meteoro** (cd 60, dist 3–20): meteoro buscador desde 1.5 bloques por encima del elemental. Emite partículas FLAME, SMOKE, LAVA y DUST naranja. En impacto directo (dist<2): 100 ticks de fuego + 12 de daño + explosión. En impacto/agotamiento: 80 ticks de fuego + 6 de daño a jugadores dentro de 3 bloques.
- Cuando el objetivo está dentro de 5 bloques: añade 20 ticks de fuego + 3 de daño si el elemental está en llamas. El cuerpo a cuerpo prende fuego al jugador (80 ticks).

- **Spawn natural:** `flame-elemental.spawn-chance` (10%) de los spawns de Blaze
- **Drops:** 60% de **Núcleo de Magma** + 40 XP
- **Comando:** `/msc spawn flameelemental`

---

## ⚡ Invocador de Tormentas

Una bruja que doblega los rayos.

| Estadística | Valor |
|---|---|
| Base | Bruja |
| Salud | 60 |
| Velocidad | 0.28 |

**Habilidades:**
- **Golpe de Rayo** (cd 60): dos rayos cerca del objetivo (desplazamiento aleatorio ±4, ajustado al bloque más alto). Jugadores dentro de 4 bloques de un rayo: 10 de daño + Lentitud III (40t) + sonido de trueno.
- **Nube de Tormenta** (cd 100): una nube giratoria de partículas DUST (gris-azul) + CLOUD 5 bloques por encima del objetivo, luego 4 rayos en un área de 10 bloques: 8 de daño + Lentitud II (60t).

Prioridad de tirada: **el Invocador de Tormentas tira primero** cuando aparece una Bruja natural; si falla, tira la Bruja de Veneno.

- **Spawn natural:** `storm-caller.spawn-chance` (4%) de los spawns de Bruja
- **Drops:** 60% de **Cristal de Tormenta** + 50 XP
- **Comando:** `/msc spawn stormcaller`

---

## 🧪 Bruja de Veneno

Una bruja especialista en veneno.

| Estadística | Valor |
|---|---|
| Base | Bruja |
| Salud | 50 |
| Velocidad | 0.25 |

**Habilidades:**
- **Nube Tóxica** (cd 80): AreaEffectCloud de radio 3.5, 100 ticks, verde 0x66FF00, con Veneno III (60t) + Wither II (40t) + Lentitud II (60t).
- **Proyectil Debuff** (cd 50): Veneno II (100t) + Debilidad II (100t) + Ceguera I (40t) + 4 de daño + partículas de bruja.

- **Spawn natural:** `venom-witch.spawn-chance` (5%) de los spawns de Bruja (solo si falla la tirada del Invocador de Tormentas)
- **Drops:** 60% de **Glándula de Veneno** + 30 XP
- **Comando:** `/msc spawn venomwitch`

---

## ⚰️ Segador de Almas

Un wither skeleton con guadaña y robo de vida.

| Estadística | Valor |
|---|---|
| Base | Wither Skeleton |
| Salud | 100 |
| Velocidad | 0.28 |
| Rango de seguimiento | 30 |
| Arma | Hacha de Netherita ("Guadaña del Segador de Almas", Nitidez V / Aspecto Ígneo II) |

**Habilidades:**
- **Drenaje de Alma** (dist <8, cd 60): 10 de daño, se cura el 50% del daño infligido, aplica Wither III (80t) + Hambre III (80t). Flujo de partículas END_ROD + PORTAL del objetivo a sí mismo + sonido de disparo de wither.
- **Robo de vida cuerpo a cuerpo**: se cura el 30% del daño final. Aplica Wither II (100t).

- **Spawn natural:** `soul-reaper.spawn-chance` (5%) de los spawns de Wither Skeleton
- **Drops:** 60% de **Esencia de Segador** + 60 XP
- **Comando:** `/msc spawn soulreaper`

---

## 🌀 Mago del Caos

Un evocador que lanza uno de 6 hechizos caóticos aleatorios cada 50+ ticks.

| Estadística | Valor |
|---|---|
| Base | Evocador |
| Salud | 70 |
| Velocidad | 0.25 |

**Hechizos (aleatorio 0–5):**
0. Invoca un "Vex del Caos" (etiqueta `MSC_ChaosVex`) como pasajero.
1. Anillo de fuego + prende fuego al objetivo (80t) + 8 de daño.
2. Invoca 3 Creepers a su alrededor.
3. Debuff aleatorio (Veneno/Wither/Lentitud/Debilidad/Ceguera/Hambre/Levitación/Oscuridad, amp 2, 120t) + 5 de daño + partículas de bruja.
4. Dispara 4 SmallFireballs en direcciones cardinales (yield 0).
5. Intercambia posiciones con el objetivo; teletransporte + aplica Náusea I (100t) + partículas de portal + sonido de enderman.

- **Spawn natural:** `chaos-mage.spawn-chance` (6%) de los spawns de Evocador
- **Drops:** 60% de **Orbe del Caos** + 50 XP
- **Comando:** `/msc spawn chaosmage`

---

## 🕳️ Void Crawler

Una araña emboscadora que cambia de fase.

| Estadística | Valor |
|---|---|
| Base | Araña |
| Salud | 80 |
| Velocidad | 0.35 (la más rápida de tipo araña) |

**Habilidades:**
- **Teletransporte de Fase** (dist >4, cd 50): si está atascada en un bloque sólido, se teletransporta a un espacio abierto cercano; si no, 10% de probabilidad por tick de teletransportarse aleatoriamente. Partículas de portal en ambos extremos.
- **Ráfaga de Veneno** (dist <6, cd 60): anillo de 12 puntos de partículas DUST púrpura oscuro (0x8800AA) + WITCH; Veneno III (100t) + Wither II (60t) + 6 de daño.
- **Cuerpo a cuerpo:** Veneno II (80t) + Wither I (40t).

- **Spawn natural:** `void-crawler.spawn-chance` (7%) de los spawns de Araña
- **Drops:** 50% de **Esencia del Vacío** + 35 XP
- **Comando:** `/msc spawn voidcrawler`

---

## 🗡️ Shadow Rogue

Un asesino esqueleto rápido.

| Estadística | Valor |
|---|---|
| Base | Esqueleto |
| Salud | 60 |
| Velocidad | 0.35 |
| Arma | Espada de Netherita ("Hoja de las Sombras", Nitidez IV / Knock I) + casco de vidrio negro |

**Habilidades:**
- **Teletransporte de Sombra** (dist² >25, cd 60): se teletransporta 2 bloques detrás del objetivo + partículas de portal en ambos extremos + sonido de enderman; re-bloquea el objetivo.
- **Apuñalada por la Espalda** (dist² <9, cd 40): solo se activa si el objetivo mira hacia otro lado (dot > 0.7). Inflige **18 de daño**, knockback, Ceguera II (60t) + Lentitud III (60t). Partículas CRIT + SWEEP_ATTACK + sonido crítico.

- **Spawn natural:** `shadow-rogue.spawn-chance` (5%) de los spawns de Esqueleto
- **Drops:** 50% de **Fragmento de Capa de las Sombras** + 30 XP
- **Comando:** `/msc spawn shadowrogue`

---

## 🦴 Bone Shield

Un esqueleto defensivo con un muro de huesos que se recarga.

| Estadística | Valor |
|---|---|
| Base | Esqueleto |
| Salud | 120 |
| Velocidad | 0.2 |
| HP del escudo | 30 (se recarga tras 100 ticks cuando se agota) |
| Mano secundaria | Escudo (se re-equipa automáticamente si se pierde) |

**Habilidades:**
- **Escudo pasivo:** reduce el daño entrante un 60% (absorbe hasta el HP del escudo), partículas de bloques de hueso al bloquear + sonido.
- **Contraataque:** al bloquear, refleja 3.0 de daño a los jugadores atacantes + partículas CRIT.
- Partículas orbitales DUST (0xEEEEEE) alrededor del esqueleto en un anillo de 8 puntos.

- **Spawn natural:** `bone-shield.spawn-chance` (6%) de los spawns de Esqueleto (se tira junto a Shadow Rogue)
- **Drops:** 80% de **Hueso Reforzado** + 40 XP
- **Comando:** `/msc spawn boneshield`

---

## 🛒 Comerciante Multiversal ("Shaggy")

Un reemplazo del Comerciante Errante — el 30% (`SHAGGY_CHANCE`) de los spawns de comerciante se convierten en "Comerciante Multiversal" con **intercambios personalizados de todo el multiverso**:

| Objeto | Coste | Usos |
|---|---|---|
| 5× Galleta de Scooby | 20 Diamantes | 999 |
| Excalibur | 16 Núcleos Estelares + 32 Lingotes de Netherita | 1 |
| Corona del Rey Helado | 48 Estrellas del Nether + 64 Hielo Azul | 1 |
| Linterna de Wirt | The Beast's Antler + Edelwood Oil | 1 |
| Garras de Mantis | 16 Lingotes de Hierro + 8 Cuerdas | 999 |

- **Comando:** `/msc spawn merchant`

---

## 👁️ El Monstruo

Una figura alta y pálida, con brazos tan largos que llegan al suelo, hecha de 80 cabezas de jugador. No hace daño: está ahí para que lo veas de reojo.

Cada `check-interval-seconds` (20) un jugador que está a oscuras (luz en los ojos ≤ `max-light-level`, 7) puede atraerlo, con una probabilidad `chance` del 4%, una vez cada `cooldown-seconds` (15 min). Primero llegan los **presagios**, durante `omen-seconds` (30), que solo tú oyes y ves: pasos a tu espalda que se paran cuando están cerca, una puerta que se abre y se cierra cerca, las antorchas y faroles a tu alrededor apagándose uno a uno (solo en tu pantalla; vuelven cuando acaba la visita) y quizá, una vez, un **vistazo** de él de pie al borde de tu vista, que desaparece en cuanto te giras. `omen-seconds: 0` los quita, y los comandos nunca los esperan.

Luego espera **donde no estás mirando**, en una pose que se adapta al lugar:

| Dónde estás | Dónde espera | Poses |
|---|---|---|
| Al aire libre | En el suelo, detrás o al lado de ti | justo detrás, sobre el hombro, asomándose de lado, agachado, cabeza ladeada, alcanzándote, grito |
| En una sala baja o un túnel | En el suelo, plegado para caber | gateando, encorvado bajo el techo, sentado en un rincón |
| Bajo un techo | Colgado del bloque de arriba | pegado al techo como una araña, colgando boca abajo |
| Cerca de cualquier bloque sólido (un cofre, una mesa de crafteo, piedra…) | Encima de él | tumbado boca arriba, sentado en el borde, encaramado, tumbado boca abajo |
| Volando o planeando | En el aire, a tu lado | flotando |
| En un hueco, pozo o agujero de 1×1 | Plegado en ese bloque | apretado en un pozo, doblado en un agujero, estirado hacia arriba, cabeza fuera del agujero, colgado en un pozo |
| En un pasillo de 1×2 (un bloque de ancho) | A lo largo | gateando en el pasillo, acechando en el pasillo, agachado en el pasillo, tumbado en una ranura, gateando por el techo del pasillo |
| En un nicho de 2×1 (un bloque de fondo) | De lado a lado | abierto contra la pared, en cuclillas, sentado en el nicho, acurrucado de lado, pegado al techo a lo ancho |
| Cerca de un cofre doble (o dos bloques sólidos juntos) | A lo largo | tumbado sobre un cofre doble, incorporado sobre un cofre doble, sentado sobre un cofre doble, de lado sobre un cofre doble, boca abajo sobre un cofre doble |

Un sitio solo vale si la pose entera cabe ahí y ni sus ojos ni su pecho están a la vista. Las poses estrechas se prueban alineadas con los bloques en las cuatro direcciones y solo se usan donde el espacio de verdad es así de estrecho, así que al aire libre nunca aparece plegado.

**Cuando lo ves** (a menos de `view-angle`, 30°, del centro de la vista), se delata con uno de seis comportamientos:

| Comportamiento | Qué hace | Cuándo |
|---|---|---|
| Caminar | Se levanta como pide su sitio (cae dando la vuelta desde el techo, salta del bloque, se despliega fuera del hueco) y camina hacia ti | La mayoría de las veces |
| Salir del ataúd | Se incorpora despacio sobre el cofre doble, pasa las piernas por el borde y baja | Siempre desde un cofre doble |
| Carrera a gatas | Se pone a cuatro patas y corre hacia ti, más rápido | 1 de cada 4 desde el suelo |
| Correr por el techo | Corre boca abajo por el techo hasta encima de ti y cae delante | 6 de cada 10 desde un techo |
| Asomarse y esconderse | Se aparta fuera de tu vista y vuelve a esperar más cerca, donde no miras | 3 de cada 10, una vez por visita |
| Parpadeo | Parpadea, desaparece y de pronto está en tu cara | Cuando nada más cabe |

Todos terminan igual: cuando está cerca, su cuerpo desaparece y **solo su cara aparece de golpe justo delante de tus ojos**, con la mandíbula abierta y temblando, con un grito (durante `scream-ticks`, 20 ticks, mires a donde mires, mientras vas ralentizado). Luego desaparece en una nube de tinta, con Oscuridad y un instante de Ceguera.

Desde que llega **lo oyes respirar** donde está, y solo tú lo oyes: una respiración lenta y profunda cada pocos segundos mientras espera, con algún olfateo, y respiraciones rápidas cuando va a por ti. Donde se le vea, **su cabeza gira despacio para seguirte**, y mientras va a por ti tu vista late a oscuras (`darkness-while-coming`). Si nadie lo ve en `lurk-seconds` (45), se va sin hacer ruido mientras no miras, pero deja **huellas** que se alejan durante `trace-seconds` (12), y una puerta, trampilla o valla cerca de donde esperaba se ve abierta (solo para ti).

`sounds.scream` y `sounds.breath` aceptan un sonido de resource pack (p. ej. `myhorror:monster.scream`) para sonar en lugar de la mezcla vanilla; vacío la mantiene.

**Nunca atraviesa bloques.** Todo lo que hace se planea contra los bloques de alrededor antes de empezar, tick a tick: camina hacia ti por una ruta, de pie donde hay espacio, estrecho en un pasillo, agachado bajo un techo bajo y a cuatro patas en un túnel, y sale de un hueco con un tirón cuando no hay espacio para desplegarse despacio. Si no hay ninguna salida (un pozo cerrado, un agujero hondo) parpadea en su lugar.

- **Comandos:** `/msc spawn monster` lo envía ya al jugador más cercano, sin probabilidad, luz ni enfriamiento; `/msc troll <jugador> monster <posición>` lo envía en la posición que elijas (ver [Comandos](./Commands.md)).
- **Animación:** el visor reproduce cada comportamiento que puede hacer cada pose, en los bloques para los que se hizo (el pozo, el pasillo, el nicho, el cofre doble): elige una pose y luego un comportamiento en Animation.
- **Config:** `entities.monster`
- **Visor:** todas las poses, con un jugador como escala, en el [visor 3D](https://slimefunnewhorizons.github.io/MultiverseCreatures/viewer/?stand=monster).

---

## Tabla resumen de spawns

| Mob vanilla | Reemplazo MSC | Probabilidad por defecto |
|---|---|---|
| Zombie (Luna Llena) | ZombieHorseTrap (ejército) | 0.1% |
| Zombie | Mahoraga | 2% |
| Zombie | Guardia de Obsidiana | 2% |
| Slime | Head Slime | 10% |
| Creeper | Creeper Jr. (×3) | 15% |
| Esqueleto | Shadow Rogue | 5% |
| Esqueleto | Bone Shield | 6% |
| Blaze | Elemental de Llama | 10% |
| Gólem de Hierro | Gólem de Escarcha | 8% |
| Araña | Void Crawler | 7% |
| Bruja | Invocador de Tormentas (tira primero) | 4% |
| Bruja | Bruja de Veneno (tira segunda) | 5% |
| Wither Skeleton | Segador de Almas | 5% |
| Evocador | Mago del Caos | 6% |
| Enderman | Caballero Ender | 4% |
| Comerciante Errante | Comerciante Multiversal | 30% |