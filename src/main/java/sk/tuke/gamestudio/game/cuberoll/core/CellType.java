package sk.tuke.gamestudio.game.cuberoll.core;

public enum CellType {
    FLOOR('.'),
    VOID('#'),
    FINISH('F'),
    PAINTER_RED('r', FaceColor.RED),
    GATE_RED('R', FaceColor.RED),
    PAINTER_BLUE('b', FaceColor.BLUE),
    GATE_BLUE('B', FaceColor.BLUE),
    PAINTER_GREEN('g', FaceColor.GREEN),
    GATE_GREEN('G', FaceColor.GREEN),
    PAINTER_YELLOW('y', FaceColor.YELLOW),
    GATE_YELLOW('Y', FaceColor.YELLOW);

    private final char symbol;
    private final FaceColor color;

    CellType(char symbol) {
        this(symbol, FaceColor.NONE);
    }

    CellType(char symbol, FaceColor color) {
        this.symbol = symbol;
        this.color = color;
    }

    public char getSymbol() {
        return symbol;
    }

    public FaceColor getColor() {
        return color;
    }

    public boolean isPainter() {
        return name().startsWith("PAINTER_");
    }

    public boolean isGate() {
        return name().startsWith("GATE_");
    }
}

/*
1. Змінити формулу score
2. Додати ліміт ходів
3. Gate пропускає завжди
4. Неправильний gate не вбиває, а просто не пускає
5. Gate не зникає після проходження
6. Gate не очищає колір куба
7. Painter не зникає і може використовуватись багато разів
8. Painter фарбує весь куб
9. Додати trap-клітинку x
10. Додати bonus-клітинку o
11. Finish вимагає червону нижню сторону куба
12. Finish завжди відкритий
13. Додати новий рівень
14. Додати purple painter/gate
15. Додати checkpoint
16. Додати lives / 3 життя
17. Додати ліміт по часу

* # CubeRoll — шпаргалки для нових механік

База під твою версію `CubeRollGame_mobile_panels.zip`.

Головні файли:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Cube.java
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/FaceColor.java
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
src/main/resources/static/style.css
src/main/resources/static/cuberoll-playcanvas.js
```

Після зміни Java-коду перезапусти сервер:

```bash
mvn spring-boot:run -Dspring-boot.run.main-class=sk.tuke.gamestudio.server.GameStudioServer
```

Після зміни CSS/JS у браузері натисни:

```text
Ctrl + F5
```

Номери рядків нижче приблизно для твоєї поточної версії. Якщо рядок не співпав, шукай по тексту, який я даю.

---

## 1. Змінити формулу score

### Завдання

> Зроби іншу формулу рахунку.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Де

Приблизно рядки `314–320`. Шукай:

```java
public int calculateScore() {
```

### Що прибрати

Прибери старий метод:

```java
public int calculateScore() {
    int score = 250 - moveCount * 10;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

### Що вставити

Приклад: базовий score 500, за кожний хід мінус 5, за перемогу +200.

```java
public int calculateScore() {
    int score = 500 - moveCount * 5;
    if (state == GameState.SOLVED) {
        score += 200;
    }
    return Math.max(score, 10);
}
```

---

## 2. Додати ліміт ходів

### Завдання

> Гравець програє, якщо зробив 30 ходів.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати константу

Після рядка `3`:

```java
public class Field {
```

встав:

```java
private static final int MAX_MOVES = 30;
```

Буде так:

```java
public class Field {
    private static final int MAX_MOVES = 30;

    private final LevelDefinition definition;
```

### Крок 2 — додати перевірку

У методі `moveWithOutcome`, приблизно після рядка `213`, перед:

```java
return new MoveOutcome(
```

встав:

```java
if (state == GameState.PLAYING && moveCount >= MAX_MOVES) {
    state = GameState.FAILED;
    lastMessage = "Failed: move limit exceeded.";
}
```

### Готовий шматок має виглядати так

```java
        } else {
            lastMessage = "Move the painted face to the bottom when stepping onto a matching gate.";
        }

        if (state == GameState.PLAYING && moveCount >= MAX_MOVES) {
            state = GameState.FAILED;
            lastMessage = "Failed: move limit exceeded.";
        }

        return new MoveOutcome(
```

---

## 3. Gate пропускає завжди, навіть без кольору

### Завдання

> Зроби, щоб через gate можна було проходити без правильної сторони куба.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Де

Приблизно рядки `161–184`. Шукай:

```java
if (destination.isGate() && bottomFaceAfterRoll != destination.getColor()) {
```

### Що прибрати / закоментувати

Закоментуй весь цей блок:

```java
// Gate must be checked against the REAL bottom face after the roll,
// not against some stored active color.
if (destination.isGate() && bottomFaceAfterRoll != destination.getColor()) {
    state = GameState.FAILED;
    lastMessage = "Failed: gate " + destination.getColor().getDisplayName()
            + " requires the painted bottom face.";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            bottomFaceAfterRoll,
            true,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.ROLLED, MoveEffect.WRONG_GATE_COLOR)
    );
}
```

Можеш просто зробити так:

```java
// Gate color check disabled: any gate can be passed.
// if (destination.isGate() && bottomFaceAfterRoll != destination.getColor()) {
//     state = GameState.FAILED;
//     lastMessage = "Failed: gate " + destination.getColor().getDisplayName()
//             + " requires the painted bottom face.";
//     return new MoveOutcome(
//             direction,
//             fromRow,
//             fromColumn,
//             attemptedRow,
//             attemptedColumn,
//             playerRow,
//             playerColumn,
//             destination,
//             bottomFaceAfterRoll,
//             true,
//             false,
//             stateBefore,
//             state,
//             lastMessage,
//             java.util.List.of(MoveEffect.ROLLED, MoveEffect.WRONG_GATE_COLOR)
//     );
// }
```

### Бажано ще змінити текст gate

Нижче, приблизно рядки `201–206`, шукай:

```java
} else if (destination.isGate()) {
```

Заміни повідомлення:

```java
lastMessage = "Gate opened permanently: color was consumed, repaint before the next gate.";
```

на:

```java
lastMessage = "Gate opened permanently.";
```

---

## 4. Неправильний gate не вбиває, а просто не пускає

### Завдання

> Якщо колір неправильний, gate блокує хід, але гра не програна.

Це чистіший варіант, бо куб не буде перекочуватись у неправильний gate.

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Cube.java
```

### Куди вставити

Після методу `paintBottom`, приблизно після рядка `54`:

```java
public void paintBottom(FaceColor color) {
    bottom = color;
}
```

встав:

```java
public FaceColor getBottomAfterRoll(Direction direction) {
    return switch (direction) {
        case NORTH -> north;
        case SOUTH -> south;
        case WEST -> west;
        case EAST -> east;
    };
}
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — вставити перевірку ДО `cube.roll(direction)`

У `moveWithOutcome` знайди приблизно рядок `157`:

```java
cube.roll(direction);
```

Перед ним встав:

```java
FaceColor bottomFaceIfRolled = cube.getBottomAfterRoll(direction);
if (destination.isGate() && bottomFaceIfRolled != destination.getColor()) {
    lastMessage = "Gate " + destination.getColor().getDisplayName()
            + " requires the painted bottom face.";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            bottomFaceIfRolled,
            false,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.WRONG_GATE_COLOR)
    );
}
```

### Крок 2 — прибрати старий fail-блок gate

Нижче, приблизно рядки `161–184`, видали або закоментуй старий блок:

```java
if (destination.isGate() && bottomFaceAfterRoll != destination.getColor()) {
    state = GameState.FAILED;
    lastMessage = "Failed: gate " + destination.getColor().getDisplayName()
            + " requires the painted bottom face.";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            bottomFaceAfterRoll,
            true,
            false,
            stateBefore,
            state,
            lastMessage,
            java.util.List.of(MoveEffect.ROLLED, MoveEffect.WRONG_GATE_COLOR)
    );
}
```

---

## 5. Gate не зникає після проходження

### Завдання

> Gate після проходження залишається на карті.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Де

Приблизно рядки `201–206`. Шукай:

```java
} else if (destination.isGate()) {
```

### Що прибрати

Закоментуй рядок:

```java
board[playerRow][playerColumn] = CellType.FLOOR;
```

### Готовий блок

```java
} else if (destination.isGate()) {
    activeColor = FaceColor.NONE;
    cube.clearColors();
    // Gate stays on the map.
    // board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Gate passed, but it stays on the board.";
    effects.add(MoveEffect.GATE_OPENED);
}
```

---

## 6. Gate не очищає колір куба

### Завдання

> Після gate колір куба не пропадає.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Де

Приблизно рядки `201–206`. Шукай:

```java
} else if (destination.isGate()) {
```

### Що прибрати

Закоментуй:

```java
activeColor = FaceColor.NONE;
cube.clearColors();
```

### Готовий блок

```java
} else if (destination.isGate()) {
    // Color is not consumed by gate.
    // activeColor = FaceColor.NONE;
    // cube.clearColors();
    board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Gate opened permanently, color stayed on the cube.";
    effects.add(MoveEffect.GATE_OPENED);
}
```

---

## 7. Painter не зникає і може використовуватися багато разів

### Завдання

> Painter повинен залишатися на полі після використання.

Це правильний варіант, який не робить `remainingPainters` від’ємним.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати поле

Після рядка `18`:

```java
private FaceColor activeColor;
```

встав:

```java
private boolean[][] usedPainters;
```

### Крок 2 — створити масив у конструкторі

У конструкторі після рядка `25`:

```java
this.board = new CellType[rowCount][columnCount];
```

встав:

```java
this.usedPainters = new boolean[rowCount][columnCount];
```

### Крок 3 — скидати використання painter-ів при reset

У методі `loadBoardFromDefinition`, приблизно після рядка `253`:

```java
char symbol = rows[row].charAt(column);
```

встав:

```java
usedPainters[row][column] = false;
```

### Крок 4 — замінити painter-блок

Знайди приблизно рядки `193–200`:

```java
if (destination.isPainter()) {
    cube.paintBottom(destination.getColor());
    activeColor = destination.getColor(); // can stay for status/debug
    board[playerRow][playerColumn] = CellType.FLOOR;
    remainingPainters--;
    lastMessage = "Painter used once: bottom face is now "
            + destination.getColor().getDisplayName() + ".";
    effects.add(MoveEffect.PAINTER_USED);
}
```

Заміни на:

```java
if (destination.isPainter()) {
    cube.paintBottom(destination.getColor());
    activeColor = destination.getColor();

    if (!usedPainters[playerRow][playerColumn]) {
        usedPainters[playerRow][playerColumn] = true;
        remainingPainters--;
    }

    lastMessage = "Painter reused: bottom face is now "
            + destination.getColor().getDisplayName() + ".";
    effects.add(MoveEffect.PAINTER_USED);
}
```

Головне: тут немає цього рядка:

```java
board[playerRow][playerColumn] = CellType.FLOOR;
```

---

## 8. Painter фарбує весь куб, а не тільки нижню грань

### Завдання

> Painter має фарбувати всі сторони куба.

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Cube.java
```

### Куди вставити

Після методу `paintBottom`, приблизно після рядка `54`:

```java
public void paintBottom(FaceColor color) {
    bottom = color;
}
```

встав:

```java
public void paintAll(FaceColor color) {
    top = color;
    bottom = color;
    north = color;
    south = color;
    west = color;
    east = color;
}
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Що замінити

У painter-блоці, приблизно рядок `194`, заміни:

```java
cube.paintBottom(destination.getColor());
```

на:

```java
cube.paintAll(destination.getColor());
```

### Бажано змінити повідомлення

Заміни:

```java
lastMessage = "Painter used once: bottom face is now "
        + destination.getColor().getDisplayName() + ".";
```

на:

```java
lastMessage = "Painter used once: all cube faces are now "
        + destination.getColor().getDisplayName() + ".";
```

---

## 9. Додати trap-клітинку `x`: наступив — програв

### Завдання

> Додай пастку. Якщо куб наступив на `x`, гра програна.

## Java-механіка

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
```

### Що замінити

Зараз кінець enum такий:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW);
```

Заміни на:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW),
TRAP('x');
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати символ у перший switch

У конструкторі, приблизно рядки `49–57`, після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'x' -> board[row][column] = CellType.TRAP;
```

### Крок 2 — додати символ у `loadBoardFromDefinition`

У методі `loadBoardFromDefinition`, приблизно рядки `274–279`, після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'x' -> board[row][column] = CellType.TRAP;
```

### Крок 3 — додати механіку trap

У `moveWithOutcome`, приблизно рядок `193`, знайди:

```java
if (destination.isPainter()) {
```

Заміни цей початок на:

```java
if (destination == CellType.TRAP) {
    state = GameState.FAILED;
    lastMessage = "Failed: you stepped on a trap.";
    effects.add(MoveEffect.FELL);
} else if (destination.isPainter()) {
```

Тобто старий painter-блок має продовжитись після `else if`.

## Додати trap на рівень

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

У будь-який рядок рівня додай символ:

```text
x
```

Наприклад було:

```java
"#S...###",
```

можна зробити:

```java
"#S.x.###",
```

Але рядок має залишитись такої ж довжини, як інші рядки цього рівня.

## 2D-відображення trap

### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

У методі `boardCellFor`, приблизно рядки `176–190`, перед `default` встав:

```java
case 'x' -> new BoardCell("cell trap", "X", "trap");
```

### Файл

```text
src/main/resources/static/style.css
```

Після `.gate.yellow` додай:

```css
.trap {
    background: linear-gradient(135deg, #7f1d1d, #ef4444);
    color: #fee2e2;
    box-shadow: inset 0 0 0 3px #fca5a5;
}
```

## 3D-відображення trap, мінімальний варіант

Без змін у JS trap у 3D може виглядати як звичайна плита, але механіка буде працювати. Якщо треба, щоб trap було видно, зроби ще ці JS-кроки.

### Файл

```text
src/main/resources/static/cuberoll-playcanvas.js
```

### Крок 1 — MaterialFactory

У `class MaterialFactory`, у методі `build`, після матеріалу `wrongGate`, приблизно після рядка `460`, встав:

```js
this.materials.trap = this.standard('trap-pad', '#ef4444', {
    emissiveHex: '#fb7185',
    emissiveIntensity: 1.2,
    shininess: 88
});
```

### Крок 2 — render trap

У методі `createCell`, приблизно рядки `798–804`, знайди:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType.startsWith('PAINTER_')) {
    this.createPainter(actor, position);
} else if (cellType.startsWith('GATE_')) {
    this.createGate(actor, position);
} else {
```

Заміни на:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType === 'TRAP') {
    const trap = createBoxEntity(this.app, {
        name: `trap-${row}-${column}`,
        parent: this.root,
        position: new pc.Vec3(position.x, TILE_HEIGHT * 0.84, position.z),
        scale: new pc.Vec3(TILE_SIZE * 0.58, 0.055, TILE_SIZE * 0.58),
        material: this.materials.materials.trap,
        castShadows: true,
        receiveShadows: true
    });
    actor.parts.push(trap);
} else if (cellType.startsWith('PAINTER_')) {
    this.createPainter(actor, position);
} else if (cellType.startsWith('GATE_')) {
    this.createGate(actor, position);
} else {
```

---

## 10. Додати bonus-клітинку `o`: +50 score

### Завдання

> Додай бонусну плиту. Якщо куб наступив на `o`, гравець отримує +50 score, а плитка зникає.

## Java-механіка

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
```

Заміни кінець enum:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW);
```

на:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW),
BONUS('o');
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати поле

Після рядка `18`:

```java
private FaceColor activeColor;
```

встав:

```java
private int bonusScore;
```

### Крок 2 — скидати bonusScore при reset

У методі `reset`, приблизно після рядка `241`:

```java
this.activeColor = FaceColor.NONE;
```

встав:

```java
this.bonusScore = 0;
```

### Крок 3 — додати символ у перший switch

У конструкторі після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'o' -> board[row][column] = CellType.BONUS;
```

### Крок 4 — додати символ у `loadBoardFromDefinition`

У `loadBoardFromDefinition` після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'o' -> board[row][column] = CellType.BONUS;
```

### Крок 5 — додати механіку bonus

У `moveWithOutcome`, приблизно рядок `193`, знайди:

```java
if (destination.isPainter()) {
```

Заміни початок на:

```java
if (destination == CellType.BONUS) {
    bonusScore += 50;
    board[playerRow][playerColumn] = CellType.FLOOR;
    lastMessage = "Bonus collected: +50 points.";
} else if (destination.isPainter()) {
```

### Крок 6 — змінити score formula

Знайди метод `calculateScore`, приблизно рядки `314–320`:

```java
public int calculateScore() {
    int score = 250 - moveCount * 10;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

Заміни перший рядок score на:

```java
int score = 250 - moveCount * 10 + bonusScore;
```

Повний метод:

```java
public int calculateScore() {
    int score = 250 - moveCount * 10 + bonusScore;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

## Додати bonus на рівень

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

У рядок рівня додай:

```text
o
```

Наприклад:

```java
"#S.o.###",
```

Не змінюй довжину рядка відносно інших рядків у цьому рівні.

## 2D-відображення bonus

### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

У `boardCellFor`, перед `default`, додай:

```java
case 'o' -> new BoardCell("cell bonus", "+", "bonus");
```

### Файл

```text
src/main/resources/static/style.css
```

Після `.gate.yellow` додай:

```css
.bonus {
    background: linear-gradient(135deg, #f59e0b, #fde68a);
    color: #422006;
    text-shadow: none;
    box-shadow: 0 0 16px rgba(250, 204, 21, 0.55);
}
```

## 3D-відображення bonus, мінімальний варіант

Без JS-змін bonus у 3D буде виглядати як floor, але score-механіка працюватиме.

Щоб було видно у 3D:

### Файл

```text
src/main/resources/static/cuberoll-playcanvas.js
```

### Крок 1 — MaterialFactory

У `build`, після `wrongGate`, встав:

```js
this.materials.bonus = this.standard('bonus-pad', '#facc15', {
    emissiveHex: '#fde68a',
    emissiveIntensity: 1.3,
    shininess: 96
});
```

### Крок 2 — createCell

У `createCell` заміни шматок:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType.startsWith('PAINTER_')) {
```

на:

```js
if (cellType === 'FINISH') {
    this.createFinish(actor, position);
} else if (cellType === 'BONUS') {
    const bonus = createBoxEntity(this.app, {
        name: `bonus-${row}-${column}`,
        parent: this.root,
        position: new pc.Vec3(position.x, TILE_HEIGHT * 0.84, position.z),
        scale: new pc.Vec3(TILE_SIZE * 0.50, 0.06, TILE_SIZE * 0.50),
        material: this.materials.materials.bonus,
        castShadows: true,
        receiveShadows: true
    });
    actor.parts.push(bonus);
} else if (cellType.startsWith('PAINTER_')) {
```

---

## 11. Finish вимагає червону нижню сторону куба

### Завдання

> На finish можна зайти тільки якщо нижня грань куба червона.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Де вставити

У методі `moveWithOutcome`, після:

```java
cube.roll(direction);
FaceColor bottomFaceAfterRoll = cube.getBottom();
moveCount++;
```

приблизно після рядка `159`, встав:

```java
if (destination == CellType.FINISH && bottomFaceAfterRoll != FaceColor.RED) {
    state = GameState.FAILED;
    lastMessage = "Failed: finish requires red bottom face.";
    return new MoveOutcome(
            direction,
            fromRow,
            fromColumn,
            attemptedRow,
            attemptedColumn,
            playerRow,
            playerColumn,
            destination,
            bottomFaceAfterRoll,
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

## 12. Finish завжди відкритий, навіть якщо не використані всі painter-и

### Завдання

> Прибери умову, що треба використати всі painter-и перед finish.

### Варіант A — прибрати для всіх рівнів

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Що прибрати

Приблизно рядки `134–155`, закоментуй цей блок:

```java
if (destination == CellType.FINISH
        && definition.isFinishRequiresAllPainters()
        && remainingPainters > 0) {
    lastMessage = "Finish is locked: use all painter blocks first (remaining: " + remainingPainters + ").";
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

### Варіант B — тільки для одного рівня

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

Якщо рівень має `true`, наприклад:

```java
new LevelDefinition(
        "Red And Blue",
        true,
```

заміни `true` на `false`:

```java
new LevelDefinition(
        "Red And Blue",
        false,
```

---

## 13. Додати новий рівень

### Завдання

> Додай новий level.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

### Де

Перед закриттям списку, приблизно перед рядком `43`:

```java
);
```

### Що зробити

У попереднього рівня треба додати кому після `)`.

Було в кінці:

```java
            new LevelDefinition(
                    "Three Colors",
                    true,
                    "################",
                    "#S...###########",
                    "#.##.###########",
                    "#..rR..#########",
                    "#..###.#########",
                    "######.bB..#####",
                    "######.##..#####",
                    "######...#gG.F##",
                    "##########..####",
                    "################"
            )
    );
```

Зроби:

```java
            new LevelDefinition(
                    "Three Colors",
                    true,
                    "################",
                    "#S...###########",
                    "#.##.###########",
                    "#..rR..#########",
                    "#..###.#########",
                    "######.bB..#####",
                    "######.##..#####",
                    "######...#gG.F##",
                    "##########..####",
                    "################"
            ),
            new LevelDefinition(
                    "Exam Level",
                    "########",
                    "#S....F#",
                    "#.####.#",
                    "#..rR..#",
                    "########"
            )
    );
```

У кожному рівні всі рядки мають бути однакової довжини.

---

## 14. Додати новий purple color: purple painter `p` і purple gate `P`

### Завдання

> Додай новий колір, наприклад purple.

## Java-механіка

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/FaceColor.java
```

### Що замінити

Було:

```java
NONE("none", '-'),
RED("red", 'R'),
BLUE("blue", 'B'),
GREEN("green", 'G'),
YELLOW("yellow", 'Y');
```

Зроби:

```java
NONE("none", '-'),
RED("red", 'R'),
BLUE("blue", 'B'),
GREEN("green", 'G'),
YELLOW("yellow", 'Y'),
PURPLE("purple", 'P');
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
```

Було:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW);
```

Зроби:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW),
PAINTER_PURPLE('p', FaceColor.PURPLE),
GATE_PURPLE('P', FaceColor.PURPLE);
```

### Файл 3

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Конструктор — перший switch

Після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'p' -> board[row][column] = CellType.PAINTER_PURPLE;
case 'P' -> board[row][column] = CellType.GATE_PURPLE;
```

### `loadBoardFromDefinition` — другий switch

Після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'p' -> {
    board[row][column] = CellType.PAINTER_PURPLE;
    painterCount++;
}
case 'P' -> board[row][column] = CellType.GATE_PURPLE;
```

## Додати purple на рівень

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

Тепер можна використовувати:

```text
p  purple painter
P  purple gate
```

Наприклад:

```java
"#..pP.F#",
```

## 2D-відображення

### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

У `boardCellFor`, перед `default`, додай:

```java
case 'p' -> new BoardCell("cell painter purple", "p", "purple painter");
case 'P' -> new BoardCell("cell gate purple", "P", "purple gate");
```

### Файл

```text
src/main/resources/static/style.css
```

Після `.gate.yellow` додай:

```css
.painter.purple {
    background: #a855f7;
}

.gate.purple {
    background: linear-gradient(135deg, #581c87, #c084fc);
}
```

## 3D-відображення

### Файл

```text
src/main/resources/static/cuberoll-playcanvas.js
```

### Крок 1 — додати колір

Угорі, у `FACE_COLORS`, було:

```js
const FACE_COLORS = {
    RED: '#ef4444',
    BLUE: '#3b82f6',
    GREEN: '#22c55e',
    YELLOW: '#facc15',
    NONE: '#dbeafe'
};
```

Зроби:

```js
const FACE_COLORS = {
    RED: '#ef4444',
    BLUE: '#3b82f6',
    GREEN: '#22c55e',
    YELLOW: '#facc15',
    PURPLE: '#a855f7',
    NONE: '#dbeafe'
};
```

### Крок 2 — оновити regex

Знайди приблизно рядок `128`:

```js
const match = /_(RED|BLUE|GREEN|YELLOW)$/.exec(cellType || '');
```

Заміни на:

```js
const match = /_(RED|BLUE|GREEN|YELLOW|PURPLE)$/.exec(cellType || '');
```

---

## 15. Додати checkpoint `c`: якщо впав — повертає на checkpoint

### Завдання

> Додай checkpoint. Коли гравець наступив на `c`, позиція зберігається. Якщо впав у void — повертається на checkpoint, а не програє.

## Java-механіка

### Файл 1

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/CellType.java
```

Заміни кінець enum:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW);
```

на:

```java
PAINTER_YELLOW('y', FaceColor.YELLOW),
GATE_YELLOW('Y', FaceColor.YELLOW),
CHECKPOINT('c');
```

### Файл 2

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати поля

Після:

```java
private FaceColor activeColor;
```

встав:

```java
private int checkpointRow;
private int checkpointColumn;
```

### Крок 2 — reset checkpoint на старт

У методі `reset`, після:

```java
this.playerColumn = startColumn;
```

встав:

```java
this.checkpointRow = startRow;
this.checkpointColumn = startColumn;
```

### Крок 3 — додати символ у перший switch

У конструкторі після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'c' -> board[row][column] = CellType.CHECKPOINT;
```

### Крок 4 — додати символ у `loadBoardFromDefinition`

У `loadBoardFromDefinition` після:

```java
case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
```

встав:

```java
case 'c' -> board[row][column] = CellType.CHECKPOINT;
```

### Крок 5 — замінити VOID-блок

У `moveWithOutcome`, приблизно рядки `110–132`, знайди:

```java
if (destination == CellType.VOID) {
    cube.roll(direction);
    moveCount++;
    state = GameState.FAILED;
    lastMessage = "Failed: the cube fell into void or left the board.";
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
            java.util.List.of(MoveEffect.ROLLED, MoveEffect.FELL)
    );
}
```

Заміни на:

```java
if (destination == CellType.VOID) {
    cube.roll(direction);
    moveCount++;
    playerRow = checkpointRow;
    playerColumn = checkpointColumn;
    cube = new Cube();
    activeColor = FaceColor.NONE;
    lastMessage = "You fell into void and returned to checkpoint.";
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
            java.util.List.of(MoveEffect.ROLLED, MoveEffect.FELL)
    );
}
```

### Крок 6 — додати механіку checkpoint

У `moveWithOutcome`, приблизно рядок `193`, знайди:

```java
if (destination.isPainter()) {
```

Заміни початок на:

```java
if (destination == CellType.CHECKPOINT) {
    checkpointRow = playerRow;
    checkpointColumn = playerColumn;
    lastMessage = "Checkpoint saved.";
} else if (destination.isPainter()) {
```

## 2D-відображення checkpoint

### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/CubeRollController.java
```

У `boardCellFor`, перед `default`, додай:

```java
case 'c' -> new BoardCell("cell checkpoint", "C", "checkpoint");
```

### Файл

```text
src/main/resources/static/style.css
```

Після `.gate.yellow` додай:

```css
.checkpoint {
    background: linear-gradient(135deg, #0891b2, #67e8f9);
    color: #083344;
    text-shadow: none;
    box-shadow: 0 0 16px rgba(103, 232, 249, 0.55);
}
```

## Додати checkpoint на рівень

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Levels.java
```

У рівень додай символ:

```text
c
```

Наприклад:

```java
"#S.c.RF#",
```

---

## 16. Додати lives: 3 життя замість миттєвого програшу від void

### Завдання

> У гравця є 3 життя. Якщо падає в void — життя мінус 1. Коли життів 0 — програш.

### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

### Крок 1 — додати поле

Після:

```java
private FaceColor activeColor;
```

встав:

```java
private int lives;
```

### Крок 2 — reset lives

У методі `reset`, після:

```java
this.moveCount = 0;
```

встав:

```java
this.lives = 3;
```

### Крок 3 — замінити VOID-блок

Заміни старий блок `if (destination == CellType.VOID) { ... }` приблизно рядки `110–132` на:

```java
if (destination == CellType.VOID) {
    cube.roll(direction);
    moveCount++;
    lives--;

    if (lives <= 0) {
        state = GameState.FAILED;
        lastMessage = "Failed: no lives left.";
    } else {
        playerRow = startRow;
        playerColumn = startColumn;
        cube = new Cube();
        activeColor = FaceColor.NONE;
        lastMessage = "You fell. Lives left: " + lives + ".";
    }

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
            java.util.List.of(MoveEffect.ROLLED, MoveEffect.FELL)
    );
}
```

### Крок 4 — якщо треба getter

Після `getRemainingPainters()` можеш додати:

```java
public int getLives() {
    return lives;
}
```

Для механіки getter не обов’язковий, але якщо треба показати життя в UI — знадобиться.

---

# Найшвидші варіанти на захисті

Якщо часу мало, найпростіші зміни:

1. Score formula — тільки `calculateScore()`.
2. Gate пропускає завжди — закоментувати один `if` у `Field.java`.
3. Gate не очищає колір — закоментувати `activeColor = NONE` і `cube.clearColors()`.
4. Gate не зникає — закоментувати `board[playerRow][playerColumn] = CellType.FLOOR`.
5. Painter фарбує весь куб — додати `paintAll()` у `Cube.java` і замінити один виклик.
6. Новий level — тільки `Levels.java`.



---

## 17. Додати ліміт по часу

### Завдання

> Гравець має, наприклад, 60 секунд на рівень. Якщо час закінчився — програш.

Це робиться в головному файлі механіки:

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

Мінімальна механіка буде працювати і в 2D, і в 3D, бо обидва режими беруть стан гри з `Field.java`.

---

### Крок 1 — додати константу часу

#### Файл

```text
src/main/java/sk/tuke/gamestudio/game/cuberoll/core/Field.java
```

#### Де вставити

На самому початку класу, одразу після:

```java
public class Field {
```

#### Що вставити

```java
private static final long TIME_LIMIT_MILLIS = 60_000L;
```

Буде так:

```java
public class Field {
    private static final long TIME_LIMIT_MILLIS = 60_000L;

    private final LevelDefinition definition;
```

`60_000L` — це 60 секунд. Якщо треба 30 секунд:

```java
private static final long TIME_LIMIT_MILLIS = 30_000L;
```

Якщо треба 2 хвилини:

```java
private static final long TIME_LIMIT_MILLIS = 120_000L;
```

---

### Крок 2 — додати поле старту таймера

#### Файл

```text
Field.java
```

#### Де вставити

Приблизно біля інших полів класу. Шукай:

```java
private FaceColor activeColor;
```

#### Що вставити після нього

```java
private long levelStartedAtMillis;
```

Буде так:

```java
private FaceColor activeColor;
private long levelStartedAtMillis;
```

---

### Крок 3 — запускати таймер при reset рівня

#### Файл

```text
Field.java
```

#### Де вставити

Шукай метод:

```java
public void reset() {
```

У ньому знайди:

```java
this.moveCount = 0;
```

#### Що вставити після нього

```java
this.levelStartedAtMillis = System.currentTimeMillis();
```

Буде так:

```java
this.moveCount = 0;
this.levelStartedAtMillis = System.currentTimeMillis();
this.state = GameState.PLAYING;
```

---

### Крок 4 — додати перевірку часу

#### Файл

```text
Field.java
```

#### Де вставити

Знайди метод:

```java
public String getStatusLine() {
    return lastMessage;
}
```

#### Що прибрати

Прибери старий метод:

```java
public String getStatusLine() {
    return lastMessage;
}
```

#### Що вставити замість нього

```java
public String getStatusLine() {
    updateTimeLimit();
    return lastMessage;
}

private void updateTimeLimit() {
    if (state == GameState.PLAYING && getRemainingTimeMillis() <= 0) {
        state = GameState.FAILED;
        lastMessage = "Failed: time limit exceeded.";
    }
}

public long getRemainingTimeMillis() {
    if (levelStartedAtMillis <= 0) {
        return TIME_LIMIT_MILLIS;
    }
    long elapsed = System.currentTimeMillis() - levelStartedAtMillis;
    return Math.max(0, TIME_LIMIT_MILLIS - elapsed);
}

public int getRemainingTimeSeconds() {
    return (int) Math.ceil(getRemainingTimeMillis() / 1000.0);
}
```

---

### Крок 5 — перевіряти час перед кожним ходом

#### Файл

```text
Field.java
```

#### Де вставити

У методі:

```java
public MoveOutcome moveWithOutcome(Direction direction)
```

знайди:

```java
GameState stateBefore = state;

if (state != GameState.PLAYING) {
```

#### Що вставити між ними

```java
updateTimeLimit();
```

Буде так:

```java
GameState stateBefore = state;
updateTimeLimit();

if (state != GameState.PLAYING) {
```

Тепер якщо гравець спробує зробити хід після завершення часу, гра стане `FAILED`.

---

### Крок 6 — щоб `state` автоматично оновлювався при показі сторінки

#### Файл

```text
Field.java
```

#### Де

Шукай метод:

```java
public GameState getState() {
    return state;
}
```

#### Що прибрати

```java
public GameState getState() {
    return state;
}
```

#### Що вставити

```java
public GameState getState() {
    updateTimeLimit();
    return state;
}
```

---

### Крок 7 — щоб score теж враховував закінчення часу

#### Файл

```text
Field.java
```

#### Де

Шукай:

```java
public int calculateScore() {
    int score = 250 - moveCount * 10;
```

#### Що змінити

Додай `updateTimeLimit();` на початок методу:

```java
public int calculateScore() {
    updateTimeLimit();
    int score = 250 - moveCount * 10;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

---

## Повний готовий блок для `Field.java`

Якщо треба швидко, роби так:

### 1. Після `public class Field {` встав:

```java
private static final long TIME_LIMIT_MILLIS = 60_000L;
```

### 2. Після `private FaceColor activeColor;` встав:

```java
private long levelStartedAtMillis;
```

### 3. У `reset()` після `this.moveCount = 0;` встав:

```java
this.levelStartedAtMillis = System.currentTimeMillis();
```

### 4. У `moveWithOutcome()` після `GameState stateBefore = state;` встав:

```java
updateTimeLimit();
```

### 5. Заміни `getStatusLine()` на цей блок:

```java
public String getStatusLine() {
    updateTimeLimit();
    return lastMessage;
}

private void updateTimeLimit() {
    if (state == GameState.PLAYING && getRemainingTimeMillis() <= 0) {
        state = GameState.FAILED;
        lastMessage = "Failed: time limit exceeded.";
    }
}

public long getRemainingTimeMillis() {
    if (levelStartedAtMillis <= 0) {
        return TIME_LIMIT_MILLIS;
    }
    long elapsed = System.currentTimeMillis() - levelStartedAtMillis;
    return Math.max(0, TIME_LIMIT_MILLIS - elapsed);
}

public int getRemainingTimeSeconds() {
    return (int) Math.ceil(getRemainingTimeMillis() / 1000.0);
}
```

### 6. Заміни `getState()` на:

```java
public GameState getState() {
    updateTimeLimit();
    return state;
}
```

### 7. Додай `updateTimeLimit();` на початок `calculateScore()`:

```java
public int calculateScore() {
    updateTimeLimit();
    int score = 250 - moveCount * 10;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

---

## Опціонально: показати час у 2D інтерфейсі

### Файл

```text
src/main/resources/templates/cuberoll.html
```

### Де

У блоці:

```html
<div class="status-grid">
```

поруч із `Moves` або `Current score`.

### Що вставити

```html
<div><span>Time left</span><strong th:text="|${field.remainingTimeSeconds}s|"></strong></div>
```

Наприклад:

```html
<div><span>Moves</span><strong th:text="${field.moveCount}"></strong></div>
<div><span>Time left</span><strong th:text="|${field.remainingTimeSeconds}s|"></strong></div>
<div><span>Current score</span><strong th:text="${field.calculateScore()}"></strong></div>
```

---

## Опціонально: показати час у 3D PlayCanvas HUD

Тут треба 3 маленькі зміни.

---

### 3D Крок 1 — додати поле в DTO

#### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/cuberoll3d/CubeRollStateDto.java
```

#### Де

Шукай:

```java
int remainingPainters,
int score,
```

#### Що змінити

Зроби так:

```java
int remainingPainters,
int remainingTimeSeconds,
int score,
```

---

### 3D Крок 2 — передати значення з Java API

#### Файл

```text
src/main/java/sk/tuke/gamestudio/server/web/cuberoll3d/CubeRollApiController.java
```

#### Де

У методі:

```java
private CubeRollStateDto toStateDto(WebGameSession session)
```

шукай:

```java
field.getRemainingPainters(),
field.calculateScore(),
```

#### Що змінити

Зроби так:

```java
field.getRemainingPainters(),
field.getRemainingTimeSeconds(),
field.calculateScore(),
```

---

### 3D Крок 3 — додати рядок часу в HTML

#### Файл

```text
src/main/resources/static/cuberoll-playcanvas.html
```

#### Де

У блоці:

```html
<div class="hud-status">
```

поруч із `Moves` або `Score`.

#### Що вставити

```html
<div><span>Time</span><strong id="hud-time">—</strong></div>
```

Наприклад:

```html
<div><span>Moves</span><strong id="hud-moves">—</strong></div>
<div><span>Time</span><strong id="hud-time">—</strong></div>
<div><span>Score</span><strong id="hud-score">—</strong></div>
```

---

### 3D Крок 4 — оновити текст часу в JS

#### Файл

```text
src/main/resources/static/cuberoll-playcanvas.js
```

#### Де

Шукай у `HUDController` метод, де є:

```js
$('hud-moves').textContent = String(state.moveCount);
$('hud-score').textContent = String(state.score);
```

#### Що вставити між ними

```js
$('hud-time').textContent = `${state.remainingTimeSeconds}s`;
```

Буде так:

```js
$('hud-moves').textContent = String(state.moveCount);
$('hud-time').textContent = `${state.remainingTimeSeconds}s`;
$('hud-score').textContent = String(state.score);
```

---

## Якщо хочеш, щоб час впливав на score

Наприклад, бонус за залишок часу.

### Файл

```text
Field.java
```

### Де

Метод:

```java
public int calculateScore()
```

### Варіант

```java
public int calculateScore() {
    updateTimeLimit();
    int timeBonus = getRemainingTimeSeconds() * 2;
    int score = 250 - moveCount * 10 + timeBonus;
    if (state == GameState.SOLVED) {
        score += 100;
    }
    return Math.max(score, 10);
}
```

Тут кожна залишена секунда дає `+2` score.

---

## Коротко для сесії

Для таймера майже все в одному файлі:

```text
Field.java
```

Головні вставки:

```java
private static final long TIME_LIMIT_MILLIS = 60_000L;
private long levelStartedAtMillis;
```

```java
this.levelStartedAtMillis = System.currentTimeMillis();
```

```java
updateTimeLimit();
```

```java
private void updateTimeLimit() {
    if (state == GameState.PLAYING && getRemainingTimeMillis() <= 0) {
        state = GameState.FAILED;
        lastMessage = "Failed: time limit exceeded.";
    }
}
```

Після зміни Java-коду перезапусти сервер.
*/