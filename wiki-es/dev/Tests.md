# 🧪 Tests

Esta página documenta **cómo se prueban** los cambios del plugin y **qué verifica cada suite**. Los tests solo cubren lógica pura y matemática de alto riesgo (los jefes y rituales se rompen fácilmente si se tocan); no existe un servidor real como dependencia.

## ⚙️ Framework y ejecución

- **JUnit 5 (Jupiter)** — dependencia `org.junit.jupiter:junit-jupiter:5.11.4` (scope `test`).
- **Maven Surefire 3.5.2** — ejecuta los tests automáticamente en la fase `test`.
- **Java 21** — mismo compilador que el código principal.
- **Headless**: no se arranca un servidor Paper/Purpur. Las clases de Bukkit que se tocan (p. ej. `World`) se simulan con `java.lang.reflect.Proxy` o se usan objetos `Location` con mundo `null` para ejercitar solo la aritmética.
- **SnakeYAML** (viene con `purpur-api`) parsea `config.yml` y `plugin.yml` en `ConfigFilesGuardTest`, así una indentación inválida falla en la suite y no al arrancar el servidor.
- El **soporte de tests** (`testsupport/ProjectPaths`, `testsupport/LimbGeometry`, `testsupport/SourceText`) es el arnés que comparten las guardias: encuentra el proyecto subiendo desde el directorio de trabajo (una guardia de fuentes leía `src/main/java` desde donde se lanzara Maven, no encontraba nada y pasaba de forma vacua), nombra un archivo por segmentos con un único modo de fallo, lee la segunda articulación de una extremidad del propio export en vez de fiarse del código que la fija, y `SourceText` lee el cuerpo de un método o quita comentarios para las guardias de fuentes, así que las tres no pueden separarse.

Comandos:

```bash
mvn test                     # Ejecuta todos los tests
mvn clean package -DskipTests  # Compila el JAR saltando los tests
```

Para ejecutar una clase concreta:

```bash
mvn test -Dtest=InvocationStructuresTest
```

## 📋 Inventario de tests

Las 64 clases de test viven en `src/test/java/com/Chagui68/` reflejando el paquete de la clase que prueban.

### `utils/MscEntityUtilsHealthTest` — Salud virtual de los jefes
Cubre la aritmética de salud de `utils/MscEntityUtils`:
- **`calculateSafeHealth`** — Ajusta la salud pedida al límite del servidor (cap de atributo, por defecto 1024 en Paper) evitando `IllegalArgumentException`. Verifica casos de jefes reales: ArmorStandBoss (3200), NIX (450), Frost Golem (200); clampa a mínimo **0.1** (evita muerte instantánea al aparecer) y protege de entradas negativas.
- **`calculateVirtualProgress`** — Clampa el progreso de la boss bar entre 0.0 y 1.0 (incluye `0/0` = 0).
- **`calculateScaledPhysicalHealth`** — Convierte la salud **virtual** (p. ej. 3200) a la salud física real almacenada en la entidad (escalada a su máximo físico), con 0 → muerte.
- **`clampHitboxScale`** — La escala del stand de cada jefe se acota a `0.25`–`8.0`: los bordes siguen siendo usables, `0`, negativos, `NaN` y ambos infinitos caen a un stand normal (`1.0`), un valor enorme se corta en `8.0`, y las cuatro escalas que envían los jefes (`1.0`, `1.2`, `1.9`, `7.5`) pasan intactas.

### `utils/MscGeometryOverlayTest` — Geometría dibujada en el mundo
- El overlay de la hitbox se dibuja por las **doce aristas** de la caja: cada arista corre por un único eje, suman cuatro de cada lado y se encuentran en exactamente las ocho esquinas — un contorno al que le falta una arista escondería justo el hueco que la auditoría busca. Una caja degenerada (vacía) también se dibuja en vez de lanzar excepción.
- Una articulación se coloca en el **mismo marco que usan las piezas del display** (`yaw + 180`): un stand mirando al norte refleja el punto del modelo, un cuarto de vuelta lo lleva al otro eje, y ninguna rotación cambia su altura ni su distancia al stand.
- Las articulaciones que el comando entrega de verdad (las constantes reales de Kinger, NIX y Jack Star, **codos y rodillas incluidos**) quedan dentro de un cuerpo: dentro de la altura del stand, cerca del eje del cuerpo y sobre su propio plano.
- La **repetición del caminar** construye su caja solo con la escala de hitbox (medio bloque de ancho y 1.975 de alto a escala 1, centrada en el ancla con los pies sobre ella) y convierte una extremidad posada en huesos: uno para una extremidad que no se pliega, dos que se encuentran en su articulación cuando sí lo hace. El rig de Kinger son seis huesos; los de NIX y Jack Star, ocho.
- Una guardia de fuentes mantiene el comando cableado: cada uno de los tres modelos que caminan se puede reproducir con `::walkPose` a su propio `WALK_RATE`, y el dibujo pasa por el overlay — la repetición no puede perder un modelo en silencio, ni convertirse en un segundo renderer.

### `utils/DisplaySuitTest` — El traje que viste cada jefe
- Una adopción reconoce una pieza solo cuando coinciden sus **etiquetas de traje, pieza y propietario**: una pieza de otro jefe, otra pieza del mismo traje, o la etiqueta de traje sin las demás nunca se adoptan.
- La búsqueda salta las entidades que no son displays, no devuelve nada cuando no hay qué adoptar, e ignora un mundo o una localización ausentes.
- El traje se construye en **un solo sitio**: una guardia de fuentes falla si alguno de los tres jefes vestidos construye su propia cabeza desde la textura, pone a cero sus propios ajustes de display u olvida adoptar/quitar sus piezas.
- `shouldSync` actualiza un traje ocupado cada tick y uno quieto cada tres ticks.

### `utils/MscLeftoversTest` — Barrido de sobrantes de ataques al arrancar
- Los props de un ataque que un reinicio cortó se eliminan al habilitar (escudos orbitando, el portador de escudo plantado, el anillo de lanzas, los paneles de alas, el sello triangular, las copias espejo, las balas de Kinger), porque su ataque ya no existe y nada más los quitaría.
- La lista se prueba también desde el otro lado: **criaturas** (`MSC_FrostGolem`, las invocaciones), **piezas del traje** (cada jefe adopta las suyas) y no se barren nunca. Los marcadores de los retirados `/msc dummy` y `/msc seal` (`MSC_Dummy`, `MSC_SealMarker`) sí se barren, porque ya nadie los posee.
- El barrido solo toca las entidades que reconoce — no descarga el mundo alrededor — y un mundo ausente se ignora en vez de lanzar excepción.

### `utils/MscBossBarTest` — Quién ve una barra de jefe
- Una barra de jefe es un paquete por jugador, no un objeto del mundo, así que la lista de espectadores hay que revisarla: el jugador que **entra más tarde** recibe la barra y al que ya la tenía no se le añade dos veces.
- El espectador que **se desconecta** o se va a otro mundo deja de verla; una barra limitada por distancia (la de Jack Star) cambia sus espectadores por los jugadores dentro del rango en vez de acumularlos.
- Una barra, un mundo o una localización ausentes se ignoran en vez de lanzar excepción, para que una barra entregada a medias no pueda romper el ticker.

### `utils/MscLimbTest` — La segunda articulación de una extremidad
- La articulación entre dos segmentos del export está **a medio camino entre sus centros**, así no hay que suponer el tamaño de ninguna pieza y un reexport mueve la articulación con ella; una extremidad quieta queda exactamente donde la deja el export, segunda articulación incluida.
- En todo el rango que alcanza un paso, el segmento inferior conserva su distancia exacta a la segunda articulación — no puede desprenderse de la extremidad — y se queda por debajo de esa articulación en vez de plegarse a través del muslo.
- Una **rodilla** se dobla solo en la mitad trasera del balanceo y un **codo** solo en la delantera, ambos por una fracción documentada del balanceo del padre (`1.5×`, con tope en `1.2` rad ≈ 69°) y siempre en la dirección en la que ya va la extremidad, así la articulación suma al balanceo en vez de adelantarlo o cancelarlo.
- Posar una extremidad la mantiene rígida —el pivote no se mueve y ambos huesos conservan sus longitudes— mientras que un plegado acerca la mano o el pie a la articulación de la que cuelga más de lo que llega la extremidad recta. Una extremidad exportada de una pieza gira rígida y nunca le crece una articulación que no tiene.

### `ritual/BossDimensionTest` — El mundo de la dimensión del ritual
- Todas las etapas de generación vanilla siguen apagadas en `WastelandGenerator`: si `shouldGenerateNoise()` devolviera true se generaría terreno del overworld bajo y alrededor del campo de batalla.
- El borde del mundo mide 1500 bloques por defecto y se limita a 100 - 1500 diga lo que diga el config.
- Una carpeta de mundo sin la marca del generador, o con el nombre de otro generador, se detecta como antigua (y se regenera); una con la marca actual se conserva.

### `ritual/terrain/WastelandTest` — El campo de batalla que recibe la pelea
- La arena es plana en y=40 y no hay nada encima ni a menos de 40 bloques; a su alrededor ningún escalón supera un bloque, y no hay lava dentro del radio de peligro.
- La lava nunca sube de su nivel y siempre toca lava o roca a los lados y debajo; toda columna tiene bedrock en y=0 y es sólida hasta su superficie; hay cordillera a lo largo de todo el borde.
- Todos los peligros y estructuras (lava, magma, ambos fuegos, agujas, huesos, ruinas, los tres suelos) aparecen en el campo de batalla, la misma semilla reconstruye las mismas columnas, y otra semilla genera un páramo distinto alrededor de la misma arena.

### `ritual/InvocationStructuresTest` — Todas las estructuras de invocación
- Los cuatro altares de 5×5 (el cadalso de NIX, el trono de DIO, la terminal de JACKSTAR y el altar del panteón) comparten un mismo diseño, comprobado una sola vez para los cuatro: cuatro velas en la capa del suelo a **distancia Manhattan 1** del centro y cuatro pilares en las esquinas.
- Cada estructura pone su centro y sus marcas donde el ritual los espera: el yunque de NIX en `origin + (2.5, 0.5, 2.5)` y el brillo de sus horcas a `Y + 1.8`, el trono de DIO y el altar del panteón un bloque más arriba, la luz de los pilares de DIO a `Y + 2.6`, el centro del ritual del overworld en `(3, 0, 3)` con radio 5 y el del círculo del Centinela en `(2, 0, 2)`.
- Solo los huecos de vela cuentan como velas — nunca el centro, un pilar ni nada de fuera — en NIX, JACKSTAR y el anillo de 12 velas del Centinela; el ritual del overworld tiene exactamente 12 velas en la capa `Y=1`, ninguna en el centro.
- El núcleo, las velas y la base de JACKSTAR aceptan lo que documenta el ritual y nada más, y cualquier cráneo o cabeza corona un pilar de DIO.

### `commands/CommandCatalogueTest` — Tablas de `/msc spawn`, `give` y `attack` y sus páginas de ayuda
- Cada tabla es la única fuente que leen el ejecutor, la ayuda y el autocompletado, así que el test comprueba que coinciden: **un nombre en minúsculas por entidad, objeto o ataque**, buscado sin importar mayúsculas, y los atajos retirados (`army`, `sword`, `warrant`, `slam`, `heal`, `crossbarrage`, …) ya no existen.
- `spawn`: cada tipo documentado aparece exactamente una vez, Jack queda fuera de la ayuda y cada tipo arma sus mensajes de éxito y fallo.
- `give`: cada nombre que muestra la ayuda (incluidas las líneas agrupadas como `reaperessence &8/ &evoidessence`) es un objeto entregable con su fábrica.
- `attack`: la ayuda lista cada ataque registrado más las siete mecánicas (`flyup`, `land`, `reset`, `phase*`), la misma lista que ejecuta `ArmorStandBoss.COMMAND_MECHANICS`.
- La aritmética de páginas del `CommandMenu` real: las páginas se limitan a `[1, total]`, el conteo cubre cada línea con al menos una página, cada línea sale en exactamente una página, y cada página de cada menú tiene título y líneas.

### `commands/AttackRegistryCoherenceTest` — las cinco listas de ataques siguen de acuerdo
- Lee las fuentes de los ataques y `ArmorStandBoss` y comprueba que cada clase de ataque está registrada en `initAttacks()` y responde al nombre derivado de su clase (con las excepciones documentadas `executionsweep`, `soultethers`, `runemines`).
- Los conjuntos aéreo/suelo coinciden con las carpetas donde viven las clases, ningún ataque está en ambos conjuntos, y los de distancia/defensivos no están en ninguno.
- El catálogo de ayuda y las fuentes son el mismo conjunto de nombres, así que no se anuncia nada que no pueda ejecutarse ni queda oculto nada ejecutable.

### `commands/MscKillFilterTest` — Predicados de `/msc kill`
- Las etiquetas de scoreboard con prefijo `MSC_` identifican a una entidad del plugin; los nombres legacy sin etiqueta (Mahoraga, Garou, Bone Shield, …) siguen contando; los mobs vanilla quedan intactos.
- El filtro por tipo compara etiquetas con `-`/`_` eliminados y cae al nombre como subcadena; un tipo `null` o vacío nunca coincide.

### `entities/boss/NixDamageCapTest` — Cap de daño de NIX
- Nix nunca puede perder más de `entities.nix-executioner.max-damage-per-hit` (por defecto **100**) en un solo golpe: lo que supera el cap se recorta, lo que queda por debajo pasa intacto y el cap nunca *infla* un golpe.
- `0` (o cualquier valor no positivo) desactiva el límite, que es la forma documentada de volver al comportamiento sin tope.
- Fija la aritmética del pool de vida: una ráfaga de 10 000 deja 350 de 450 HP, cuatro golpes con cap dejan 50 y el quinto termina con el jefe.

### `entities/boss/PenetratingDamageTest` — Daño penetrante del Centinela
- **La armadura se devuelve**: el motor ya ha aplicado `ARMOR`, `MAGIC` (encantamientos de Protección) y `RESISTANCE` al daño del evento, y `unmitigated` deshace esos tres para que un tajo de 22 sobreviva a netherite completo. El bloqueo con escudo *no* se devuelve a propósito y el resultado nunca es negativo.
- `penetratingDamage` mantiene la Resistencia parcialmente efectiva: el jefe ignora `penetrating-resistance-pierce` (por defecto **0.2**) de la reducción de la poción, así que Resistencia I bloquea el 16% en lugar del 20% (un golpe de 10 quita 8.4), `0.0` deja la poción totalmente efectiva y `1.0` la ignora por completo. La mitigación es del 20% por nivel y se topa al 100% (Resistencia V).
- Los valores de perforación fuera de rango se recortan, un golpe nunca puede superar el daño bruto y el cap por golpe (`max-damage-dealt`, 15) se aplica antes de la Resistencia, así que la poción nunca puede subirlo.
- `PenetratingHit` encadena los mismos pasos que el handler en vivo (devolver, topar, perforar) y es lo que imprime `/msc debug`: un tajo de 22 contra netherite completo queda en 12.6, un golpe absorbido por la armadura acaba en 0 en vez de negativo o `NaN`, la Resistencia se muestra como nivel desde 1 y la antigüedad nunca es negativa.

### `entities/boss/SentinelDefenseTest` — Daño entrante del Centinela
- Fija todo el stack defensivo del jefe, ahora extraído de su manejador de eventos a `SentinelDefense`: sello de escudo ×0.5, círculo de curación ×0.8, piel de piedra ×0.5, barrera reflectante ×0.7, el escudo de absorción gastando su vida y el cap `max-damage-per-hit` aplicado **al final**.
- Comprueba las trampas que escondía la versión inline: las defensas no pueden subir un golpe por encima del cap (`200 → 40 → cap 30`), un golpe justo en el cap no se marca como capeado, la barrera reflectante devuelve el 30% del golpe *reducido* y el cap no recorta lo que devuelve, y un jefe invulnerable no genera ningún paso de cap.
- La traza `steps` se comprueba byte a byte porque `/msc debug` la imprime, y un barrido por todas las combinaciones de defensa demuestra que ninguna puede agrandar un golpe ni devolver un valor negativo.

### `commands/DebugReportTest` — Render de `/msc debug`
- Fija las líneas renderizadas sin sender: un golpe penetrante lista su daño del evento, la armadura/Protección/Resistencia devueltas, el total tras la armadura, el cap, la perforación, el nivel de Resistencia y el valor final; una muestra `DEALT` empareja el daño deseado del ataque con lo que el jugador recibió; una muestra `TAKEN` detalla el cap o el reparto del load balancer entre el golpe y lo que costó.
- Una nota de mecánica vacía se omite en vez de dejar un separador suelto, y la línea de edad se controla con un reloj inyectado para que la salida sea determinista.

### `entities/boss/BossDamageLogTest` — Registro de `/msc debug`
- Mantiene una muestra por jugador, jefe y sentido: una muestra nueva sustituye a la anterior en su hueco, y `samplesFor` las devuelve ordenadas por `BossId` y luego por sentido sin importar el orden de inserción, así que el informe no puede reordenarse entre ejecuciones.
- Los jugadores se siguen por separado, `forget` limpia uno sin tocar otro, y los registros nulos se ignoran en vez de lanzar.
- `forgetBoss` elimina las muestras de un jefe para todos los jugadores en una sola pasada y deja intactos los otros jefes: es la misma llamada que hace el listener de la propia bitácora cuando el soporte de un jefe sale del mundo.

### `entities/boss/JackResilienceTest` — Daño entrante de Jack Star
- Cada golpe se resuelve por una sola puerta: un esquive explícito no quita nada, cualquier otro golpe se reparte.
- El reparto **conserva el golpe** (`toBoss + sharedTotal == incoming`) en un barrido de valores y tamaños de grupo, así que un ajuste de balance no puede borrar ni duplicar daño en silencio.
- Un golpe que acierta **siempre llega al jefe**, sea cual sea el tamaño del grupo: es lo que mantiene a Jack Star recibiendo daño en todo momento; una tirada exactamente en la probabilidad de esquive sigue acierta.
- La forma comprimida (degradada) sube la probabilidad configurada a 0.45, y el valor límite conserva la configurada.

### `entities/boss/JackModelTest` — Geometría del modelo de Jack Star
- Las once piezas coinciden con el **modelo de referencia del juego** salvo un desplazamiento en X común, y el modelo queda **re-centrado en la hitbox**; cabeza sobre torso sobre piernas, coronilla cerca de los dos bloques.
- El tajo dobla codos y rodillas, el cambio de forma escala traslaciones y tamaños a la vez, y la hitbox del soporte cubre cabeza, torso y piernas (los brazos quedan fuera a propósito).

### `entities/boss/HumanoidRigTest` — Lo que comparten NIX y Jack Star
- Un mismo contrato sobre los dos modelos. **Pose de reposo**: extremidades izquierda y derecha simétricas, ninguna pieza en el sitio de otra, cada pieza del lado y en el eje de su articulación, una extremidad que gira conserva su X y nunca se separa de su articulación, y cada **codo y rodilla donde el export deja el mayor hueco** entre los segmentos (sacado de la geometría, nunca del código).
- **Caminar**: solo se dobla la mitad inferior de cada extremidad, con el ángulo de su propia extremidad; todas las piezas quedan sobre la hitbox del soporte (salvo los brazos de Jack, a propósito); el esqueleto de la repetición mantiene huesos rígidos y pivotes fijos, se dobla justo en las articulaciones de las piezas, deja el pie atrás en la mitad trasera del paso y la mano delante en la delantera; y el jefe avanza al `WALK_RATE` de su modelo.

### `entities/boss/NixModelTest` — Geometría del modelo de NIX
- Las 27 piezas quedan fijadas contra el **modelo exportado**: cada traslación coincide y todo el cuerpo comparte un único eje X. El modelo queda **centrado en la hitbox** (`NixModel.baseTranslation` deja la columna en cero en vez del `+0.066` del export), y `CENTER` es el **punto medio de los extremos exportados**, que ninguna pieza añadida puede arrastrar.
- Cabeza sobre torso sobre piernas, coronilla cerca de los dos bloques, pies separados del suelo; brazos y piernas de seis piezas cada uno.
- El **test de la hitbox** mantiene `MODEL_HITBOX_SCALE` cubriendo toda la pose de reposo sin pasarse en más de 0.05 de la escala mínima que necesita el modelo.
- El tajo dobla los codos al cargar, los estira al golpear y flexiona las rodillas.
- Una guarda de código impide que las piezas se retrasen (`setTeleportDuration`/`setInterpolationDuration`/`setDisplayWidth`/`setDisplayHeight` a cero, configurados en un solo sitio) y exige que una recarga **adopte** las piezas que ya tiene en vez de crear un segundo cuerpo.

### `entities/LimbArticulationGuardTest` — Cada segmento exportado está articulado
- Guardia cruzada sobre los tres jefes vestidos: recorre **todas** las agrupaciones de extremidad del enum de piezas de cada modelo — no la lista escrita a mano que usan los tests de cada modelo — y le pregunta la respuesta al export. Donde está el hueco más grande entre dos piezas apiladas, ahí va una articulación, y el código tiene que plegar exactamente las piezas de debajo, ni una más ni una menos.
- Falla cuando una extremidad viene exportada en dos segmentos que el código nunca articula (media extremidad se quedaría rígida para siempre) y cuando el código pliega una extremidad que el export dejó de una pieza. También demuestra que al menos una extremidad *sí* quedó articulada por modelo, así que la guardia no puede pasar de forma vacua.
- Encontró un caso real mientras se escribía: el `TORSO_UPPER` de Kinger son dos piezas a 19 cm. Eso no es una articulación —el torso se inclina desde la cintura y no se dobla por la mitad— así que la guardia exige **ninguna** articulación a los grupos del cuerpo, y el hallazgo queda documentado en la clase en vez de taparlo con un umbral de hueco.

### `entities/KingerModelTest` — Geometría del modelo de Kinger
- Las quince piezas del traje quedan fijadas contra el **modelo exportado**: cada traslación coincide, el tronco y las piernas comparten un único eje Z, y cada mitad de pierna está apilada sobre su propio eje X, así que una pierna que gira no puede partirse de lado.
- `CENTER` es el **eje del torso** (el punto medio de las dos piezas del torso) y no la media de las quince anclas ni el punto medio del bbox, que los brazos arrastran 0.03 y 0.08 bloques hacia delante; el tronco, las piernas y la cabeza quedan sobre ese eje, y el recentrado nunca toca una altura.
- Cada pieza pertenece a un **grupo de extremidad**: las piezas de un grupo conservan sus distancias al girar, ninguna se desliza de lado ni se desprende de su articulación, y no hay dos piezas en el mismo sitio.
- Una extremidad tiene **dos articulaciones donde el export tiene dos segmentos y una donde no**: la espinilla se pliega en la rodilla que el export deja entre el muslo y la espinilla (comparada con el hueco mayor de la geometría de esa pierna), el muslo y la placa de la bota quedan rígidos, los brazos de una sola pieza de Kinger no exponen ninguna segunda articulación, y un jefe quieto (o cuyo balanceo está en la mitad delantera del paso) no pliega nada.
- El **test de la hitbox** mantiene `MODEL_HITBOX_SCALE` cubriendo toda la pose de reposo (0.5 de ancho, 1.975 de alto) sin alejarse más de 0.1 de la escala 0.94 que la geometría necesita — el literal `2.0` anterior duplicaba la caja en todas direcciones y se tragaba golpes al aire.
- Un segundo test de hitbox **recorre toda la forma de caminar**: un paso dobla la rodilla y lleva la espinilla más lejos del eje que la pose de reposo, así que cada pieza se comprueba en un ciclo completo de `walkSwing`, y **sin ningún margen de tolerancia**. El paso está calibrado para que incluso el más profundo deje la espinilla doblada sobre la caja de 0.5 del stand: un golpe que falla al stand no golpea nada, así que el presupuesto para «que la pierna parezca viva» es la caja y nada más.
- La etiqueta de cada pieza es única y lleva su propietario, así que una adopción no puede confundir dos piezas; una guardia de fuentes impide que las piezas vuelvan a retrasarse (`setTeleportDuration`/`setInterpolationDuration`/`setInterpolationDelay`/`setDisplayWidth`/`setDisplayHeight` a cero, configurados en un solo sitio), exige que un recargue **adopte** el traje que ya tiene en vez de crear un segundo superpuesto y que **reconstruya la barra de jefe** de ese jefe (con la salud virtual de la que se lee su progreso), y mantiene la animación pasando por `KingerModel.compose` en lugar de una transformación por pieza.
- La **pose de caminar** que dibuja la repetición se comprueba como esqueleto: cuatro extremidades —las rodillas de Kinger plegando en la articulación del export, sus brazos de una pieza rígidos y sin articulación— con ambos huesos conservando su longitud y el pivote fijo en cada fase, cada punto dentro de la caja de 0.5 del stand, y la rodilla plegando el pie detrás de la pierna recta y más cerca de la cadera. Un chequeo de fuentes mantiene al mob avanzando a `KingerModel.WALK_RATE`.

### `entities/handler/MobHandlerRecountTest` — Cooldown del tope de población
- `MobHandler.puedeRecontar` permite el recontado **por mundo** y respeta un cooldown de fallo independiente por mundo: `world` con cooldown hasta `160000` no re-cuenta en `159999` pero sí en `160000`; `world_nether` no se bloquea por el cooldown de `world`.

### `entities/EnderKnightWorldGuardTest` — Teleport del Caballero Ender
- `EnderKnight.sharesWorld` solo devuelve `true` si los mundos coinciden; rechaza identidad de mundo `null`. Garantiza que los cálculos de distancia del tira-tira solo ocurran dentro del mismo mundo.

### `entities/boss/attack/ChoreographyTest` — Cada ataque del Centinela, sin servidor
- Encuentra en las fuentes cada clase que extiende `ChoreographedAttack` (las 61) y la reproduce hasta el final sobre un `RecordingStage`: termina, da al menos 20 ticks que un jugador pueda leer, bloquea el cuerpo no menos de 15 ticks ni más de lo que dura, dibuja y suena, no deja objetos atrás y vuelve a su guardia (o a flotar). El stage rechaza una partícula lanzada sin los datos que necesita.
- Con jugadores en los puntos habituales de combate, cada ataque ofensivo golpea al menos a uno, así que un aviso que señala un sitio al que el golpe nunca llega se detecta.

### `entities/boss/SignatureMovesTest` — Los movimientos de firma de NIX y JackStar
- Cada fotograma de Cosecha de Sangre, Salto del Patíbulo, Condena, fork(), Lluvia Binaria y Stack Overflow es una rotación real, ningún codo ni rodilla se pliega más allá de `MscLimb.MAX_BEND`, ninguna extremidad gira más de 1.2 rad entre dos ticks y cada movimiento termina cerca de la pose de reposo.
- Los golpes coinciden con el cuerpo: brazos sobre la cabeza en el aire y abajo al aterrizar, el brazo de la condena arriba mientras dura la sentencia y abajo cuando caen las hojas, el brazo que lanza echado atrás antes de soltar y adelante después.

### `entities/boss/SentinelAttackPoolTest` — La rotación de ataques del Centinela
- Todo ataque registrado sale de algún pool o lo maneja el propio bucle de IA, y todo nombre de un pool es un ataque registrado, así que no se puede añadir un ataque que luego nunca se usa (`doombeam` y `rainoflances` estaban así).
- Cada pool aéreo tiene ataques suficientes para que un vuelo termine antes, una elección nunca repite uno de los últimos seis ataques mientras quede otro, y un pool más pequeño que el historial sigue dando un ataque.

### `entities/boss/BossWalkTest` — Cómo caminan los jefes
- `BossArena.nextFeetY`: el suelo llano sigue llano, un bloque de subida es un escalón, algo más alto es una pared, y un borde o una caída sin fondo se bajan poco a poco en vez de quedarse flotando; asentarse converge exactamente sobre el suelo.

### `entities/boss/JackStarBossTest` — Las fases de Jack Star
- Cada tramo de vida corresponde a su fase con los límites incluidos, el kernel panic es siempre la última fase, y cada grupo de extremidad tiene las piezas que esperan sus articulaciones.

### `entities/KingerMeleeTest` — El golpe cuerpo a cuerpo de Kinger
- El golpe entra en el punto más alto del swing, no en el tick en que empieza, solo alcanza a quien está delante, y su alcance es una esfera, no un cubo.

### `listener/bossdimension/BossFightGuardTest` — Comandos durante una pelea de jefe
- `/say`, `/me`, `/help`, `/?` y `/dimtp` siguen funcionando en plena pelea en cualquier mayúscula y con namespace, mientras que un comando que solo empieza como ellos (`/menu`, `/sayhi`, `/helpop`) se bloquea.

### `entities/boss/BossArenaGroundRecoveryTest` — Recuperación de suelo
- `findFloorY` devuelve la altura del piso, y **`NaN`** — no la `Y` propia del jefe — cuando el escaneo no alcanza nada. Esa distinción es el arreglo: antes "estar apoyado en el piso" y "no haber nada debajo" eran el mismo valor, así que un jefe en modo suelo dejaba de atacar para siempre.
- `getGroundY` conserva su comportamiento documentado de devolver la `Y` actual, fijado por test para que los dos comportamientos no vuelvan a confundirse.
- `ringOffsets` empieza en el origen, contiene cada desplazamiento del radio exactamente una vez y nunca retrocede hacia el origen: la columna usable más cercana debe ganar siempre.
- `findUsableColumn` prefiere la columna del propio jefe, camina hacia fuera si está vacía, respeta el radio de búsqueda y devuelve `null` cuando no hay nada usable, para que el llamante pueda probar la columna del objetivo y después el spawn del mundo.
- `findFloorY` siempre responde la cara superior de un bloque: a un jefe que flota medio bloque sobre el suelo se le da la altura del suelo, no su propia Y (eso dejaba al Centinela flotando sin atacar), un cuerpo hundido en un bloque se sube encima, y una losa se pisa a su propia altura.

### `utils/MscWorldPolicyTest` — Lista blanca de mundos
- Una lista **vacía (o ausente) significa todos los mundos**, que es lo que documenta `config.yml`. La implementación la trataba como una lista fija de cinco nombres, así que un servidor con un mundo de nombre propio no tenía conversiones — y su recuento periódico borraba cualquier criatura MSC que encontrara ahí.
- Una lista con contenido restringe, normalizando mayúsculas y espacios sobrantes.
- Los mundos propios del plugin (`boss_dimension`, `drakes_bosses`) siguen permitidos incluso con una lista explícita.

### `commands/CommandPermissionTest` — Regla de permisos de `/msc`
- Tener `commands.permission` basta para usar `/msc`, con OP o sin él.
- `commands.op-only: true` mantiene operativos a los operadores que no tienen el nodo; `false` hace que solo cuente el nodo.
- Un jugador normal sin nodo y sin OP queda rechazado.
- Un subcomando sin nodo configurado queda abierto para quien pasó la puerta principal, mientras que uno fijado en `commands.subcommand-permissions` necesita su nodo además de la principal.

### `entities/HeadSlimeImmunityTest` — Inmunidad de la gelatina
- La ventana de inmunidad es una fecha límite por jugador, así que comer una segunda gelatina la **extiende** en vez de que el removal programado anterior la corte antes, y nada sobrevive a la ventana tras un logout.
- Una ventana expirada se descarta al consultarla, y `clearAllImmunity()` se invoca desde `onDisable`.

### `entities/boss/SentinelPhaseTest` — Escalera de fases del Centinela
- La escalera **reproduce exactamente la cadena de comparaciones hardcodeada**: un barrido de fracciones de salud de −5% a 105% se compara contra la cadena `> 0.8 / > 0.6 / > 0.4 / > 0.2` que el jefe tenía en línea, más los bordes exactos (a 80% justos el jefe ya está en la fase 1), entradas sin sentido, y que la fase solo crece cuando la salud baja.
- `sanitizeThresholds` descarta umbrales fuera de `(0, 1]` y no finitos, ordena el resto de mayor a menor, colapsa duplicados (serían una fase de ancho cero), conserva el `1.0`, devuelve una lista inmutable, y recurre a los valores por defecto cuando una edición del config no deja nada usable.
- Los títulos generados de la barra de jefe se afirman **byte a byte** contra los cinco strings que tenía el switch, para las cinco fases por defecto y para una escalera reescalada de tres fases — incluyendo que una fase más allá del final no puede emitir cuadrados negativos.
- Los colores de la barra siguen a las fases, y una escalera más larga reusa el último color de la paleta en vez de caer a rojo.

### `entities/boss/SentinelHitboxTest` — El propio stand del Centinela de Obsidiana
- El Centinela no es un traje sobre un stand invisible: **es** el stand escalado, así que un único número es a la vez el tamaño del modelo y la caja que golpean los jugadores: `MODEL_HITBOX_SCALE` queda fijado en `7.5` (un guerrero de unos catorce bloques), dentro del rango que permite el acotado y lo bastante ancho para poder ser golpeado.
- La escala se lee de `armor-stand-boss.hitbox-scale` y se **acota**, nunca vuelve a ser un literal dentro de `trySpawn` — que el código la fije desde un `7.5` suelto es un fallo del test.
- Los dos números del pentagrama de llegada (su duración y su radio) son constantes con nombre que la llamada del sello pasa, así el tiempo y el tamaño de la arena no quedan enterrados en un punto de llamada.
- El stand por el que se golpea la pelea se configura en **exactamente un sitio**: brazos activados, sin placa base, sin gravedad, invulnerable desactivado, persistente, con nombre y etiqueta — se verifica que cada uno aparece una sola vez en el código, porque un segundo camino de spawn que olvidara uno produciría un jefe contra el que no se puede ganar la pelea.

### `utils/MscTextTest` — Paridad de nombres de items y mobs
- Cada helper que arma nombres de items, lore, frases de sabor y los pies `✦ … ✦` se serializa de vuelta con `LegacyComponentSerializer.legacySection()` y se compara con el string `ChatColor` exacto al que reemplazó, así la migración no puede mover un espacio, un código de color ni una negrita sin que se note.
- Cubre los cambios de color a mitad de línea (`rich`), las líneas vacías separadoras (`blank`), los nombres sin color (`plain`) y la validación de argumentos de `rich`.
- Fija la regla legacy de que **un código de color limpia la negrita**: un prefijo en negrita seguido de otro color queda en negrita solo en el prefijo, y por eso el nombre de Garou se arma como dos hermanos y no como padre decorado. Un hijo heredaría la negrita, y el test deja esa trampa a la vista.
- `plainText` es la contraparte que sirve para **comparar** un nombre en vez de mostrarlo, así que debe quitar todo código y devolver string vacío para una entidad sin nombre.

### `ConfigFilesGuardTest` — Contrato de recursos (`config.yml` / `plugin.yml`)
- Parsea ambos recursos con SnakeYAML, así una indentación rota o una sección perdida fallan en el build y no al arrancar el servidor.
- Escanea `src/main/java` buscando rutas de config entre comillas y falla si alguna no existe en `config.yml`. Nada hacía cumplir la promesa del encabezado ("all paths match the code"): el handler del Nullshear Edge leía cinco claves `items.nullshear-edge.*` que no estaban en el archivo, y dos claves del pasivo de Excalibur estaban documentadas pero hardcodeadas, cayendo ambas silenciosamente a los valores por defecto del código.
- Verifica que `plugin.yml` conserve el comando, el nodo `msc.admin` (el mismo que declara `commands.permission`), el nodo `msc.admin.bypass` que usan los handlers de la dimensión del jefe, y una línea `usage` que liste todos los subcomandos.
- Comprueba que los ajustes visibles para el jugador sigan sanos: los `phase-thresholds` del Centinela descienden dentro de `(0, 1]`, las duraciones de defensa duran al menos un tick, `boss-balance.despawn` tiene radios válidos y `delay-ticks` admite `0`, y cada mob conmutable conserva su flag `enabled`.
- Un autotest prueba que el escáner de literales reporta los literales con punto fuera de comentarios e ignora los que están dentro.
- Verifica que `commands.subcommand-permissions` exista como **mapa vacío** por defecto: la puerta documentada no debe desaparecer en silencio, y la config que se envía no debe restringir nada por sorpresa.
- Verifica que **todos** los jefes envíen la escala de hitbox que su test de geometría demuestra correcta (`kinger.hitbox-scale` 1.0, `nix-executioner.hitbox-scale` 1.9, `jackstar-architect.hitbox-scale` 1.2, `armor-stand-boss.hitbox-scale` 7.5 — el propio cuerpo del Centinela), dentro del rango 0.25–8 al que se acota un valor editado a mano — el ajuste y los tests de geometría deben coincidir de fábrica.

### `utils/SourceGuardsTest` — Reglas leídas del código
- **FLASH necesita color**: cada `spawnParticle(Particle.FLASH, …)` lleva un `Color`, porque desde 1.21 sin él lanza una excepción (un reinicio de JackStar lo hacía en cada golpe).
- **Ningún catch silencioso**: se quitan comentarios, strings y chars (conservando los números de línea) y cada `catch` de `src/main/java` tiene que hacer algo; lista de permitidos vacía y un mínimo de 40 catches para que el escaneo no pase en vacío.
- **Los nombres son Components**: ningún archivo vuelve a las APIs de nombre con String obsoletas (`setDisplayName`, `setLore`, `setItemName`, `setCustomName`, `getDisplayName`, `getCustomName`); se ignoran las coincidencias en comentarios, el escaneo cubre todo el código y una muestra con las seis APIs prueba que el detector las encuentra.

### `utils/MscLogTest` — Fallos reportados
- Los veintiún bloques `catch` que se tragaban su excepción (`catch (Exception ignored) { }`) ahora reportan por `utils/MscLog`; esta suite lo maneja con un `Handler` capturador y comprueba que de verdad se pide al logger del plugin que imprima.
- Un fallo tolerado se registra en `FINE` y uno accionable en `WARNING`, siempre con el contexto, el nombre simple de la excepción y su mensaje — un `NumberFormatException` conserva la entrada ofensiva.
- Una excepción sin mensaje igualmente se nombra (`java.lang.IllegalStateException`), una nula reporta `unknown error` en vez de lanzar, e `init(null)` conserva el logger anterior, así que el orden de arranque no puede silenciar el plugin.

### `utils/MscConfigMigrationTest` — Actualizaciones del config
- La regla de fusión es pura y se prueba: solo se reportan las rutas que le faltan al archivo, el valor que editó un operador nunca aparece como ausente, y una ruta va punteada por todas sus secciones mientras que una **lista sigue siendo hoja** (indexarla inventaría claves que no están en el archivo).
- Lee `config.yml` y exige que declare el mismo `config-version` que `MscConfigMigration.CONFIG_VERSION`, así una release no puede enviar un archivo con el que el código no está de acuerdo, ni un salto de versión que nunca ocurrió.
- Lee `MultiverseCreatures.onEnable` y demuestra que la migración corre **después de `saveDefaultConfig()` y antes de la primera lectura de `getConfig()`**: `saveDefaultConfig()` solo escribe el config cuando no existe, así que una clave añadida por una actualización se quedaría invisible en todos los servidores que ya existen.
- Lee `MSCCommand.handleReload` y demuestra que `/msc reload` relee el archivo, fusiona los defaults del jar y solo después recarga los handlers — un servidor que actualizó el plugin obtiene las claves nuevas con una recarga, no solo tras reiniciar.

### `utils/SchedulerHandleGuardTest` — Toda tarea en bucle se puede parar
- Guardia de fuentes: recorta comentarios, encuentra cada `.runTaskTimer(` / `.scheduleSyncRepeatingTask(` bajo `src/main/java` y exige que cada sitio sea una de dos formas — un runnable cuyo propio cuerpo llama a `cancel()`, o una tarea cuyo handle sobrevive al enunciado (una llamada sobre un nombre: `task`, `instance.flyTask`; o una asignación `x = new BukkitRunnable() { … }.runTaskTimer(…)`). Un suelo de 100 sitios mantiene el escaneo sobre todo el proyecto.
- El escáner sigue las llaves, no el orden de las líneas: empareja `new BukkitRunnable()` con su propia llave de cierre (incluido `org.bukkit.scheduler.BukkitRunnable`), así una tarea de un solo uso anidada en un bucle mayor ya no esconde el `cancel()` de ese bucle, y lee un receptor `instance.<campo>` a través de su declaración.
- Un segundo test demuestra que los handles no son decoración: todo handle de una tarea que no se autocancela tiene que cancelarse en algún punto del proyecto (`task.cancel()`, `instance.defenseTask.cancel()`), devolverse a quien lo pidió (`return task;` en los sellos que el jefe cancela antes) o entregarse a un nombre que a su vez se cancela (`instance.aiTask = ai;`).
- Escrita contra el código tal como estaba, encontró veinticuatro bucles sin dueño: uno por mob propio (Head Slime tenía dos), uno por jefe, el pasivo de Excalibur, las auras de objeto (Wirt's Lantern, Mantis Claws, Frost Heart, Obsidian Bastion) y el recuento de población — todos guardan ya su `BukkitTask` y se paran con el nuevo `stopTasks()`.
- Un tercer test mantiene el apagado honesto: `onDisable` tiene que llamar a `stopAll()`, `unloadBossDimension()` y a la parada de tickers, así un reload no puede dejar un bucle recorriendo estado que nadie lee.

### `entities/boss/seal/SealPlaneTest` — Dónde aterriza un sello
- El mapeo del plano es una isometría: un anillo llevado a XZ, XY o YZ conserva su radio, dos puntos cualesquiera conservan su distancia, un sello plano mantiene una altura y uno vertical mantiene un eje, y el desfase normal solo mueve el sello por su propia normal (XZ arriba, XY por Z, YZ por X). Esa es toda la diferencia entre un sello en el suelo y uno de pie en el aire.

### `entities/boss/seal/SealGeometryTest` — Las formas y sus reglas
- Un círculo conserva radio, número de muestras y espaciado; un pentagrama son cinco cuerdas que visitan cada vértice exactamente dos veces —el mensaje de fallo nombra la cuerda duplicada que el dibujo viejo tenía en su primera arista— y un triángulo reparte su presupuesto de muestras entre tres lados iguales. Un anillo estrellado zigzaguea doce veces entre el anillo exterior y el 55 % interior, cerrando sobre sí mismo, y una banda de runas mantiene cada punto entre sus dos radios y se reproduce desde un random con semilla.
- Las reglas con números quedan fijadas: los suelos de muestras (60 líneas / 220 anillo), cantidad y radio del aura, el anillo envolvente 1.24, la escala celestial vertical 1.3×, la columna del escudo que nunca colapsa por debajo de medio bloque, la espiral que se queda dentro, y las bandas documentadas de los vagabundeos del vórtice, el seísmo, lo divino y el pulso de la cruz.

### `entities/boss/seal/WingGeometryTest` — Las alas
- Cada pluma va de su hombro a una punta dentro de `length + reach`, las dos alas se reflejan exactamente (aleteo incluido), un cuarto de vuelta rota toda la nube con el stand, y un aleteo rota cada pluma rígidamente en vez de estirarla.
- Los perfiles se comprueban como datos: las alas ardientes son el par más largo, más lleno y más lento, y ningún perfil puede poner una constante a cero. La comprobación de NaN no es teórica —la última pluma evaluaba `Math.pow(sin(π + 0.1), 1.5)` sobre base negativa, que es NaN, así que la geometría recorta el seno antes de la potencia.

### `entities/boss/seal/SealOrchestrationGuardTest` — El listener sigue siendo un pincel
- El listener tiene exactamente un `.runTaskTimer(` (todos los sellos pasan por el bucle compartido), no contiene trigonometría, y el paquete puro no menciona ningún tipo del servidor: ni `org.bukkit`, ni `Particle`, ni `Location`.
- También falla si el listener deja de delegar (menos de 20 llamadas a la geometría) o vuelve a crecer por encima de 700 líneas — el archivo queda en 573 tras la división, desde 1088 y sin cambio de comportamiento.

## 🗃️ Dónde se corren

Los tests se ejecutan en la **fase `test` de Maven** (Surefire). No requieren servidor ni red: solo el JDK 21 y las dependencias del `pom.xml`.

También corren en CI: `.github/workflows/verify.yml` ejecuta `mvn verify` en cada push a `main` y en cada pull request. La publicación a Modrinth (`modrinth-publish.yml`) espera a ese job, porque su propio paso de compilación usa `-DskipTests`.

> Al tocar código relacionado con salud/jefes, estructuras de ritual o el modelo cinemático de NIX/Kinger, ejecuta la suite completa con `mvn test` para asegurarte de no introducir regresiones.