package sk.tuke.gamestudio.game.cuberoll.core;

public class Cube {
    private FaceColor top = FaceColor.NONE;
    private FaceColor bottom = FaceColor.NONE;
    private FaceColor north = FaceColor.NONE;
    private FaceColor south = FaceColor.NONE;
    private FaceColor west = FaceColor.NONE;
    private FaceColor east = FaceColor.NONE;

    public void roll(Direction direction) {
        switch (direction) {
            case NORTH -> rollNorth();
            case SOUTH -> rollSouth();
            case WEST -> rollWest();
            case EAST -> rollEast();
        }
    }

    private void rollNorth() {
        FaceColor originalTop = top;
        top = south;
        south = bottom;
        bottom = north;
        north = originalTop;
    }

    private void rollSouth() {
        FaceColor originalTop = top;
        top = north;
        north = bottom;
        bottom = south;
        south = originalTop;
    }

    private void rollWest() {
        FaceColor originalTop = top;
        top = east;
        east = bottom;
        bottom = west;
        west = originalTop;
    }

    private void rollEast() {
        FaceColor originalTop = top;
        top = west;
        west = bottom;
        bottom = east;
        east = originalTop;
    }

    public void paintBottom(FaceColor color) {
        bottom = color;
    }

    public void clearColors() {
        top = FaceColor.NONE;
        bottom = FaceColor.NONE;
        north = FaceColor.NONE;
        south = FaceColor.NONE;
        west = FaceColor.NONE;
        east = FaceColor.NONE;
    }

    public FaceColor getTop() {
        return top;
    }

    public FaceColor getBottom() {
        return bottom;
    }

    public FaceColor getNorth() {
        return north;
    }

    public FaceColor getSouth() {
        return south;
    }

    public FaceColor getWest() {
        return west;
    }

    public FaceColor getEast() {
        return east;
    }

    public String getOrientationSummary() {
        return String.format(
                "Top=%s Bottom=%s North=%s South=%s West=%s East=%s",
                top.getShortCode(),
                bottom.getShortCode(),
                north.getShortCode(),
                south.getShortCode(),
                west.getShortCode(),
                east.getShortCode()
        );
    }
}

/*
1. Switch + bridge / trapdoor
2. Fragile tile / single-use tile
3. Collectible prisms / stars before finish
4. Teleport pair
5. Ice / slippery tile
6. Conveyor arrows
7. Hidden finish / reveal switch
8. Pushable block / moving block
9. Target tiles / quota before finish
10. DarkCube enemy
11. Fall counter / score penalty
12. Undo last move

Switch + bridge / trapdoor
Є кнопка-перемикач на карті. Коли куб наступає на switch, десь на рівні з’являється або зникає міст/платформа. Наприклад, натиснув кнопку — відкрився шлях через яму.
Fragile tile / single-use tile
Це крихка плита. Куб може стати на неї тільки один раз. Після цього плита ламається або перетворюється на void. Якщо повернутися туди ще раз — куб впаде.
Collectible prisms / stars before finish
На рівні є зірки/кристали, які треба зібрати перед фінішем. Finish не спрацює, поки не зібрані всі потрібні предмети.
Teleport pair
Є дві телепорт-клітинки. Якщо куб стає на одну, він одразу переноситься на іншу. Наприклад, став на T1 — з’явився на T2.
Ice / slippery tile
Лід. Коли куб стає на таку плиту, він не зупиняється, а ковзає далі в тому ж напрямку, поки не виїде на звичайну плиту або не впаде.
Conveyor arrows
Конвеєрна плитка зі стрілкою. Якщо куб стає на неї, його автоматично рухає в напрямку стрілки. Наприклад, плитка > сама штовхає куб вправо.
Hidden finish / reveal switch
Фініш спочатку прихований або заблокований. Щоб він з’явився, треба натиснути switch або виконати умову. Наприклад, натиснув кнопку — замість звичайної плити з’явився finish.
Pushable block / moving block
На полі є блок, який можна штовхати кубом. Якщо куб рухається в сторону блока, блок пересувається на наступну клітинку. Можна використовувати його як міст або для натискання кнопок.
Target tiles / quota before finish
Є спеціальні target-клітинки, які треба активувати. Наприклад, потрібно наступити на 3 target-плити, і тільки після цього finish відкриється.
DarkCube enemy
Ворог-куб, який рухається по рівню або стоїть на певній клітинці. Якщо гравець торкається DarkCube — програш. Можна зробити, щоб він рухався після кожного ходу гравця.
Fall counter / score penalty
Падіння не одразу кінець гри, а просто штраф. Наприклад, якщо куб впав, він повертається на старт, але score зменшується або збільшується лічильник падінь.
Undo last move
Кнопка “Назад”. Гравець може скасувати останній хід і повернути куб, позицію, кольори та стан поля назад. Дуже корисно для puzzle-гри.





Головний файл механіки майже завжди:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

Додаткові файли, якщо додаєш новий тип клітинки:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
src/main/resources/static/style.css
src/main/resources/static/cuberoll-playcanvas.js
```

Після зміни Java-коду перезапусти сервер:

```bash
mvn spring-boot:run -Dspring-boot.run.main-class=sk.tuke.gamestudio.server.GameStudioServer
```

Після зміни CSS/JS онови браузер через:

```text
Ctrl + F5
```

**Важливо:** не вставляй усі механіки одразу. На сесії бери тільки той блок, який відповідає завданню.

---

## Які механіки я взяв як ідеї з інших ігор

У схожих rolling-cube / tile puzzle іграх часто зустрічаються такі механіки:

```text
Bloxorz             → switches, trapdoors/bridges, single-use/fragile tiles
Edge                → moving blocks, switches, collectible prisms, time/fall ranking, Darkcube enemy
Rollaround          → tiles that change after touch, quotas, switches, hidden exits, enemies, time limit
A Monster Expedition → pushable logs/blocks, bridges, reset/undo-style puzzle design
```

Нижче — адаптації цих механік під твою CubeRoll-архітектуру.

---

# 1. Switch + bridge / trapdoor

## Завдання

> Додай switch. Коли куб наступає на `s`, усі bridge-плити відкриваються або закриваються.

Символи рівня:

```text
s  switch
=  open bridge, по ньому можна ходити
_  closed bridge / hole, по ньому не можна ходити
```

---

## Крок 1 — `CellType.java`

Файл:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
```

Приблизно рядки `3–14`. Знайди останній enum-елемент:

```java
GATE_YELLOW('Y', FaceColor.YELLOW);
```

Заміни на:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
SWITCH('s'),
BRIDGE_OPEN('='),
BRIDGE_CLOSED('_');
```

---

## Крок 2 — `Field.java`, перший switch у constructor

Файл:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

У constructor, приблизно рядки `37–57`, знайди:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
default -> throw new IllegalArgumentException("Unsupported map symbol: " + symbol);
```

Між ними встав:

```java
case 's' -> board[row][column] = CellType.SWITCH;
case '=' -> board[row][column] = CellType.BRIDGE_OPEN;
case '_' -> board[row][column] = CellType.BRIDGE_CLOSED;
```

Має бути так:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
case 's' -> board[row][column] = CellType.SWITCH;
case '=' -> board[row][column] = CellType.BRIDGE_OPEN;
case '_' -> board[row][column] = CellType.BRIDGE_CLOSED;
default -> throw new IllegalArgumentException("Unsupported map symbol: " + symbol);
```

---

## Крок 3 — `Field.java`, другий switch у `loadBoardFromDefinition()`

У методі:

```java
private void loadBoardFromDefinition()
```

приблизно рядки `254–279`, знайди:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
default -> throw new IllegalArgumentException("Unsupported map symbol: " + symbol);
```

Між ними встав:

```java
case 's' -> board[row][column] = CellType.SWITCH;
case '=' -> board[row][column] = CellType.BRIDGE_OPEN;
case '_' -> board[row][column] = CellType.BRIDGE_CLOSED;
```

---

## Крок 4 — `Field.java`, closed bridge поводиться як void

У `moveWithOutcome()` знайди:

```java
CellType destination = isInside(attemptedRow, attemptedColumn)
        ? board[attemptedRow][attemptedColumn]
        : CellType.VOID;
```

Одразу після цього встав:

```java
if (destination == CellType.BRIDGE_CLOSED) {
    destination = CellType.VOID;
}
```

---

## Крок 5 — `Field.java`, дія switch

У `moveWithOutcome()` знайди цей блок:

```java
if (destination.isPainter()) {
```

Заміни тільки початок chain на це:

```java
if (destination == CellType.SWITCH) {
    toggleBridges();
    lastMessage = "Switch pressed: bridges toggled.";
} else if (destination.isPainter()) {
```

Інший код нижче не чіпай.

---

## Крок 6 — `Field.java`, helper `toggleBridges()`

Перед методом:

```java
public void exit() {
```

встав:

```java
private void toggleBridges() {
    for (int row = 0; row < rowCount; row++) {
        for (int column = 0; column < columnCount; column++) {
            if (board[row][column] == CellType.BRIDGE_OPEN) {
                board[row][column] = CellType.BRIDGE_CLOSED;
            } else if (board[row][column] == CellType.BRIDGE_CLOSED) {
                board[row][column] = CellType.BRIDGE_OPEN;
            }
        }
    }
}
```

---

## Крок 7 — `CubeRollController.java`, 2D відображення

Файл:

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

У методі:

```java
private BoardCell boardCellFor(Field field, int row, int column)
```

знайди:

```java
case 'Y' -> new BoardCell("cell gate yellow", "Y", "yellow gate");
default -> new BoardCell("cell floor", String.valueOf(symbol), "floor");
```

Між ними встав:

```java
case 's' -> new BoardCell("cell switch", "S", "switch");
case '=' -> new BoardCell("cell bridge open", "", "open bridge");
case '_' -> new BoardCell("cell bridge closed", "", "closed bridge");
```

---

## Крок 8 — `style.css`, 2D кольори

Файл:

```text
src/main/resources/static/style.css
```

У кінець файлу встав:

```css
.switch {
    background: linear-gradient(135deg, #fde68a, #f59e0b);
    color: #422006;
    box-shadow: 0 0 14px rgba(245, 158, 11, 0.45);
}

.bridge.open {
    background: linear-gradient(135deg, #64748b, #94a3b8);
    border-color: #fbbf24;
}

.bridge.closed {
    background: #020617;
    border-color: #334155;
}
```

---

## Крок 9 — `cuberoll-playcanvas.js`, 3D closed bridge не малювати

Файл:

```text
src/main/resources/static/cuberoll-playcanvas.js
```

У `BoardRenderer.createCell(...)` знайди:

```js
if (cellType === 'VOID') {
    return;
}
```

Заміни на:

```js
if (cellType === 'VOID' || cellType === 'BRIDGE_CLOSED') {
    return;
}
```

Цього достатньо, щоб `_` виглядав як діра, а `=` як звичайна плита.

---

## Тестовий рівень для `Levels.java`

Можеш тимчасово замінити перший рівень на:

```java
new LevelDefinition(
        "Switch Bridge",
        "#########",
        "#S..s..F#",
        "###_#####",
        "###=#####",
        "#########"
)
```

---

# 2. Fragile tile / single-use tile

## Завдання

> Додай fragile tile. Коли куб наступив на `f`, плитка зламається після того, як куб з неї піде.

Символ рівня:

```text
f  fragile tile
```

---

## Крок 1 — `CellType.java`

Знайди:

```java
GATE_YELLOW('Y', FaceColor.YELLOW);
```

Заміни на:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
FRAGILE('f');
```

Якщо в тебе вже додані інші механіки, просто додай `FRAGILE('f')` перед останньою крапкою з комою.

---

## Крок 2 — `Field.java`, поля

Після:

```java
private FaceColor activeColor;
```

встав:

```java
private int standingFragileRow = -1;
private int standingFragileColumn = -1;
```

---

## Крок 3 — `Field.java`, парсинг символу

У двох switch-блоках `Field.java` додай:

```java
case 'f' -> board[row][column] = CellType.FRAGILE;
```

Додай перед `default`.

---

## Крок 4 — `reset()`

У методі:

```java
public void reset()
```

після:

```java
this.activeColor = FaceColor.NONE;
```

встав:

```java
this.standingFragileRow = -1;
this.standingFragileColumn = -1;
```

---

## Крок 5 — `moveWithOutcome()`, зламати стару fragile-плиту після руху

У `moveWithOutcome()` знайди:

```java
playerRow = attemptedRow;
playerColumn = attemptedColumn;

java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
```

Заміни на:

```java
int oldFragileRow = standingFragileRow;
int oldFragileColumn = standingFragileColumn;

playerRow = attemptedRow;
playerColumn = attemptedColumn;

if (oldFragileRow >= 0 && (oldFragileRow != playerRow || oldFragileColumn != playerColumn)) {
    board[oldFragileRow][oldFragileColumn] = CellType.VOID;
}
standingFragileRow = -1;
standingFragileColumn = -1;

java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
```

---

## Крок 6 — `moveWithOutcome()`, reaction на fragile

Знайди:

```java
if (destination.isPainter()) {
```

Заміни на:

```java
if (destination == CellType.FRAGILE) {
    standingFragileRow = playerRow;
    standingFragileColumn = playerColumn;
    lastMessage = "Fragile tile will break when you leave it.";
} else if (destination.isPainter()) {
```

---

## Крок 7 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` додай перед `default`:

```java
case 'f' -> new BoardCell("cell fragile", "f", "fragile tile");
```

`style.css`, в кінець файлу додай:

```css
.fragile {
    background: linear-gradient(135deg, #fef3c7, #f59e0b);
    color: #451a03;
    border-style: dashed;
}
```

---

## Крок 8 — 3D мінімально

У 3D `FRAGILE` без JS-змін буде виглядати як звичайна плита, але механіка працює.

Щоб візуально відрізнялась, у `cuberoll-playcanvas.js` в `MaterialFactory.build()` після `floorInset` додай:

```js
this.materials.fragile = this.standard('fragile-tile', '#f59e0b', {
    emissiveHex: '#fbbf24',
    emissiveIntensity: 0.22,
    shininess: 80
});
```

Потім у `createCell(...)`, після створення `actor.top`, додай:

```js
if (cellType === 'FRAGILE') {
    setEntityMaterial(actor.top, this.materials.materials.fragile);
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Fragile Tiles",
        "########",
        "#S.f..F#",
        "########"
)
```

---

# 3. Collectible prisms / stars before finish

## Завдання

> Додай collectibles. Finish заблокований, поки гравець не збере всі `*`.

Символ:

```text
*  prism / star collectible
```

---

## Крок 1 — `CellType.java`

Знайди:

```java
GATE_YELLOW('Y', FaceColor.YELLOW);
```

Заміни на:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
PRISM('*');
```

---

## Крок 2 — `Field.java`, поле

Після:

```java
private int remainingPainters;
```

встав:

```java
private int remainingPrisms;
```

---

## Крок 3 — `Field.java`, парсинг символу

У constructor switch додай перед `default`:

```java
case '*' -> board[row][column] = CellType.PRISM;
```

У `loadBoardFromDefinition()` зроби так:

Знайди:

```java
int painterCount = 0;
```

Після нього встав:

```java
int prismCount = 0;
```

У switch перед `default` додай:

```java
case '*' -> {
    board[row][column] = CellType.PRISM;
    prismCount++;
}
```

Внизу методу знайди:

```java
remainingPainters = painterCount;
```

Після нього встав:

```java
remainingPrisms = prismCount;
```

---

## Крок 4 — `moveWithOutcome()`, finish locked by prisms

Знайди блок:

```java
if (destination == CellType.FINISH
        && definition.isFinishRequiresAllPainters()
        && remainingPainters > 0) {
```

Перед цим блоком встав:

```java
if (destination == CellType.FINISH && remainingPrisms > 0) {
    lastMessage = "Finish is locked: collect all prisms first (remaining: " + remainingPrisms + ").";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            cube.getBottom(),
            false,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.FINISH_LOCKED)
    );
}
```

---

## Крок 5 — `moveWithOutcome()`, збір prism

Знайди:

```java
if (destination.isPainter()) {
```

Заміни на:

```java
if (destination == CellType.PRISM) {
    remainingPrisms--;
    board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Prism collected. Remaining: " + remainingPrisms + ".";
} else if (destination.isPainter()) {
```

---

## Крок 6 — getter

Після getter-а:

```java
public int getRemainingPainters() {
    return remainingPainters;
}
```

встав:

```java
public int getRemainingPrisms() {
    return remainingPrisms;
}
```

---

## Крок 7 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default` додай:

```java
case '*' -> new BoardCell("cell prism", "★", "prism");
```

`style.css`, у кінець:

```css
.prism {
    background: radial-gradient(circle, #fef08a, #f59e0b);
    color: #422006;
    box-shadow: 0 0 18px rgba(250, 204, 21, 0.75);
}
```

---

## Крок 8 — 3D мінімально

У 3D без JS-змін `PRISM` буде звичайною плитою. Для простого візуалу:

`cuberoll-playcanvas.js`, у `MaterialFactory.build()` додай:

```js
this.materials.prism = this.standard('prism-star', '#facc15', {
    emissiveHex: '#fde68a',
    emissiveIntensity: 1.45,
    shininess: 100
});
```

У `BoardRenderer.createCell(...)` знайди chain:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType.startsWith('PAINTER_')) {
```

Заміни на:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType === 'PRISM') {
    const prism = createBoxEntity(this.app, {
        name: `prism-${row}-${column}`,
        parent: this.root,
        position: new pc.Vec3(position.x, TILE_HEIGHT * 1.18, position.z),
        scale: new pc.Vec3(TILE_SIZE * 0.34, TILE_SIZE * 0.34, TILE_SIZE * 0.34),
        material: this.materials.materials.prism,
        castShadows: true
    });
    actor.parts.push(prism);
} else if (cellType.startsWith('PAINTER_')) {
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Collect Prisms",
        "#########",
        "#S.*..F#",
        "#..*...#",
        "#########"
)
```

---

# 4. Teleport pair

## Завдання

> Додай teleport. Коли куб наступає на `@`, він переноситься на інший `@`.

Символ:

```text
@  teleport
```

---

## Крок 1 — `CellType.java`

Додай enum перед останньою `;`:

```java
TELEPORT('@')
```

Наприклад:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
TELEPORT('@');
```

---

## Крок 2 — `Field.java`, парсинг

У двох switch-блоках додай перед `default`:

```java
case '@' -> board[row][column] = CellType.TELEPORT;
```

---

## Крок 3 — `Field.java`, reaction

У `moveWithOutcome()` знайди:

```java
if (destination.isPainter()) {
```

Заміни на:

```java
if (destination == CellType.TELEPORT) {
    int[] exit = findOtherTeleport(playerRow, playerColumn);
    if (exit != null) {
        playerRow = exit[0];
        playerColumn = exit[1];
        lastMessage = "Teleported to the paired portal.";
    } else {
        lastMessage = "Teleport has no pair.";
    }
} else if (destination.isPainter()) {
```

---

## Крок 4 — `Field.java`, helper

Перед:

```java
public void exit() {
```

встав:

```java
private int[] findOtherTeleport(int currentRow, int currentColumn) {
    for (int row = 0; row < rowCount; row++) {
        for (int column = 0; column < columnCount; column++) {
            if (board[row][column] == CellType.TELEPORT
                    && (row != currentRow || column != currentColumn)) {
                return new int[] { row, column };
            }
        }
    }
    return null;
}
```

---

## Крок 5 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case '@' -> new BoardCell("cell teleport", "@", "teleport");
```

`style.css`, у кінець:

```css
.teleport {
    background: radial-gradient(circle, #c084fc, #7c3aed);
    color: #f5f3ff;
    box-shadow: 0 0 18px rgba(168, 85, 247, 0.75);
}
```

---

## Крок 6 — 3D мінімально

Без JS-змін teleport буде як звичайна плита. Для простого візуалу у `cuberoll-playcanvas.js`:

У `MaterialFactory.build()` додай:

```js
this.materials.teleport = this.standard('teleport-pad', '#8b5cf6', {
    emissiveHex: '#c084fc',
    emissiveIntensity: 1.25,
    shininess: 96
});
```

У `BoardRenderer.createCell(...)`, після `actor.top` додай:

```js
if (cellType === 'TELEPORT') {
    setEntityMaterial(actor.top, this.materials.materials.teleport);
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Teleport",
        "##########",
        "#S..@####",
        "####.####",
        "####@..F#",
        "##########"
)
```

---

# 5. Ice tile / slippery tile

## Завдання

> Додай ice. Коли куб наступає на `i`, він автоматично ковзає вперед по floor/ice.

Символ:

```text
i  ice
```

Цей варіант спеціально простий: куб ковзає тільки через `FLOOR` і `ICE`, але зупиняється перед gate/painter/finish/void.

---

## Крок 1 — `CellType.java`

Додай перед останньою `;`:

```java
ICE('i')
```

Наприклад:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
ICE('i');
```

---

## Крок 2 — `Field.java`, парсинг

У двох switch-блоках додай перед `default`:

```java
case 'i' -> board[row][column] = CellType.ICE;
```

---

## Крок 3 — `moveWithOutcome()`, sliding logic

У `moveWithOutcome()` знайди:

```java
java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
effects.add(MoveEffect.ROLLED);
effects.add(MoveEffect.MOVED);

if (destination.isPainter()) {
```

Заміни на:

```java
java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
effects.add(MoveEffect.ROLLED);
effects.add(MoveEffect.MOVED);

boolean slipped = false;
while (destination == CellType.ICE) {
    int nextRow = playerRow + direction.getRowDelta();
    int nextColumn = playerColumn + direction.getColumnDelta();
    if (!isInside(nextRow, nextColumn)) {
        break;
    }
    CellType next = board[nextRow][nextColumn];
    if (next != CellType.FLOOR && next != CellType.ICE) {
        break;
    }

    cube.roll(direction);
    moveCount++;
    playerRow = nextRow;
    playerColumn = nextColumn;
    destination = next;
    bottomFaceAfterRoll = cube.getBottom();
    slipped = true;
    effects.add(MoveEffect.ROLLED);
    effects.add(MoveEffect.MOVED);
}

if (destination == CellType.ICE) {
    lastMessage = "The cube stopped on ice.";
} else if (destination.isPainter()) {
```

Тепер унизу цього same `if/else` chain знайди фінальний `else`:

```java
} else {
    lastMessage = "Move the painted face to the bottom when stepping onto a matching gate.";
}
```

Заміни на:

```java
} else {
    lastMessage = slipped
            ? "The cube slid on ice."
            : "Move the painted face to the bottom when stepping onto a matching gate.";
}
```

---

## Крок 4 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case 'i' -> new BoardCell("cell ice", "i", "ice");
```

`style.css`, у кінець:

```css
.ice {
    background: linear-gradient(135deg, #cffafe, #38bdf8);
    color: #083344;
    box-shadow: 0 0 16px rgba(56, 189, 248, 0.55);
}
```

---

## Крок 5 — 3D візуал

У `cuberoll-playcanvas.js`, `MaterialFactory.build()` додай:

```js
this.materials.ice = this.standard('ice-tile', '#67e8f9', {
    emissiveHex: '#22d3ee',
    emissiveIntensity: 0.35,
    opacity: 0.82,
    shininess: 100
});
```

У `BoardRenderer.createCell(...)`, після створення `actor.top`, додай:

```js
if (cellType === 'ICE') {
    setEntityMaterial(actor.top, this.materials.materials.ice);
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Ice Slide",
        "###########",
        "#S.iii..F#",
        "###########"
)
```

---

# 6. Conveyor arrows

## Завдання

> Додай conveyor. Якщо куб наступає на стрілку, його автоматично штовхає в напрямку стрілки.

Символи:

```text
^  conveyor north
v  conveyor south
<  conveyor west
>  conveyor east
```

Цей варіант штовхає тільки через `FLOOR` і conveyors. Перед gate/painter/finish/void куб зупиняється.

---

## Крок 1 — `CellType.java`

Додай перед останньою `;`:

```java
CONVEYOR_NORTH('^'),
CONVEYOR_SOUTH('v'),
CONVEYOR_WEST('<'),
CONVEYOR_EAST('>')
```

Наприклад:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
CONVEYOR_NORTH('^'),
CONVEYOR_SOUTH('v'),
CONVEYOR_WEST('<'),
CONVEYOR_EAST('>');
```

---

## Крок 2 — `Field.java`, парсинг

У двох switch-блоках додай перед `default`:

```java
case '^' -> board[row][column] = CellType.CONVEYOR_NORTH;
case 'v' -> board[row][column] = CellType.CONVEYOR_SOUTH;
case '<' -> board[row][column] = CellType.CONVEYOR_WEST;
case '>' -> board[row][column] = CellType.CONVEYOR_EAST;
```

---

## Крок 3 — `Field.java`, helper для direction

Перед:

```java
public void exit() {
```

встав:

```java
private Direction conveyorDirection(CellType cellType) {
    return switch (cellType) {
        case CONVEYOR_NORTH -> Direction.NORTH;
        case CONVEYOR_SOUTH -> Direction.SOUTH;
        case CONVEYOR_WEST -> Direction.WEST;
        case CONVEYOR_EAST -> Direction.EAST;
        default -> null;
    };
}
```

---

## Крок 4 — `moveWithOutcome()`, conveyor logic

Знайди:

```java
java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
effects.add(MoveEffect.ROLLED);
effects.add(MoveEffect.MOVED);

if (destination.isPainter()) {
```

Заміни на:

```java
java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
effects.add(MoveEffect.ROLLED);
effects.add(MoveEffect.MOVED);

int conveyorSteps = 0;
Direction pushDirection;
while ((pushDirection = conveyorDirection(destination)) != null && conveyorSteps < 8) {
    int nextRow = playerRow + pushDirection.getRowDelta();
    int nextColumn = playerColumn + pushDirection.getColumnDelta();
    if (!isInside(nextRow, nextColumn)) {
        break;
    }

    CellType next = board[nextRow][nextColumn];
    boolean nextIsSafeForAutoMove = next == CellType.FLOOR || conveyorDirection(next) != null;
    if (!nextIsSafeForAutoMove) {
        break;
    }

    cube.roll(pushDirection);
    moveCount++;
    playerRow = nextRow;
    playerColumn = nextColumn;
    destination = next;
    bottomFaceAfterRoll = cube.getBottom();
    conveyorSteps++;
    effects.add(MoveEffect.ROLLED);
    effects.add(MoveEffect.MOVED);
}

if (conveyorDirection(destination) != null || conveyorSteps > 0) {
    lastMessage = "Conveyor moved the cube.";
} else if (destination.isPainter()) {
```

---

## Крок 5 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case '^' -> new BoardCell("cell conveyor", "↑", "conveyor north");
case 'v' -> new BoardCell("cell conveyor", "↓", "conveyor south");
case '<' -> new BoardCell("cell conveyor", "←", "conveyor west");
case '>' -> new BoardCell("cell conveyor", "→", "conveyor east");
```

`style.css`, у кінець:

```css
.conveyor {
    background: linear-gradient(135deg, #bae6fd, #2563eb);
    color: #eff6ff;
    font-weight: 900;
    box-shadow: 0 0 16px rgba(37, 99, 235, 0.55);
}
```

---

## Крок 6 — 3D візуал

У `cuberoll-playcanvas.js`, `MaterialFactory.build()` додай:

```js
this.materials.conveyor = this.standard('conveyor-tile', '#2563eb', {
    emissiveHex: '#60a5fa',
    emissiveIntensity: 0.5,
    shininess: 88
});
```

У `BoardRenderer.createCell(...)`, після `actor.top` додай:

```js
if (cellType.startsWith('CONVEYOR_')) {
    setEntityMaterial(actor.top, this.materials.materials.conveyor);
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Conveyor",
        "##########",
        "#S.>>>.F#",
        "##########"
)
```

---

# 7. Hidden finish / reveal switch

## Завдання

> Додай прихований finish. Він стає справжнім finish тільки після натискання reveal-switch.

Символи:

```text
e  reveal switch
h  hidden finish
```

---

## Крок 1 — `CellType.java`

Додай перед останньою `;`:

```java
REVEAL_SWITCH('e'),
HIDDEN_FINISH('h')
```

Наприклад:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
REVEAL_SWITCH('e'),
HIDDEN_FINISH('h');
```

---

## Крок 2 — `Field.java`, constructor switch

У першому switch додай перед `default`:

```java
case 'e' -> board[row][column] = CellType.REVEAL_SWITCH;
case 'h' -> {
    board[row][column] = CellType.HIDDEN_FINISH;
    finishFound = true;
}
```

`finishFound = true` потрібен, щоб рівень без звичайного `F` не падав з помилкою.

---

## Крок 3 — `Field.java`, `loadBoardFromDefinition()` switch

У другому switch додай перед `default`:

```java
case 'e' -> board[row][column] = CellType.REVEAL_SWITCH;
case 'h' -> board[row][column] = CellType.HIDDEN_FINISH;
```

---

## Крок 4 — `moveWithOutcome()`, reaction

Знайди:

```java
if (destination.isPainter()) {
```

Заміни на:

```java
if (destination == CellType.REVEAL_SWITCH) {
    revealHiddenFinishes();
    board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Hidden finish revealed.";
} else if (destination == CellType.HIDDEN_FINISH) {
    lastMessage = "This finish is hidden. Find the reveal switch first.";
} else if (destination.isPainter()) {
```

---

## Крок 5 — helper `revealHiddenFinishes()`

Перед:

```java
public void exit() {
```

встав:

```java
private void revealHiddenFinishes() {
    for (int row = 0; row < rowCount; row++) {
        for (int column = 0; column < columnCount; column++) {
            if (board[row][column] == CellType.HIDDEN_FINISH) {
                board[row][column] = CellType.FINISH;
            }
        }
    }
}
```

---

## Крок 6 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case 'e' -> new BoardCell("cell reveal", "!", "reveal switch");
case 'h' -> new BoardCell("cell hidden-finish", "?", "hidden finish");
```

`style.css`, у кінець:

```css
.reveal {
    background: linear-gradient(135deg, #f0abfc, #a855f7);
    color: #2e1065;
}

.hidden-finish {
    background: linear-gradient(135deg, #334155, #0f172a);
    color: #e2e8f0;
    border-style: dashed;
}
```

---

## Крок 7 — 3D мінімально

У 3D ці клітинки без JS-змін будуть виглядати як floor. Механіка працює.

---

## Тестовий рівень

```java
new LevelDefinition(
        "Hidden Finish",
        "#########",
        "#S..e..#",
        "#.....h#",
        "#########"
)
```

---

# 8. Pushable block / moving block

## Завдання

> Додай pushable block. Якщо перед кубом `X`, куб може штовхнути його на наступну клітинку.

Символ:

```text
X  pushable block
```

Правила цього варіанту:

```text
- якщо за блоком FLOOR, блок пересувається туди
- якщо за блоком VOID всередині карти, блок заповнює яму і створює FLOOR
- якщо за блоком щось інше, хід блокується
```

---

## Крок 1 — `CellType.java`

Додай перед останньою `;`:

```java
BOX('X')
```

Наприклад:

```java
GATE_YELLOW('Y', FaceColor.YELLOW),
BOX('X');
```

---

## Крок 2 — `Field.java`, парсинг

У двох switch-блоках додай перед `default`:

```java
case 'X' -> board[row][column] = CellType.BOX;
```

---

## Крок 3 — `moveWithOutcome()`, змінна `pushedBox`

У `moveWithOutcome()` знайди:

```java
CellType destination = isInside(attemptedRow, attemptedColumn)
        ? board[attemptedRow][attemptedColumn]
        : CellType.VOID;
```

Після цього встав:

```java
boolean pushedBox = false;
```

---

## Крок 4 — `moveWithOutcome()`, push logic

Після `boolean pushedBox = false;` встав:

```java
if (destination == CellType.BOX) {
    int boxTargetRow = attemptedRow + direction.getRowDelta();
    int boxTargetColumn = attemptedColumn + direction.getColumnDelta();

    if (!isInside(boxTargetRow, boxTargetColumn)) {
        lastMessage = "Box cannot be pushed outside the board.";
        return new MoveOutcome(
                direction,
                fromRow,
                fromColumn,
                attemptedRow,
                attemptedColumn,
                playerRow,
                playerColumn,
                destination,
                cube.getBottom(),
                false,
                false,
                stateBefore,
                state,
                lastMessage,
                java.util.List.of()
        );
    }

    CellType boxTarget = board[boxTargetRow][boxTargetColumn];
    if (boxTarget == CellType.FLOOR) {
        board[boxTargetRow][boxTargetColumn] = CellType.BOX;
        board[attemptedRow][attemptedColumn] = CellType.FLOOR;
        destination = CellType.FLOOR;
        pushedBox = true;
    } else if (boxTarget == CellType.VOID) {
        board[boxTargetRow][boxTargetColumn] = CellType.FLOOR;
        board[attemptedRow][attemptedColumn] = CellType.FLOOR;
        destination = CellType.FLOOR;
        pushedBox = true;
    } else {
        lastMessage = "Box is blocked.";
        return new MoveOutcome(
                direction,
                fromRow,
                fromColumn,
                attemptedRow,
                attemptedColumn,
                playerRow,
                playerColumn,
                destination,
                cube.getBottom(),
                false,
                false,
                stateBefore,
                state,
                lastMessage,
                java.util.List.of()
        );
    }
}
```

---

## Крок 5 — `moveWithOutcome()`, повідомлення

У кінці chain знайди:

```java
} else {
    lastMessage = "Move the painted face to the bottom when stepping onto a matching gate.";
}
```

Заміни на:

```java
} else {
    lastMessage = pushedBox
            ? "Box pushed."
            : "Move the painted face to the bottom when stepping onto a matching gate.";
}
```

---

## Крок 6 — 2D відображення

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case 'X' -> new BoardCell("cell box", "X", "pushable box");
```

`style.css`, у кінець:

```css
.box {
    background: linear-gradient(135deg, #92400e, #f59e0b);
    color: #fff7ed;
    box-shadow: 0 0 14px rgba(245, 158, 11, 0.45);
}
```

---

## Крок 7 — 3D візуал

У `cuberoll-playcanvas.js`, `MaterialFactory.build()` додай:

```js
this.materials.box = this.standard('pushable-box', '#b45309', {
    emissiveHex: '#f59e0b',
    emissiveIntensity: 0.18,
    shininess: 60
});
```

У `BoardRenderer.createCell(...)`, у chain після `FINISH` додай:

```js
} else if (cellType === 'BOX') {
    const box = createBoxEntity(this.app, {
        name: `box-${row}-${column}`,
        parent: this.root,
        position: new pc.Vec3(position.x, TILE_HEIGHT * 1.7, position.z),
        scale: new pc.Vec3(TILE_SIZE * 0.60, TILE_SIZE * 0.60, TILE_SIZE * 0.60),
        material: this.materials.materials.box,
        castShadows: true,
        receiveShadows: true
    });
    actor.parts.push(box);
```

Повний шматок має виглядати приблизно так:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType === 'BOX') {
    const box = createBoxEntity(this.app, {
        name: `box-${row}-${column}`,
        parent: this.root,
        position: new pc.Vec3(position.x, TILE_HEIGHT * 1.7, position.z),
        scale: new pc.Vec3(TILE_SIZE * 0.60, TILE_SIZE * 0.60, TILE_SIZE * 0.60),
        material: this.materials.materials.box,
        castShadows: true,
        receiveShadows: true
    });
    actor.parts.push(box);
} else if (cellType.startsWith('PAINTER_')) {
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Push Box",
        "##########",
        "#S.X#..F#",
        "#...#...#",
        "##########"
)
```

---

# 9. Target tiles / quota before finish

## Завдання

> Додай target tiles. Гравець має активувати всі `t`, щоб finish відкрився.

Це схоже на Rollaround-style quota: треба торкнутися певних плит.

Символ:

```text
t  target tile
```

---

## Крок 1 — `CellType.java`

Додай перед останньою `;`:

```java
TARGET('t')
```

---

## Крок 2 — `Field.java`, поле

Після:

```java
private int remainingPainters;
```

встав:

```java
private int remainingTargets;
```

---

## Крок 3 — парсинг

Constructor switch:

```java
case 't' -> board[row][column] = CellType.TARGET;
```

`loadBoardFromDefinition()`:

після:

```java
int painterCount = 0;
```

встав:

```java
int targetCount = 0;
```

У switch перед `default`:

```java
case 't' -> {
    board[row][column] = CellType.TARGET;
    targetCount++;
}
```

Внизу після:

```java
remainingPainters = painterCount;
```

встав:

```java
remainingTargets = targetCount;
```

---

## Крок 4 — finish lock

У `moveWithOutcome()` перед існуючим finish-lock для painters встав:

```java
if (destination == CellType.FINISH && remainingTargets > 0) {
    lastMessage = "Finish is locked: activate all target tiles first (remaining: " + remainingTargets + ").";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            cube.getBottom(),
            false,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.FINISH_LOCKED)
    );
}
```

---

## Крок 5 — stepping on target

Знайди:

```java
if (destination.isPainter()) {
```

Заміни на:

```java
if (destination == CellType.TARGET) {
    remainingTargets--;
    board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Target activated. Remaining: " + remainingTargets + ".";
} else if (destination.isPainter()) {
```

---

## Крок 6 — getter

Після:

```java
public int getRemainingPainters() {
    return remainingPainters;
}
```

встав:

```java
public int getRemainingTargets() {
    return remainingTargets;
}
```

---

## Крок 7 — 2D visual

`CubeRollController.java`, у `boardCellFor(...)`:

```java
case 't' -> new BoardCell("cell target", "t", "target tile");
```

`style.css`:

```css
.target {
    background: radial-gradient(circle, #f9a8d4, #be185d);
    color: #fff1f2;
    box-shadow: 0 0 16px rgba(190, 24, 93, 0.55);
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "Targets",
        "#########",
        "#S.t..F#",
        "#..t...#",
        "#########"
)
```

---

# 10. DarkCube enemy

## Завдання

> Додай ворога DarkCube. Він рухається на 1 клітинку після кожного твого ходу. Якщо торкнувся тебе — програш.

Символ:

```text
D  DarkCube start position
```

Цей варіант працює у Java core і 2D. Для 3D нижче є короткий optional visual.

---

## Крок 1 — `Field.java`, поля

Після:

```java
private FaceColor activeColor;
```

встав:

```java
private int enemyRow = -1;
private int enemyColumn = -1;
```

---

## Крок 2 — constructor switch

У першому switch перед `default` додай:

```java
case 'D' -> board[row][column] = CellType.FLOOR;
```

Тут ми просто дозволяємо символ `D`. Реальну позицію enemy будемо ставити в `loadBoardFromDefinition()`.

---

## Крок 3 — `loadBoardFromDefinition()`

На початку методу після:

```java
int painterCount = 0;
```

встав:

```java
enemyRow = -1;
enemyColumn = -1;
```

У switch перед `default` додай:

```java
case 'D' -> {
    board[row][column] = CellType.FLOOR;
    enemyRow = row;
    enemyColumn = column;
}
```

---

## Крок 4 — якщо гравець іде прямо на enemy

У `moveWithOutcome()` після визначення `destination` встав:

```java
if (attemptedRow == enemyRow && attemptedColumn == enemyColumn) {
    cube.roll(direction);
    moveCount++;
    state = GameState.FAILED;
    lastMessage = "Failed: DarkCube caught you.";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            cube.getBottom(),
            true,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.ROLLED)
    );
}
```

---

## Крок 5 — рух enemy після ходу

У `moveWithOutcome()` перед фінальним:

```java
return new MoveOutcome(
```

встав:

```java
if (state == GameState.PLAYING) {
    moveDarkCube();
}
```

---

## Крок 6 — helpers enemy

Перед:

```java
public void exit() {
```

встав:

```java
private void moveDarkCube() {
    if (enemyRow < 0 || enemyColumn < 0) {
        return;
    }

    if (enemyRow == playerRow && enemyColumn == playerColumn) {
        state = GameState.FAILED;
        lastMessage = "Failed: DarkCube caught you.";
        return;
    }

    int rowDelta = Integer.compare(playerRow, enemyRow);
    int columnDelta = Integer.compare(playerColumn, enemyColumn);

    int tryRow = enemyRow;
    int tryColumn = enemyColumn;

    if (Math.abs(playerRow - enemyRow) >= Math.abs(playerColumn - enemyColumn)) {
        tryRow += rowDelta;
    } else {
        tryColumn += columnDelta;
    }

    if (canDarkCubeMoveTo(tryRow, tryColumn)) {
        enemyRow = tryRow;
        enemyColumn = tryColumn;
    }

    if (enemyRow == playerRow && enemyColumn == playerColumn) {
        state = GameState.FAILED;
        lastMessage = "Failed: DarkCube caught you.";
    }
}

private boolean canDarkCubeMoveTo(int row, int column) {
    if (!isInside(row, column)) {
        return false;
    }
    CellType cell = board[row][column];
    return cell != CellType.VOID && !cell.isGate();
}
```

---

## Крок 7 — показати enemy у 2D board

У `Field.java` знайди:

```java
public char getSymbolAt(int row, int column) {
    if (row == playerRow && column == playerColumn && state != GameState.FAILED) {
        return 'C';
    }
    return board[row][column].getSymbol();
}
```

Заміни на:

```java
public char getSymbolAt(int row, int column) {
    if (row == playerRow && column == playerColumn && state != GameState.FAILED) {
        return 'C';
    }
    if (row == enemyRow && column == enemyColumn && state != GameState.FAILED) {
        return 'D';
    }
    return board[row][column].getSymbol();
}
```

---

## Крок 8 — getters для 3D optional

У `Field.java`, після `getPlayerColumn()` додай:

```java
public int getEnemyRow() {
    return enemyRow;
}

public int getEnemyColumn() {
    return enemyColumn;
}
```

---

## Крок 9 — 2D visual

`CubeRollController.java`, у `boardCellFor(...)` перед `default`:

```java
case 'D' -> new BoardCell("cell darkcube", "D", "DarkCube enemy");
```

`style.css`:

```css
.darkcube {
    background: linear-gradient(135deg, #111827, #7c3aed);
    color: #f5f3ff;
    box-shadow: 0 0 18px rgba(124, 58, 237, 0.75);
}
```

---

## Крок 10 — 3D optional visual

### 10.1 `CubeRollStateDto.java`

Файл:

```text
src/main/java/sk/tuke/gamestudio/server/web/cuberoll3d/CubeRollStateDto.java
```

Знайди:

```java
int playerRow,
int playerColumn,
CubeFacesDto cubeFaces,
```

Заміни на:

```java
int playerRow,
int playerColumn,
int enemyRow,
int enemyColumn,
CubeFacesDto cubeFaces,
```

### 10.2 `CubeRollApiController.java`

У `toStateDto(...)` знайди:

```java
field.getPlayerRow(),
field.getPlayerColumn(),
toCubeFacesDto(field.getCube()),
```

Заміни на:

```java
field.getPlayerRow(),
field.getPlayerColumn(),
field.getEnemyRow(),
field.getEnemyColumn(),
toCubeFacesDto(field.getCube()),
```

### 10.3 `cuberoll-playcanvas.js`

У `MaterialFactory.build()` додай:

```js
this.materials.darkCube = this.standard('darkcube-enemy', '#111827', {
    emissiveHex: '#8b5cf6',
    emissiveIntensity: 0.85,
    shininess: 96
});
```

У `BoardRenderer.rebuild(state)` після циклу, який створює всі клітинки, встав:

```js
this.createDarkCube(state);
```

Тобто має бути приблизно так:

```js
for (let row = 0; row < state.rows; row++) {
    for (let column = 0; column < state.columns; column++) {
        this.createCell(row, column, state.cells[row][column]);
    }
}
this.createDarkCube(state);
```

У клас `BoardRenderer`, перед методом `update(dt)` встав:

```js
createDarkCube(state) {
    if (!Number.isInteger(state.enemyRow) || state.enemyRow < 0) {
        return;
    }
    const position = cellToWorld(state, state.enemyRow, state.enemyColumn);
    createBoxEntity(this.app, {
        name: 'darkcube-enemy',
        parent: this.root,
        position: new pc.Vec3(position.x, CUBE_CENTER_Y, position.z),
        scale: new pc.Vec3(CUBE_SIZE * 0.82, CUBE_SIZE * 0.82, CUBE_SIZE * 0.82),
        material: this.materials.materials.darkCube,
        castShadows: true,
        receiveShadows: true
    });
}
```

---

## Тестовий рівень

```java
new LevelDefinition(
        "DarkCube",
        "#########",
        "#S....F#",
        "#..D...#",
        "#########"
)
```

---

# 11. Falling counter / score penalty for falls

## Завдання

> Додай fall counter. Кожне падіння додає штраф до score або показується в HUD.

Це схоже на ranking у Edge, де результат може залежати від falls/time/collectibles.

Твоя поточна гра після падіння одразу FAILED, але counter все одно можна зберігати.

---

## Крок 1 — `Field.java`, поле

Після:

```java
private int moveCount;
```

встав:

```java
private int fallCount;
```

---

## Крок 2 — `reset()`

Після:

```java
this.moveCount = 0;
```

встав:

```java
this.fallCount = 0;
```

---

## Крок 3 — `moveWithOutcome()`, рахувати падіння

У void-блоці знайди:

```java
moveCount++;
state = GameState.FAILED;
```

Заміни на:

```java
moveCount++;
fallCount++;
state = GameState.FAILED;
```

---

## Крок 4 — score penalty

Знайди метод:

```java
public int calculateScore() {
```

Заміни на:

```java
public int calculateScore() {
    int score = 250 - moveCount * 10 - fallCount * 50;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

---

## Крок 5 — getter

Після `getMoveCount()` додай:

```java
public int getFallCount() {
    return fallCount;
}
```

---

# 12. Undo останнього ходу — найскладніша, але корисна механіка

## Завдання

> Додай undo. Гравець може повернути один останній хід.

Для 15–20 хвилин я б не обирав це, але якщо викладач попросить undo, найпростіше зробити snapshot у `Field`.

---

## Крок 1 — `Field.java`, snapshot record

Всередині класу `Field`, після полів, встав:

```java
private Snapshot lastSnapshot;

private record Snapshot(
        CellType[][] board,
        Cube cube,
        int playerRow,
        int playerColumn,
        int moveCount,
        int remainingPainters,
        GameState state,
        String lastMessage,
        FaceColor activeColor
) {
}
```

---

## Крок 2 — `Cube.java`, copy constructor

Файл:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Cube.java
```

Після полів додай constructor:

```java
public Cube() {
}

public Cube(Cube other) {
    this.top = other.top;
    this.bottom = other.bottom;
    this.north = other.north;
    this.south = other.south;
    this.west = other.west;
    this.east = other.east;
}
```

У тебе зараз implicit default constructor. Після додавання copy constructor треба явно додати `public Cube() {}`.

---

## Крок 3 — `Field.java`, helper copy board

Перед `exit()` встав:

```java
private CellType[][] copyBoard() {
    CellType[][] copy = new CellType[rowCount][columnCount];
    for (int row = 0; row < rowCount; row++) {
        System.arraycopy(board[row], 0, copy[row], 0, columnCount);
    }
    return copy;
}

private void restoreBoard(CellType[][] source) {
    for (int row = 0; row < rowCount; row++) {
        System.arraycopy(source[row], 0, board[row], 0, columnCount);
    }
}

private void saveSnapshot() {
    lastSnapshot = new Snapshot(
            copyBoard(),
            new Cube(cube),
            playerRow,
            playerColumn,
            moveCount,
            remainingPainters,
            state,
            lastMessage,
            activeColor
    );
}
```

---

## Крок 4 — зберігати snapshot перед ходом

У `moveWithOutcome()` після перевірки `state != PLAYING`, перед визначенням destination, встав:

```java
saveSnapshot();
```

Краще після цього блоку:

```java
if (state != GameState.PLAYING) {
    return new MoveOutcome(...);
}
```

---

## Крок 5 — метод undo

Перед `exit()` встав:

```java
public boolean undo() {
    if (lastSnapshot == null) {
        return false;
    }
    restoreBoard(lastSnapshot.board());
    this.cube = new Cube(lastSnapshot.cube());
    this.playerRow = lastSnapshot.playerRow();
    this.playerColumn = lastSnapshot.playerColumn();
    this.moveCount = lastSnapshot.moveCount();
    this.remainingPainters = lastSnapshot.remainingPainters();
    this.state = lastSnapshot.state();
    this.lastMessage = "Undo: previous move restored.";
    this.activeColor = lastSnapshot.activeColor();
    this.lastSnapshot = null;
    return true;
}
```

---

## Крок 6 — controller endpoint для 2D

Файл:

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

Після reset endpoint:

```java
@PostMapping("/cuberoll/reset")
public String reset(HttpSession httpSession) {
    getSession(httpSession).resetLevel();
    return "redirect:/cuberoll";
}
```

встав:

```java
@PostMapping("/cuberoll/undo")
public String undo(HttpSession httpSession) {
    getSession(httpSession).getField().undo();
    return "redirect:/cuberoll";
}
```

---

## Крок 7 — 2D button

Файл:

```text
src/main/resources/templates/cuberoll.html
```

У toolbar біля reset встав:

```html
<form method="post" th:action="@{/cuberoll/undo}">
    <button type="submit" class="ghost">Undo</button>
</form>
```

---

# 13. Найкоротша шпаргалка: що найімовірніше дадуть

Якщо дадуть “зроби як у Bloxorz”:

```text
switch / bridge       → розділ 1
fragile tile          → розділ 2
```

Якщо дадуть “додай collectible як у Edge”:

```text
prism / star          → розділ 3
fall counter/ranking  → розділ 11
DarkCube enemy        → розділ 10
```

Якщо дадуть “додай телепорт або спеціальну плиту”:

```text
teleport              → розділ 4
ice                   → розділ 5
conveyor              → розділ 6
hidden finish         → розділ 7
```

Якщо дадуть “додай рухомий об'єкт”:

```text
pushable block        → розділ 8
```

Якщо дадуть “зроби щоб треба було наступити на всі плити”:

```text
target quota          → розділ 9
```

---

# 14. Безпечні механіки на 15–20 хв

Я б на сесії обрав щось із цього:

```text
1. Prism / collectible before finish
2. Fragile tile
3. Switch + bridge
4. Teleport
5. Target quota
6. Pushable box, якщо є час
```

Найризикованіші:

```text
DarkCube enemy
Undo
Conveyor with special tiles
```

Вони працюють, але потребують більше уважності.

*/ 