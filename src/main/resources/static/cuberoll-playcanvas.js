import * as pc from 'playcanvas';

const API_ROOT = '/api/cuberoll3d';
const GAME_ID = 'cuberoll';
const MAX_COMMENT_LENGTH = 300;
const PLAYER_PATTERN = /^[A-Za-z0-9_-]{2,20}$/;
const TILE_SIZE = 1.16;
const TILE_HEIGHT = 0.14;
const TILE_UNDERSIDE_HEIGHT = 0.34;
const CUBE_SIZE = 0.82;
const CUBE_CENTER_Y = TILE_HEIGHT * 0.5 + CUBE_SIZE * 0.5 + 0.055;
const GATE_HEIGHT = 0.92;
const FACE_ORDER = ['top', 'bottom', 'north', 'south', 'west', 'east'];

const FACE_COLORS = {
    RED: '#ef4444',
    BLUE: '#3b82f6',
    GREEN: '#22c55e',
    YELLOW: '#facc15',
    NONE: '#dbeafe'
};

const ROLL_ROTATION = {
    NORTH: { axis: [1, 0, 0], angle: -90 },
    SOUTH: { axis: [1, 0, 0], angle: 90 },
    EAST: { axis: [0, 0, 1], angle: -90 },
    WEST: { axis: [0, 0, 1], angle: 90 }
};

const KEY_TO_DIRECTION = {
    ArrowUp: 'NORTH',
    w: 'NORTH',
    W: 'NORTH',
    ArrowDown: 'SOUTH',
    s: 'SOUTH',
    S: 'SOUTH',
    ArrowLeft: 'WEST',
    a: 'WEST',
    A: 'WEST',
    ArrowRight: 'EAST',
    d: 'EAST',
    D: 'EAST'
};

function $(id) {
    return document.getElementById(id);
}

function textOrDash(value) {
    return value === null || value === undefined || value === '' ? '—' : String(value);
}

function normalizeServiceList(value) {
    return Array.isArray(value) ? value : [];
}

function formatServiceDate(value) {
    if (value === null || value === undefined || value === '') {
        return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '—';
    }
    return new Intl.DateTimeFormat(undefined, {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

function setHidden(element, hidden) {
    if (element) {
        element.hidden = Boolean(hidden);
    }
}

function setDisabledWithin(root, disabled) {
    if (!root) {
        return;
    }
    for (const control of root.querySelectorAll('button, input, select, textarea')) {
        control.disabled = Boolean(disabled);
    }
}

function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
}

function lerpNumber(a, b, t) {
    return a + (b - a) * t;
}

function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}

function easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
}

function easeOutBack(t) {
    const c1 = 1.70158;
    const c3 = c1 + 1;
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
}

function easeInQuad(t) {
    return t * t;
}

function rawEase(t) {
    return t;
}

function hexToColor(hex, alpha = 1) {
    const value = hex.replace('#', '');
    const r = parseInt(value.slice(0, 2), 16) / 255;
    const g = parseInt(value.slice(2, 4), 16) / 255;
    const b = parseInt(value.slice(4, 6), 16) / 255;
    return new pc.Color(r, g, b, alpha);
}

function colorNameFromCell(cellType) {
    const match = /_(RED|BLUE|GREEN|YELLOW)$/.exec(cellType || '');
    return match ? match[1] : 'NONE';
}

function vec3Clone(vec) {
    return new pc.Vec3(vec.x, vec.y, vec.z);
}

function vec3Lerp(a, b, t) {
    return new pc.Vec3(
        lerpNumber(a.x, b.x, t),
        lerpNumber(a.y, b.y, t),
        lerpNumber(a.z, b.z, t)
    );
}

function setEntityMaterial(entity, material) {
    if (!entity || !material) {
        return;
    }
    if (entity.render) {
        entity.render.material = material;
    }
    if (entity.model) {
        entity.model.material = material;
    }
}

function setMaterialOpacity(material, opacity) {
    material.opacity = clamp(opacity, 0, 1);
    material.blendType = pc.BLEND_NORMAL;
    material.depthWrite = material.opacity >= 0.92;
    material.update();
}

function cellToWorld(state, row, column) {
    return new pc.Vec3(
        (column - (state.columns - 1) / 2) * TILE_SIZE,
        0,
        (row - (state.rows - 1) / 2) * TILE_SIZE
    );
}

function normalizeStateForRendering(state) {
    if (!state) {
        return state;
    }
    const sourceCells = Array.isArray(state.cells) ? state.cells : state.board;
    const cells = Array.isArray(sourceCells)
        ? sourceCells.map((row) => Array.isArray(row) ? row.map(normalizeCellTypeForRendering) : [])
        : [];
    const rows = Number.isFinite(state.rows) ? state.rows : state.rowCount ?? cells.length;
    const columns = Number.isFinite(state.columns) ? state.columns : state.columnCount ?? cells[0]?.length ?? 0;

    return {
        ...state,
        rowCount: state.rowCount ?? rows,
        columnCount: state.columnCount ?? columns,
        rows,
        columns,
        cells
    };
}

function normalizeCellTypeForRendering(cell) {
    if (typeof cell === 'string') {
        return cell;
    }
    return cell?.type || 'VOID';
}

function cellKey(row, column) {
    return `${row},${column}`;
}

function supportsWebGL() {
    try {
        const canvas = document.createElement('canvas');
        return Boolean(
            (window.WebGL2RenderingContext && canvas.getContext('webgl2')) ||
            (window.WebGLRenderingContext && canvas.getContext('webgl'))
        );
    } catch (ignored) {
        return false;
    }
}

function createBoxEntity(app, {
    name,
    parent = null,
    position = new pc.Vec3(),
    scale = new pc.Vec3(1, 1, 1),
    material,
    castShadows = false,
    receiveShadows = false
}) {
    const entity = new pc.Entity(name);
    entity.addComponent('render', {
        type: 'box',
        material,
        castShadows,
        receiveShadows
    });
    entity.setLocalScale(scale);
    if (parent) {
        parent.addChild(entity);
        entity.setLocalPosition(position);
    } else {
        app.root.addChild(entity);
        entity.setPosition(position);
    }
    return entity;
}

class ApiClient {
    constructor(root = API_ROOT) {
        this.root = root;
    }

    async state() {
        return this.fetchJson(`${this.root}/state`);
    }

    async levels() {
        return this.fetchJson(`${this.root}/levels`);
    }

    async move(direction) {
        return this.postJson('/move', { direction });
    }

    async reset() {
        return this.postJson('/reset');
    }

    async next() {
        return this.postJson('/next');
    }

    async selectLevel(levelIndex) {
        return this.postJson('/level', { levelIndex });
    }

    async topScores() {
        return this.fetchJson(`/api/score/${encodeURIComponent(GAME_ID)}`);
    }

    async comments() {
        return this.fetchJson(`/api/comment/${encodeURIComponent(GAME_ID)}`);
    }

    async averageRating() {
        return this.fetchJson(`/api/rating/${encodeURIComponent(GAME_ID)}`);
    }

    async playerRating(player) {
        return this.fetchJson(`/api/rating/${encodeURIComponent(GAME_ID)}/${encodeURIComponent(player)}`);
    }

    async login(player) {
        return this.postForm('/cuberoll/login', { player });
    }

    async logout() {
        return this.postForm('/cuberoll/logout');
    }

    async setRating(rating) {
        return this.postForm('/cuberoll/rating', { value: String(rating) });
    }

    async addComment(comment) {
        return this.postForm('/cuberoll/comment', { comment });
    }

    async services(state = null) {
        const loggedIn = Boolean(state?.loggedIn && state?.playerName);
        const playerRatingRequest = loggedIn
            ? this.playerRating(state.playerName)
            : Promise.resolve(0);
        const [topScores, comments, averageRating, playerRating] = await Promise.all([
            this.topScores(),
            this.comments(),
            this.averageRating(),
            playerRatingRequest
        ]);
        return {
            topScores: normalizeServiceList(topScores),
            comments: normalizeServiceList(comments),
            averageRating: Number.isFinite(Number(averageRating)) ? Number(averageRating) : 0,
            playerRating: Number.isFinite(Number(playerRating)) ? Number(playerRating) : 0
        };
    }

    async fetchJson(url, options = {}) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            ...options
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(`${response.status} ${response.statusText}: ${text || url}`);
        }
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    async postJson(path, body = undefined) {
        const options = { method: 'POST' };
        if (body !== undefined) {
            options.headers = { 'Content-Type': 'application/json' };
            options.body = JSON.stringify(body);
        }
        return this.fetchJson(`${this.root}${path}`, options);
    }

    async postForm(path, values = {}) {
        const response = await fetch(path, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: new URLSearchParams(values)
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(`${response.status} ${response.statusText}: ${text || path}`);
        }
        return response.text();
    }
}

class MaterialFactory {
    constructor() {
        this.materials = {};
        this.build();
    }

    standard(name, diffuseHex, options = {}) {
        const material = new pc.StandardMaterial();
        material.name = name;
        material.diffuse = hexToColor(diffuseHex, options.opacity ?? 1);
        material.useLighting = options.useLighting ?? true;
        material.shininess = options.shininess ?? 42;
        if (typeof options.metalness === 'number') {
            material.useMetalness = true;
            material.metalness = options.metalness;
        }
        if (options.emissiveHex) {
            material.emissive = hexToColor(options.emissiveHex);
            material.emissiveIntensity = options.emissiveIntensity ?? 0.45;
        }
        if (typeof options.opacity === 'number' && options.opacity < 1) {
            material.opacity = options.opacity;
            material.blendType = pc.BLEND_NORMAL;
            material.depthWrite = false;
        }
        material.update();
        return material;
    }

    build() {
        this.materials.floorTop = this.standard('tile-top-cool-metal', '#56677f', {
            shininess: 76,
            metalness: 0.18
        });
        this.materials.floorInset = this.standard('tile-inset', '#7389a5', {
            shininess: 90,
            metalness: 0.12
        });
        this.materials.floorSide = this.standard('tile-side-dark', '#1e293b', {
            shininess: 28
        });
        this.materials.tileRim = this.standard('tile-rim-cyan', '#6ee7f9', {
            emissiveHex: '#38bdf8',
            emissiveIntensity: 0.32,
            opacity: 0.82,
            shininess: 92
        });
        this.materials.platformShadow = this.standard('floating-platform-shadow', '#020617', {
            emissiveHex: '#0f172a',
            emissiveIntensity: 0.08,
            opacity: 0.42
        });
        this.materials.boardGlow = this.standard('board-underglow', '#164e63', {
            emissiveHex: '#22d3ee',
            emissiveIntensity: 0.48,
            opacity: 0.18
        });
        this.materials.finish = this.standard('finish-energy-core', '#34d399', {
            emissiveHex: '#86efac',
            emissiveIntensity: 1.15,
            shininess: 96
        });
        this.materials.finishGlow = this.standard('finish-glass-glow', '#86efac', {
            emissiveHex: '#86efac',
            emissiveIntensity: 1.85,
            opacity: 0.38,
            shininess: 100
        });
        this.materials.finishLocked = this.standard('finish-locked-pulse', '#fb7185', {
            emissiveHex: '#fb7185',
            emissiveIntensity: 1.5,
            opacity: 0.36
        });
        this.materials.cubeBody = this.standard('cube-ceramic-body', '#f8fafc', {
            shininess: 96,
            metalness: 0.04
        });
        this.materials.cubeFailed = this.standard('cube-failed-warning', '#fca5a5', {
            emissiveHex: '#ef4444',
            emissiveIntensity: 0.32,
            shininess: 88,
            metalness: 0.03
        });
        this.materials.cubeSolved = this.standard('cube-solved-success', '#dcfce7', {
            emissiveHex: '#86efac',
            emissiveIntensity: 0.28,
            shininess: 96,
            metalness: 0.04
        });
        this.materials.cubeBezel = this.standard('cube-sticker-bezel', '#0f172a', {
            shininess: 30
        });
        this.materials.cubeGlowShell = this.standard('cube-soft-shell', '#bae6fd', {
            emissiveHex: '#38bdf8',
            emissiveIntensity: 0.20,
            opacity: 0.13
        });
        this.materials.wrongGate = this.standard('wrong-gate-flash', '#fb7185', {
            emissiveHex: '#fb7185',
            emissiveIntensity: 1.75,
            opacity: 0.54
        });
        this.materials.star = this.standard('star-pin', '#e0f2fe', {
            emissiveHex: '#dbeafe',
            emissiveIntensity: 1.2,
            useLighting: false
        });
        this.materials.confetti = {};
        this.materials.face = {};
        this.materials.painter = {};
        this.materials.painterGlow = {};
        this.materials.gate = {};
        this.materials.gateShell = {};
        this.materials.gateLight = {};

        for (const [colorName, colorHex] of Object.entries(FACE_COLORS)) {
            this.materials.face[colorName] = this.standard(`cube-face-${colorName.toLowerCase()}`, colorHex, {
                emissiveHex: colorName === 'NONE' ? null : colorHex,
                emissiveIntensity: colorName === 'NONE' ? 0 : 0.34,
                shininess: 86
            });
            if (colorName !== 'NONE') {
                this.materials.confetti[colorName] = this.standard(`confetti-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 0.72,
                    shininess: 80
                });
                this.materials.painter[colorName] = this.standard(`painter-pad-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 1.15,
                    shininess: 92
                });
                this.materials.painterGlow[colorName] = this.standard(`painter-glow-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 2.0,
                    opacity: 0.34
                });
                this.materials.gate[colorName] = this.standard(`gate-core-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 0.36,
                    shininess: 86,
                    metalness: 0.14
                });
                this.materials.gateShell[colorName] = this.standard(`gate-shell-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 0.95,
                    opacity: 0.26
                });
                this.materials.gateLight[colorName] = this.standard(`gate-light-${colorName.toLowerCase()}`, colorHex, {
                    emissiveHex: colorHex,
                    emissiveIntensity: 1.55,
                    shininess: 100
                });
            }
        }
    }

    face(colorName) {
        return this.materials.face[colorName] || this.materials.face.NONE;
    }

    colorMaterialSet(colorName) {
        const normalized = colorName || 'RED';
        return {
            painter: this.materials.painter[normalized],
            painterGlow: this.materials.painterGlow[normalized],
            gate: this.materials.gate[normalized],
            gateShell: this.materials.gateShell[normalized],
            gateLight: this.materials.gateLight[normalized],
            confetti: this.materials.confetti[normalized]
        };
    }
}

class PlayCanvasRuntime {
    constructor(canvas, materials) {
        this.canvas = canvas;
        this.materials = materials;
        this.time = 0;
        this.cameraBase = null;
        this.shakeRemaining = 0;
        this.shakeDuration = 0;
        this.shakeAmount = 0;

        this.app = new pc.Application(this.canvas);
        this.app.graphicsDevice.maxPixelRatio = Math.min(window.devicePixelRatio || 1, 1.8);
        this.app.setCanvasFillMode(pc.FILLMODE_FILL_WINDOW);
        this.app.setCanvasResolution(pc.RESOLUTION_AUTO);
        this.app.scene.ambientLight = new pc.Color(0.24, 0.31, 0.43);
        this.app.start();

        this.createCamera();
        this.createLights();
        this.createStarfield();

        this.app.on('update', (dt) => this.update(dt));
        this.cameraState = null;
        this.resizeTimer = 0;
        const scheduleCameraRefresh = () => {
            window.clearTimeout(this.resizeTimer);
            this.resizeTimer = window.setTimeout(() => {
                this.app.resizeCanvas();
                if (this.cameraState) {
                    this.updateCameraForState(this.cameraState);
                }
            }, 80);
        };
        window.addEventListener('resize', scheduleCameraRefresh);
        window.addEventListener('orientationchange', scheduleCameraRefresh);
        window.addEventListener('cuberoll-panels-changed', scheduleCameraRefresh);
    }

    createCamera() {
        this.camera = new pc.Entity('camera');
        this.camera.addComponent('camera', {
            clearColor: new pc.Color(0.006, 0.011, 0.024),
            fov: 44,
            nearClip: 0.08,
            farClip: 140
        });
        this.app.root.addChild(this.camera);
    }

    createLights() {
        this.keyLight = new pc.Entity('key-light');
        this.keyLight.addComponent('light', {
            type: 'directional',
            color: new pc.Color(1.0, 0.96, 0.88),
            intensity: 1.52,
            castShadows: true,
            shadowDistance: 28,
            shadowResolution: 2048,
            shadowBias: 0.08,
            normalOffsetBias: 0.04
        });
        this.keyLight.setEulerAngles(43, -34, 0);
        this.app.root.addChild(this.keyLight);

        this.fillLight = new pc.Entity('cyan-fill-light');
        this.fillLight.addComponent('light', {
            type: 'omni',
            color: new pc.Color(0.30, 0.78, 1.0),
            intensity: 0.34,
            range: 18
        });
        this.fillLight.setPosition(-5.5, 4.8, 4.2);
        this.app.root.addChild(this.fillLight);

        this.rimLight = new pc.Entity('violet-rim-light');
        this.rimLight.addComponent('light', {
            type: 'omni',
            color: new pc.Color(0.60, 0.46, 1.0),
            intensity: 0.22,
            range: 22
        });
        this.rimLight.setPosition(5, 5.8, -7);
        this.app.root.addChild(this.rimLight);
    }

    createStarfield() {
        this.starRoot = new pc.Entity('starfield');
        this.app.root.addChild(this.starRoot);
        let seed = 17;
        const random = () => {
            seed = (seed * 1664525 + 1013904223) >>> 0;
            return seed / 0xffffffff;
        };
        for (let i = 0; i < 90; i++) {
            const x = (random() - 0.5) * 42;
            const y = 1.2 + random() * 18;
            const z = -14 - random() * 34;
            const size = 0.025 + random() * 0.05;
            const star = createBoxEntity(this.app, {
                name: `star-${i}`,
                parent: this.starRoot,
                position: new pc.Vec3(x, y, z),
                scale: new pc.Vec3(size, size, size),
                material: this.materials.materials.star
            });
            star.setEulerAngles(random() * 180, random() * 180, random() * 180);
        }
    }

    getSceneLayout() {
        const width = Math.max(320, window.innerWidth || this.canvas.clientWidth || 1280);
        const height = Math.max(320, window.innerHeight || this.canvas.clientHeight || 720);
        const shell = $('pc-shell');
        const hud = $('hud');
        const community = $('community-panel');
        const isMobile = width <= 920;
        let leftReserve = 0;
        let rightReserve = 0;
        let topReserve = 0;
        let bottomReserve = 0;

        if (isMobile) {
            topReserve = 54;
            bottomReserve = 124;
        } else if (shell) {
            if (!shell.classList.contains('hud-closed') && hud && !hud.classList.contains('is-panel-hidden')) {
                const rect = hud.getBoundingClientRect();
                if (rect.width > 0) {
                    leftReserve = Math.max(0, rect.right + 28);
                }
            }
            if (!shell.classList.contains('community-closed') && community && !community.classList.contains('is-panel-hidden')) {
                const rect = community.getBoundingClientRect();
                if (rect.width > 0) {
                    rightReserve = Math.max(0, width - rect.left + 28);
                }
            }
        }

        const usableWidth = Math.max(340, width - leftReserve - rightReserve);
        const usableHeight = Math.max(260, height - topReserve - bottomReserve);
        const shiftRatio = clamp((rightReserve - leftReserve) / width, -0.46, 0.46);
        return { width, height, usableWidth, usableHeight, shiftRatio, isMobile };
    }

    updateCameraForState(state) {
        this.cameraState = state;
        const layout = this.getSceneLayout();
        const boardWidth = Math.max(1, state.columns * TILE_SIZE);
        const boardDepth = Math.max(1, state.rows * TILE_SIZE);
        const largest = Math.max(boardWidth, boardDepth);
        const cellLargest = Math.max(state.rows, state.columns);
        const usableAspect = layout.usableWidth / layout.usableHeight;
        const widthPressure = Math.sqrt(layout.width / layout.usableWidth);
        const heightPressure = Math.sqrt(layout.height / layout.usableHeight);
        const panelZoom = clamp(Math.max(widthPressure, heightPressure), 1, 1.42);
        const portraitZoom = usableAspect < 0.85 ? clamp(1.20 - usableAspect * 0.12, 1.05, 1.18) : 1;
        const phoneZoom = layout.isMobile ? 1.12 : 1;
        const zoom = panelZoom * portraitZoom * phoneZoom;
        const targetShift = clamp(layout.shiftRatio * largest * 1.10, -largest * 0.52, largest * 0.52);
        const baseX = clamp((state.columns - state.rows) * 0.025, -0.75, 0.75);

        if (this.camera.camera) {
            this.camera.camera.fov = layout.isMobile ? 52 : (layout.usableWidth < 720 ? 49 : 44);
        }

        this.cameraBase = {
            x: baseX + targetShift,
            y: clamp((cellLargest * 0.66 + 2.0) * zoom, 5.8, 16.0),
            z: clamp((cellLargest * (usableAspect < 1 ? 1.18 : 1.02) + 3.25) * zoom, 7.4, 26.0),
            targetX: targetShift,
            targetY: 0,
            targetZ: 0
        };
        this.fillLight.setPosition(-cellLargest * 0.42, 4.7, cellLargest * 0.36);
        this.rimLight.setPosition(cellLargest * 0.34, 5.6, -cellLargest * 0.72);
        this.placeCamera();
    }

    shake(amount = 0.05, duration = 0.42) {
        this.shakeAmount = Math.max(this.shakeAmount, amount);
        this.shakeRemaining = Math.max(this.shakeRemaining, duration);
        this.shakeDuration = Math.max(this.shakeDuration, duration);
    }

    update(dt) {
        this.time += dt;
        this.placeCamera(dt);
        this.keyLight.setEulerAngles(43 + Math.sin(this.time * 0.18) * 1.4, -34 + Math.cos(this.time * 0.15) * 1.8, 0);
    }

    placeCamera(dt = 0) {
        if (!this.cameraBase) {
            this.camera.setPosition(0, 7.2, 9.2);
            this.camera.lookAt(0, 0, 0);
            return;
        }

        let shakeX = 0;
        let shakeY = 0;
        if (this.shakeRemaining > 0) {
            const fade = this.shakeDuration <= 0 ? 0 : this.shakeRemaining / this.shakeDuration;
            shakeX = Math.sin(this.time * 58) * this.shakeAmount * fade;
            shakeY = Math.cos(this.time * 73) * this.shakeAmount * 0.55 * fade;
            this.shakeRemaining = Math.max(0, this.shakeRemaining - dt);
        }

        const swayX = Math.sin(this.time * 0.16) * 0.08;
        const swayY = Math.sin(this.time * 0.22) * 0.04;
        this.camera.setPosition(
            this.cameraBase.x + swayX + shakeX,
            this.cameraBase.y + swayY + shakeY,
            this.cameraBase.z
        );
        this.camera.lookAt(this.cameraBase.targetX, this.cameraBase.targetY, this.cameraBase.targetZ);
    }
}

class BoardRenderer {
    constructor(app, materials) {
        this.app = app;
        this.materials = materials;
        this.root = null;
        this.state = null;
        this.actors = new Map();
        this.idleItems = [];
        this.time = 0;
        this.app.on('update', (dt) => this.update(dt));
    }

    rebuild(state) {
        this.destroy();
        this.state = state;
        this.root = new pc.Entity('board-root');
        this.app.root.addChild(this.root);
        this.createPlatformHalo(state);

        for (let row = 0; row < state.rows; row++) {
            for (let column = 0; column < state.columns; column++) {
                this.createCell(row, column, state.cells[row][column]);
            }
        }
    }

    destroy() {
        if (this.root) {
            this.root.destroy();
        }
        this.root = null;
        this.state = null;
        this.actors.clear();
        this.idleItems = [];
    }

    actorAt(row, column) {
        return this.actors.get(cellKey(row, column));
    }

    getWorldPosition(row, column) {
        return cellToWorld(this.state, row, column);
    }

    createPlatformHalo(state) {
        for (let row = 0; row < state.rows; row++) {
            for (let column = 0; column < state.columns; column++) {
                if (state.cells[row][column] === 'VOID') {
                    continue;
                }

                const position = cellToWorld(state, row, column);
                createBoxEntity(this.app, {
                    name: `cell-shadow-plate-${row}-${column}`,
                    parent: this.root,
                    position: new pc.Vec3(position.x, -0.56, position.z),
                    scale: new pc.Vec3(TILE_SIZE * 0.92, 0.035, TILE_SIZE * 0.92),
                    material: this.materials.materials.platformShadow,
                    receiveShadows: true
                });
                createBoxEntity(this.app, {
                    name: `cell-underglow-plate-${row}-${column}`,
                    parent: this.root,
                    position: new pc.Vec3(position.x, -0.38, position.z),
                    scale: new pc.Vec3(TILE_SIZE * 0.82, 0.025, TILE_SIZE * 0.82),
                    material: this.materials.materials.boardGlow
                });
            }
        }
    }

    createCell(row, column, cellType) {
        if (cellType === 'VOID') {
            return;
        }

        const position = this.getWorldPosition(row, column);
        const actor = {
            row,
            column,
            cellType,
            colorName: colorNameFromCell(cellType),
            base: null,
            top: null,
            accent: null,
            painter: null,
            glow: null,
            finish: null,
            gate: null,
            gateShell: null,
            parts: []
        };
        this.actors.set(cellKey(row, column), actor);

        actor.base = createBoxEntity(this.app, {
            name: `tile-side-${row}-${column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, -TILE_UNDERSIDE_HEIGHT * 0.42, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.92, TILE_UNDERSIDE_HEIGHT, TILE_SIZE * 0.92),
            material: this.materials.materials.floorSide,
            castShadows: true,
            receiveShadows: true
        });
        actor.top = createBoxEntity(this.app, {
            name: `tile-top-${row}-${column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, 0, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.90, TILE_HEIGHT, TILE_SIZE * 0.90),
            material: this.materials.materials.floorTop,
            castShadows: true,
            receiveShadows: true
        });
        actor.parts.push(actor.base, actor.top);
        this.createTileRims(actor, position);

        if (cellType === 'FINISH') {
            this.createFinish(actor, position);
        } else if (cellType.startsWith('PAINTER_')) {
            this.createPainter(actor, position);
        } else if (cellType.startsWith('GATE_')) {
            this.createGate(actor, position);
        } else {
            const inset = createBoxEntity(this.app, {
                name: `tile-inset-${row}-${column}`,
                parent: this.root,
                position: new pc.Vec3(position.x, TILE_HEIGHT * 0.62, position.z),
                scale: new pc.Vec3(TILE_SIZE * 0.48, 0.018, TILE_SIZE * 0.48),
                material: this.materials.materials.floorInset,
                receiveShadows: true
            });
            actor.parts.push(inset);
        }
    }

    createTileRims(actor, position) {
        const rimMaterial = this.materials.materials.tileRim;
        const y = TILE_HEIGHT * 0.72;
        const long = TILE_SIZE * 0.74;
        const offset = TILE_SIZE * 0.43;
        const strip = 0.026;
        const rims = [
            { suffix: 'north', position: new pc.Vec3(position.x, y, position.z - offset), scale: new pc.Vec3(long, strip, strip) },
            { suffix: 'south', position: new pc.Vec3(position.x, y, position.z + offset), scale: new pc.Vec3(long, strip, strip) },
            { suffix: 'west', position: new pc.Vec3(position.x - offset, y, position.z), scale: new pc.Vec3(strip, strip, long) },
            { suffix: 'east', position: new pc.Vec3(position.x + offset, y, position.z), scale: new pc.Vec3(strip, strip, long) }
        ];
        for (const rim of rims) {
            actor.parts.push(createBoxEntity(this.app, {
                name: `tile-rim-${rim.suffix}-${actor.row}-${actor.column}`,
                parent: this.root,
                position: rim.position,
                scale: rim.scale,
                material: rimMaterial
            }));
        }
    }

    createFinish(actor, position) {
        actor.finish = createBoxEntity(this.app, {
            name: `finish-core-${actor.row}-${actor.column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 0.96, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.60, 0.075, TILE_SIZE * 0.60),
            material: this.materials.materials.finish,
            castShadows: true,
            receiveShadows: true
        });
        actor.glow = createBoxEntity(this.app, {
            name: `finish-glow-${actor.row}-${actor.column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 1.28, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.98, 0.024, TILE_SIZE * 0.98),
            material: this.materials.materials.finishGlow
        });
        const beaconOffset = TILE_SIZE * 0.33;
        for (let i = 0; i < 4; i++) {
            const sx = i < 2 ? -beaconOffset : beaconOffset;
            const sz = i % 2 === 0 ? -beaconOffset : beaconOffset;
            actor.parts.push(createBoxEntity(this.app, {
                name: `finish-beacon-${i}-${actor.row}-${actor.column}`,
                parent: this.root,
                position: new pc.Vec3(position.x + sx, TILE_HEIGHT * 1.42, position.z + sz),
                scale: new pc.Vec3(0.065, 0.20, 0.065),
                material: this.materials.materials.finishGlow
            }));
        }
        actor.accent = actor.finish;
        actor.parts.push(actor.finish, actor.glow);
        this.idleItems.push({ type: 'finish', actor, baseGlow: vec3Clone(actor.glow.getLocalScale()) });
    }

    createPainter(actor, position) {
        const materials = this.materials.colorMaterialSet(actor.colorName);
        actor.painter = createBoxEntity(this.app, {
            name: `painter-pad-${actor.row}-${actor.column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 1.02, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.63, 0.072, TILE_SIZE * 0.63),
            material: materials.painter,
            castShadows: true,
            receiveShadows: true
        });
        actor.glow = createBoxEntity(this.app, {
            name: `painter-glow-${actor.row}-${actor.column}`,
            parent: this.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 1.32, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.88, 0.026, TILE_SIZE * 0.88),
            material: materials.painterGlow
        });
        actor.accent = actor.painter;
        actor.parts.push(actor.painter, actor.glow);
        this.idleItems.push({ type: 'painter', actor, baseGlow: vec3Clone(actor.glow.getLocalScale()) });
    }

    createGate(actor, position) {
        const materials = this.materials.colorMaterialSet(actor.colorName);
        const gate = new pc.Entity(`gate-root-${actor.row}-${actor.column}`);
        gate.setLocalPosition(position.x, TILE_HEIGHT * 0.66, position.z);
        this.root.addChild(gate);
        actor.gate = gate;
        actor.parts.push(gate);

        createBoxEntity(this.app, {
            name: `gate-core-${actor.row}-${actor.column}`,
            parent: gate,
            position: new pc.Vec3(0, GATE_HEIGHT * 0.5, 0),
            scale: new pc.Vec3(TILE_SIZE * 0.66, GATE_HEIGHT, TILE_SIZE * 0.66),
            material: materials.gate,
            castShadows: true,
            receiveShadows: true
        });
        actor.gateShell = createBoxEntity(this.app, {
            name: `gate-shell-${actor.row}-${actor.column}`,
            parent: gate,
            position: new pc.Vec3(0, GATE_HEIGHT * 0.52, 0),
            scale: new pc.Vec3(TILE_SIZE * 0.82, GATE_HEIGHT * 1.06, TILE_SIZE * 0.82),
            material: materials.gateShell
        });
        createBoxEntity(this.app, {
            name: `gate-top-light-${actor.row}-${actor.column}`,
            parent: gate,
            position: new pc.Vec3(0, GATE_HEIGHT + 0.06, 0),
            scale: new pc.Vec3(TILE_SIZE * 0.78, 0.08, TILE_SIZE * 0.78),
            material: materials.gateLight,
            castShadows: true
        });
        const postOffset = TILE_SIZE * 0.37;
        for (let i = 0; i < 4; i++) {
            createBoxEntity(this.app, {
                name: `gate-post-${i}-${actor.row}-${actor.column}`,
                parent: gate,
                position: new pc.Vec3(i < 2 ? -postOffset : postOffset, GATE_HEIGHT * 0.54, i % 2 === 0 ? -postOffset : postOffset),
                scale: new pc.Vec3(0.075, GATE_HEIGHT * 1.08, 0.075),
                material: materials.gateLight,
                castShadows: true
            });
        }
        actor.accent = gate;
    }

    update(dt) {
        this.time += dt;
        for (const item of this.idleItems) {
            if (!item.actor?.glow) {
                continue;
            }
            const phase = this.time * (item.type === 'finish' ? 2.2 : 2.8) + item.actor.row * 0.22 + item.actor.column * 0.14;
            const pulse = 1 + Math.sin(phase) * (item.type === 'finish' ? 0.055 : 0.075);
            item.actor.glow.setLocalScale(item.baseGlow.x * pulse, item.baseGlow.y, item.baseGlow.z * pulse);
        }
    }
}

class CubeRenderer {
    constructor(app, materials) {
        this.app = app;
        this.materials = materials;
        this.root = null;
        this.body = null;
        this.stickers = {};
        this.state = null;
    }

    rebuild(state, options = {}) {
        this.destroy();
        this.state = state;
        this.root = new pc.Entity('cube-root');
        const position = cellToWorld(state, state.playerRow, state.playerColumn);
        this.root.setPosition(position.x, CUBE_CENTER_Y, position.z);
        this.app.root.addChild(this.root);

        createBoxEntity(this.app, {
            name: 'cube-soft-glow-shell',
            parent: this.root,
            scale: new pc.Vec3(CUBE_SIZE * 1.07, CUBE_SIZE * 1.07, CUBE_SIZE * 1.07),
            material: this.materials.materials.cubeGlowShell
        });
        this.body = createBoxEntity(this.app, {
            name: 'cube-body',
            parent: this.root,
            scale: new pc.Vec3(CUBE_SIZE, CUBE_SIZE, CUBE_SIZE),
            material: this.materialForState(state.gameState),
            castShadows: true,
            receiveShadows: true
        });

        this.createStickerPair('top', new pc.Vec3(0, CUBE_SIZE * 0.512, 0), new pc.Vec3(CUBE_SIZE * 0.66, 0.018, CUBE_SIZE * 0.66), new pc.Vec3(0, CUBE_SIZE * 0.536, 0), new pc.Vec3(CUBE_SIZE * 0.55, 0.022, CUBE_SIZE * 0.55));
        this.createStickerPair('bottom', new pc.Vec3(0, -CUBE_SIZE * 0.512, 0), new pc.Vec3(CUBE_SIZE * 0.66, 0.018, CUBE_SIZE * 0.66), new pc.Vec3(0, -CUBE_SIZE * 0.536, 0), new pc.Vec3(CUBE_SIZE * 0.55, 0.022, CUBE_SIZE * 0.55));
        this.createStickerPair('north', new pc.Vec3(0, 0, -CUBE_SIZE * 0.512), new pc.Vec3(CUBE_SIZE * 0.66, CUBE_SIZE * 0.66, 0.018), new pc.Vec3(0, 0, -CUBE_SIZE * 0.536), new pc.Vec3(CUBE_SIZE * 0.55, CUBE_SIZE * 0.55, 0.022));
        this.createStickerPair('south', new pc.Vec3(0, 0, CUBE_SIZE * 0.512), new pc.Vec3(CUBE_SIZE * 0.66, CUBE_SIZE * 0.66, 0.018), new pc.Vec3(0, 0, CUBE_SIZE * 0.536), new pc.Vec3(CUBE_SIZE * 0.55, CUBE_SIZE * 0.55, 0.022));
        this.createStickerPair('west', new pc.Vec3(-CUBE_SIZE * 0.512, 0, 0), new pc.Vec3(0.018, CUBE_SIZE * 0.66, CUBE_SIZE * 0.66), new pc.Vec3(-CUBE_SIZE * 0.536, 0, 0), new pc.Vec3(0.022, CUBE_SIZE * 0.55, CUBE_SIZE * 0.55));
        this.createStickerPair('east', new pc.Vec3(CUBE_SIZE * 0.512, 0, 0), new pc.Vec3(0.018, CUBE_SIZE * 0.66, CUBE_SIZE * 0.66), new pc.Vec3(CUBE_SIZE * 0.536, 0, 0), new pc.Vec3(0.022, CUBE_SIZE * 0.55, CUBE_SIZE * 0.55));

        this.updateStickers(state.cubeFaces);
        if (options.hidden) {
            this.root.enabled = false;
        }
    }

    destroy() {
        if (this.root) {
            this.root.destroy();
        }
        this.root = null;
        this.body = null;
        this.stickers = {};
    }

    materialForState(gameState) {
        if (gameState === 'FAILED') {
            return this.materials.materials.cubeFailed;
        }
        if (gameState === 'SOLVED') {
            return this.materials.materials.cubeSolved;
        }
        return this.materials.materials.cubeBody;
    }

    createStickerPair(name, bezelPosition, bezelScale, stickerPosition, stickerScale) {
        createBoxEntity(this.app, {
            name: `sticker-bezel-${name}`,
            parent: this.root,
            position: bezelPosition,
            scale: bezelScale,
            material: this.materials.materials.cubeBezel,
            receiveShadows: true
        });
        this.stickers[name] = createBoxEntity(this.app, {
            name: `sticker-${name}`,
            parent: this.root,
            position: stickerPosition,
            scale: stickerScale,
            material: this.materials.face('NONE')
        });
    }

    updateStickers(cubeFaces) {
        for (const face of FACE_ORDER) {
            setEntityMaterial(this.stickers[face], this.materials.face(cubeFaces?.[face] || 'NONE'));
        }
    }

    setRootPosition(position) {
        if (this.root) {
            this.root.setPosition(position);
        }
    }

    setIdentityRotation() {
        if (this.root) {
            this.root.setRotation(new pc.Quat());
            this.root.setLocalScale(1, 1, 1);
        }
    }
}

class AnimationController {
    constructor(app, runtime, boardRenderer, cubeRenderer, materials) {
        this.app = app;
        this.runtime = runtime;
        this.board = boardRenderer;
        this.cube = cubeRenderer;
        this.materials = materials;
        this.tweens = [];
        this.generation = 0;
        this.app.on('update', (dt) => this.update(dt));
    }

    begin() {
        this.generation += 1;
        return this.generation;
    }

    isCurrent(token) {
        return token === this.generation;
    }

    cancelAll() {
        this.generation += 1;
        for (const tween of this.tweens) {
            tween.cancelled = true;
            tween.resolve(false);
        }
        this.tweens = [];
    }

    update(dt) {
        if (this.tweens.length === 0) {
            return;
        }
        const finished = [];
        for (const tween of this.tweens) {
            if (tween.cancelled || !this.isCurrent(tween.token)) {
                finished.push(tween);
                tween.resolve(false);
                continue;
            }
            tween.elapsed += dt;
            const raw = clamp(tween.elapsed / tween.duration, 0, 1);
            const eased = tween.ease(raw);
            tween.update(eased, raw);
            if (raw >= 1) {
                finished.push(tween);
                tween.update(1, 1);
                tween.resolve(true);
            }
        }
        this.tweens = this.tweens.filter((tween) => !finished.includes(tween));
    }

    tween(duration, update, ease = easeInOutCubic, token = this.generation) {
        if (!this.isCurrent(token)) {
            return Promise.resolve(false);
        }
        return new Promise((resolve) => {
            this.tweens.push({ duration: Math.max(0.001, duration), elapsed: 0, update, ease, resolve, token, cancelled: false });
        });
    }

    async animateOutcome(outcome, newState, token) {
        if (!outcome || !this.isCurrent(token)) {
            return false;
        }
        const effects = new Set(outcome.effects || []);
        if (effects.has('IGNORED_NOT_PLAYING')) {
            return true;
        }

        if (outcome.rolled && this.cube.root) {
            const rolled = await this.animateRoll(outcome, effects, token);
            if (!rolled) {
                return false;
            }
        }

        const followUps = [];
        if (effects.has('PAINTER_USED')) {
            followUps.push(this.animatePainterPulse(outcome, token));
        }
        if (effects.has('GATE_OPENED')) {
            followUps.push(this.animateGateOpen(outcome, token));
        }
        if (effects.has('WRONG_GATE_COLOR')) {
            followUps.push(this.animateWrongGate(outcome, token));
        }
        if (effects.has('FELL')) {
            followUps.push(this.animateVoidFall(token));
        }
        if (effects.has('FINISH_LOCKED')) {
            followUps.push(this.animateFinishLocked(outcome, token));
        }

        const completed = await Promise.all(followUps);
        if (completed.some((value) => value === false) || !this.isCurrent(token)) {
            return false;
        }

        if (effects.has('SOLVED')) {
            return this.animateSolved(newState, token);
        }
        return this.isCurrent(token);
    }

    async animateRoll(outcome, effects, token) {
        const state = this.board.state;
        const spec = ROLL_ROTATION[outcome.direction] || ROLL_ROTATION.NORTH;
        const startCell = cellToWorld(state, outcome.fromRow, outcome.fromColumn);
        const fell = effects.has('FELL');
        const wrongGate = effects.has('WRONG_GATE_COLOR');
        const targetRow = outcome.playerMoved ? outcome.toRow : outcome.attemptedRow;
        const targetColumn = outcome.playerMoved ? outcome.toColumn : outcome.attemptedColumn;
        const targetCell = cellToWorld(state, targetRow, targetColumn);
        const start = new pc.Vec3(startCell.x, CUBE_CENTER_Y, startCell.z);
        const target = new pc.Vec3(targetCell.x, CUBE_CENTER_Y, targetCell.z);
        const axis = new pc.Vec3(spec.axis[0], spec.axis[1], spec.axis[2]);
        const root = this.cube.root;

        root.enabled = true;
        root.setPosition(start);
        root.setRotation(new pc.Quat());
        root.setLocalScale(1, 1, 1);

        const duration = fell ? 0.36 : wrongGate ? 0.31 : 0.42;
        const rolled = await this.tween(duration, (t, raw) => {
            const travel = wrongGate ? Math.sin(Math.PI * t) * 0.30 : t;
            const position = vec3Lerp(start, target, travel);
            const arc = Math.sin(Math.PI * t) * (wrongGate ? 0.075 : 0.17);
            position.y += arc;
            const rotation = new pc.Quat().setFromAxisAngle(axis, spec.angle * t);
            root.setPosition(position);
            root.setRotation(rotation);
        }, easeInOutCubic, token);
        if (!rolled || !this.isCurrent(token)) {
            return false;
        }

        if (wrongGate) {
            this.runtime.shake(0.055, 0.36);
            const base = vec3Clone(root.getPosition());
            return this.tween(0.22, (t, raw) => {
                const shake = Math.sin(raw * Math.PI * 8) * 0.055 * (1 - raw);
                root.setPosition(base.x + shake, base.y, base.z);
            }, rawEase, token);
        }

        if (!fell) {
            const landingPosition = target;
            const bounced = await this.tween(0.16, (t) => {
                const bounce = Math.sin(Math.PI * t) * 0.055;
                const squash = Math.sin(Math.PI * t) * 0.035;
                root.setPosition(landingPosition.x, CUBE_CENTER_Y + bounce, landingPosition.z);
                root.setLocalScale(1 + squash * 0.35, 1 - squash, 1 + squash * 0.35);
            }, easeOutCubic, token);
            root.setLocalScale(1, 1, 1);
            return bounced;
        }
        return true;
    }

    async animatePainterPulse(outcome, token) {
        const actor = this.board.actorAt(outcome.toRow, outcome.toColumn);
        if (!actor) {
            return true;
        }
        const position = this.board.getWorldPosition(outcome.toRow, outcome.toColumn);
        const colorSet = this.materials.colorMaterialSet(actor.colorName);
        const pulseMaterial = colorSet.painterGlow.clone();
        const ring = createBoxEntity(this.app, {
            name: 'painter-pulse-ring',
            parent: this.board.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 1.43, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.54, 0.018, TILE_SIZE * 0.54),
            material: pulseMaterial
        });
        const padScale = actor.painter ? vec3Clone(actor.painter.getLocalScale()) : null;
        const glowScale = actor.glow ? vec3Clone(actor.glow.getLocalScale()) : null;
        const finished = await this.tween(0.46, (t, raw) => {
            const pulse = Math.sin(Math.PI * raw);
            if (actor.painter && padScale) {
                actor.painter.setLocalScale(padScale.x * (1 + pulse * 0.22), padScale.y * (1 + pulse * 0.45), padScale.z * (1 + pulse * 0.22));
            }
            if (actor.glow && glowScale) {
                actor.glow.setLocalScale(glowScale.x * (1 + pulse * 0.52), glowScale.y, glowScale.z * (1 + pulse * 0.52));
            }
            ring.setLocalScale(TILE_SIZE * (0.54 + raw * 0.62), 0.018, TILE_SIZE * (0.54 + raw * 0.62));
            setMaterialOpacity(pulseMaterial, 0.42 * (1 - raw));
        }, easeOutCubic, token);
        if (actor.painter && padScale) {
            actor.painter.setLocalScale(padScale);
        }
        if (actor.glow && glowScale) {
            actor.glow.setLocalScale(glowScale);
        }
        ring.destroy();
        return finished;
    }

    async animateGateOpen(outcome, token) {
        const actor = this.board.actorAt(outcome.toRow, outcome.toColumn);
        if (!actor?.gate) {
            return true;
        }
        const gate = actor.gate;
        const startScale = vec3Clone(gate.getLocalScale());
        const startPosition = vec3Clone(gate.getLocalPosition());
        const position = this.board.getWorldPosition(outcome.toRow, outcome.toColumn);
        const sparks = this.createSparks(position, actor.colorName, 14);

        const opened = await this.tween(0.56, (t, raw) => {
            const height = Math.max(0.04, startScale.y * (1 - t));
            const spread = 1 + Math.sin(Math.PI * raw) * 0.10;
            gate.setLocalScale(startScale.x * spread, height, startScale.z * spread);
            gate.setLocalPosition(startPosition.x, startPosition.y - GATE_HEIGHT * 0.38 * t, startPosition.z);
            for (const spark of sparks) {
                const distance = spark.radius * easeOutCubic(raw);
                spark.entity.setLocalPosition(
                    position.x + Math.cos(spark.angle) * distance,
                    TILE_HEIGHT * 1.25 + spark.lift * Math.sin(Math.PI * raw) - raw * 0.20,
                    position.z + Math.sin(spark.angle) * distance
                );
                spark.entity.setEulerAngles(360 * raw, spark.angle * 80, 180 * raw);
            }
        }, easeInOutCubic, token);

        for (const spark of sparks) {
            spark.entity.destroy();
        }
        if (opened && this.isCurrent(token)) {
            gate.enabled = false;
        }
        return opened;
    }

    async animateWrongGate(outcome, token) {
        const actor = this.board.actorAt(outcome.attemptedRow, outcome.attemptedColumn);
        const position = this.board.getWorldPosition(outcome.attemptedRow, outcome.attemptedColumn);
        const flashMaterial = this.materials.materials.wrongGate.clone();
        const flash = createBoxEntity(this.app, {
            name: 'wrong-gate-flash-shell',
            parent: this.board.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 0.66 + GATE_HEIGHT * 0.50, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.92, GATE_HEIGHT * 1.10, TILE_SIZE * 0.92),
            material: flashMaterial
        });
        const gate = actor?.gate;
        const gatePosition = gate ? vec3Clone(gate.getLocalPosition()) : null;
        this.runtime.shake(0.075, 0.48);

        const finished = await this.tween(0.48, (t, raw) => {
            const shake = Math.sin(raw * Math.PI * 10) * 0.075 * (1 - raw);
            if (gate && gatePosition) {
                gate.setLocalPosition(gatePosition.x + shake, gatePosition.y, gatePosition.z);
            }
            flash.setLocalScale(TILE_SIZE * (0.92 + raw * 0.26), GATE_HEIGHT * (1.10 + raw * 0.12), TILE_SIZE * (0.92 + raw * 0.26));
            setMaterialOpacity(flashMaterial, 0.54 * (1 - raw));
        }, rawEase, token);

        if (gate && gatePosition) {
            gate.setLocalPosition(gatePosition);
        }
        flash.destroy();
        return finished;
    }

    async animateVoidFall(token) {
        if (!this.cube.root) {
            return true;
        }
        const root = this.cube.root;
        const start = vec3Clone(root.getPosition());
        this.runtime.shake(0.035, 0.28);
        return this.tween(0.70, (t, raw) => {
            const spin = new pc.Quat().setFromAxisAngle(new pc.Vec3(0.85, 0.28, 0.44).normalize(), 260 * t);
            root.setPosition(start.x, start.y - 3.25 * easeInQuad(raw), start.z);
            root.setRotation(spin);
            root.setLocalScale(1 - raw * 0.18, 1 - raw * 0.18, 1 - raw * 0.18);
        }, rawEase, token);
    }

    async animateFinishLocked(outcome, token) {
        const actor = this.board.actorAt(outcome.attemptedRow, outcome.attemptedColumn);
        const target = actor?.finish || actor?.glow || actor?.accent;
        if (!target) {
            return true;
        }
        const position = this.board.getWorldPosition(outcome.attemptedRow, outcome.attemptedColumn);
        const lockMaterial = this.materials.materials.finishLocked.clone();
        const lockPulse = createBoxEntity(this.app, {
            name: 'finish-locked-pulse',
            parent: this.board.root,
            position: new pc.Vec3(position.x, TILE_HEIGHT * 1.55, position.z),
            scale: new pc.Vec3(TILE_SIZE * 0.70, 0.030, TILE_SIZE * 0.70),
            material: lockMaterial
        });
        const baseScale = vec3Clone(target.getLocalScale());
        const finished = await this.tween(0.56, (t, raw) => {
            const pulse = Math.sin(Math.PI * raw);
            target.setLocalScale(baseScale.x * (1 + pulse * 0.18), baseScale.y * (1 + pulse * 0.32), baseScale.z * (1 + pulse * 0.18));
            lockPulse.setLocalScale(TILE_SIZE * (0.70 + raw * 0.54), 0.030, TILE_SIZE * (0.70 + raw * 0.54));
            setMaterialOpacity(lockMaterial, 0.36 * (1 - raw));
        }, easeOutCubic, token);
        target.setLocalScale(baseScale);
        lockPulse.destroy();
        return finished;
    }

    async animateSolved(newState, token) {
        if (!this.cube.root) {
            return true;
        }
        const root = this.cube.root;
        const origin = vec3Clone(root.getPosition());
        const confetti = this.createSparks(origin, null, 34, true);
        const finished = await this.tween(1.05, (t, raw) => {
            root.setPosition(origin.x, origin.y + Math.sin(Math.PI * raw) * 0.42, origin.z);
            root.setEulerAngles(0, 720 * easeOutCubic(raw), 0);
            root.setLocalScale(1 + Math.sin(Math.PI * raw) * 0.12, 1 + Math.sin(Math.PI * raw) * 0.12, 1 + Math.sin(Math.PI * raw) * 0.12);
            for (const spark of confetti) {
                const distance = spark.radius * (0.25 + t * 1.15);
                spark.entity.setLocalPosition(
                    origin.x + Math.cos(spark.angle) * distance,
                    origin.y + 0.32 + spark.lift * Math.sin(Math.PI * raw) + raw * 0.72,
                    origin.z + Math.sin(spark.angle) * distance
                );
                spark.entity.setEulerAngles(720 * raw, 180 * raw + spark.angle * 20, 360 * raw);
            }
        }, easeOutBack, token);
        for (const spark of confetti) {
            spark.entity.destroy();
        }
        if (newState?.cubeFaces) {
            this.cube.updateStickers(newState.cubeFaces);
        }
        return finished;
    }

    createSparks(position, colorName, count, fromWorld = false) {
        const colors = colorName ? [colorName] : ['RED', 'BLUE', 'GREEN', 'YELLOW'];
        const root = fromWorld ? null : this.board.root;
        const sparks = [];
        for (let i = 0; i < count; i++) {
            const angle = (Math.PI * 2 * i) / count;
            const material = this.materials.colorMaterialSet(colors[i % colors.length]).confetti || this.materials.face(colors[i % colors.length]);
            const entity = createBoxEntity(this.app, {
                name: `spark-${i}`,
                parent: root,
                position: new pc.Vec3(position.x, position.y ?? TILE_HEIGHT * 1.3, position.z),
                scale: new pc.Vec3(0.07 + (i % 3) * 0.018, 0.07, 0.07 + (i % 4) * 0.012),
                material
            });
            sparks.push({
                entity,
                angle,
                radius: 0.55 + (i % 6) * 0.105,
                lift: 0.45 + (i % 5) * 0.08
            });
        }
        return sparks;
    }
}


class CommunityController {
    constructor(api) {
        this.api = api;
        this.callbacks = {};
        this.state = null;
        this.serviceBusy = false;
        this.cachedAverage = 0;
        this.cachedPlayerRating = 0;
        this.refreshToken = 0;

        this.panel = $('community-panel');
        this.refreshButton = $('community-refresh-button');
        this.statusElement = $('community-status');

        this.loggedOutPanel = $('community-logged-out');
        this.loggedInPanel = $('community-logged-in');
        this.loginForm = $('community-login-form');
        this.loginPlayerInput = $('community-login-player');
        this.logoutForm = $('community-logout-form');
        this.playerNameElement = $('community-player-name');

        this.currentScoreElement = $('community-current-score');
        this.scoreSavedElement = $('community-score-saved');

        this.ratingAverageElement = $('community-rating-average');
        this.playerRatingElement = $('community-player-rating');
        this.ratingForm = $('community-rating-form');
        this.ratingSelect = $('community-rating-value');
        this.ratingLoginHint = $('community-rating-login-hint');

        this.scoreEmpty = $('community-score-empty');
        this.scoreTable = $('community-score-table');
        this.scoreTableBody = $('community-score-table-body');

        this.commentForm = $('community-comment-form');
        this.commentText = $('community-comment-text');
        this.commentLoginHint = $('community-comment-login-hint');
        this.commentEmpty = $('community-comments-empty');
        this.commentList = $('community-comments-list');
    }

    bind(callbacks = {}) {
        this.callbacks = { ...this.callbacks, ...callbacks };
        this.refreshButton?.addEventListener('click', () => this.refreshAll(this.state));
        this.loginForm?.addEventListener('submit', (event) => this.handleLogin(event));
        this.logoutForm?.addEventListener('submit', (event) => this.handleLogout(event));
        this.ratingForm?.addEventListener('submit', (event) => this.handleRatingSubmit(event));
        this.commentForm?.addEventListener('submit', (event) => this.handleCommentSubmit(event));
    }

    syncState(state) {
        this.state = state;
        this.renderSessionState();
        this.renderScoreState();
        this.renderRating(this.cachedAverage, this.cachedPlayerRating);
    }

    async refreshAll(state = this.state, options = {}) {
        this.state = state || this.state;
        const token = ++this.refreshToken;
        this.setServiceBusy(true);
        if (!options.quiet) {
            this.setStatus('Refreshing rating, scores and comments…');
        }
        try {
            const services = await this.api.services(this.state);
            if (token !== this.refreshToken) {
                return;
            }
            this.cachedAverage = services.averageRating;
            this.cachedPlayerRating = services.playerRating;
            this.renderScores(services.topScores);
            this.renderComments(services.comments);
            this.renderRating(this.cachedAverage, this.cachedPlayerRating);
            if (!options.quiet) {
                this.setStatus('Rating, top scores and comments are up to date.', 'success');
            }
        } catch (error) {
            if (token === this.refreshToken) {
                console.error(error);
                this.setStatus(`Could not refresh GameStudio services: ${error.message || error}`, 'error');
            }
        } finally {
            if (token === this.refreshToken) {
                this.setServiceBusy(false);
            }
        }
    }

    renderSessionState() {
        const loggedIn = Boolean(this.state?.loggedIn && this.state.playerName);
        setHidden(this.loggedOutPanel, loggedIn);
        setHidden(this.loggedInPanel, !loggedIn);
        setHidden(this.ratingForm, !loggedIn);
        setHidden(this.commentForm, !loggedIn);
        setHidden(this.ratingLoginHint, loggedIn);
        setHidden(this.commentLoginHint, loggedIn);

        if (this.playerNameElement) {
            this.playerNameElement.textContent = loggedIn ? this.state.playerName : 'anonymous';
        }
        if (!loggedIn) {
            this.cachedPlayerRating = 0;
        }
        this.setServiceBusy(this.serviceBusy);
    }

    renderScoreState() {
        if (!this.state) {
            if (this.currentScoreElement) {
                this.currentScoreElement.textContent = 'Current score: —';
            }
            if (this.scoreSavedElement) {
                this.scoreSavedElement.textContent = 'Loading score status…';
            }
            return;
        }

        if (this.currentScoreElement) {
            this.currentScoreElement.textContent = `Current score: ${textOrDash(this.state.score)}`;
        }
        if (!this.scoreSavedElement) {
            return;
        }
        if (this.state.scoreSaved) {
            const player = this.state.lastScorePlayer || this.state.playerName || 'player';
            this.scoreSavedElement.textContent = `Score ${this.state.lastSavedScore} saved for ${player}.`;
        } else if (this.state.gameState === 'SOLVED' && !this.state.loggedIn) {
            this.scoreSavedElement.textContent = 'Solved anonymously. Login, restart and solve again to save a score.';
        } else if (!this.state.loggedIn) {
            this.scoreSavedElement.textContent = 'Login before solving to save your score.';
        } else {
            this.scoreSavedElement.textContent = 'Solve the level to save your score automatically.';
        }
    }

    renderRating(average, playerRating) {
        if (this.ratingAverageElement) {
            this.ratingAverageElement.textContent = `Average: ${Number.isFinite(average) ? average : 0} / 5`;
        }
        if (this.ratingSelect) {
            this.ratingSelect.value = String(playerRating >= 1 && playerRating <= 5 ? playerRating : 1);
        }
        if (!this.playerRatingElement) {
            return;
        }
        if (!this.state?.loggedIn) {
            this.playerRatingElement.textContent = 'Your rating: login required';
        } else if (playerRating >= 1 && playerRating <= 5) {
            this.playerRatingElement.textContent = `Your rating: ${playerRating} / 5`;
        } else {
            this.playerRatingElement.textContent = 'Your rating: not set yet';
        }
    }

    renderScores(scores) {
        const rows = normalizeServiceList(scores);
        setHidden(this.scoreEmpty, rows.length > 0);
        setHidden(this.scoreTable, rows.length === 0);
        if (!this.scoreTableBody) {
            return;
        }
        this.scoreTableBody.innerHTML = '';
        rows.forEach((score, index) => {
            const row = document.createElement('tr');
            [
                String(index + 1),
                textOrDash(score?.player),
                textOrDash(score?.points),
                formatServiceDate(score?.playedOn)
            ].forEach((value) => {
                const cell = document.createElement('td');
                cell.textContent = value;
                row.appendChild(cell);
            });
            this.scoreTableBody.appendChild(row);
        });
    }

    renderComments(comments) {
        const rows = normalizeServiceList(comments);
        setHidden(this.commentEmpty, rows.length > 0);
        if (!this.commentList) {
            return;
        }
        this.commentList.innerHTML = '';
        rows.forEach((comment) => {
            const wrapper = document.createElement('article');
            wrapper.className = 'comment';

            const meta = document.createElement('div');
            meta.className = 'comment-meta';

            const player = document.createElement('strong');
            player.className = 'comment-player';
            player.textContent = textOrDash(comment?.player);

            const date = document.createElement('span');
            date.textContent = formatServiceDate(comment?.commentedOn);

            const text = document.createElement('p');
            text.className = 'comment-text';
            text.textContent = textOrDash(comment?.comment);

            meta.append(player, date);
            wrapper.append(meta, text);
            this.commentList.appendChild(wrapper);
        });
    }

    async handleLogin(event) {
        event.preventDefault();
        const player = this.loginPlayerInput?.value.trim() || '';
        if (!PLAYER_PATTERN.test(player)) {
            this.setStatus('Use 2-20 chars: letters, digits, _, -.', 'error');
            this.loginPlayerInput?.focus();
            return;
        }
        this.setServiceBusy(true);
        this.setStatus('Logging in…');
        try {
            await this.api.login(player);
            if (this.loginPlayerInput) {
                this.loginPlayerInput.value = '';
            }
            await this.callbacks.onSessionChanged?.();
            await this.refreshAll(this.state, { quiet: true });
            if (this.state?.loggedIn) {
                this.setStatus(`Logged in as ${this.state.playerName}.`, 'success');
            } else {
                this.setStatus('Login did not complete. Check the player name and try again.', 'error');
            }
        } catch (error) {
            console.error(error);
            this.setStatus(`Login failed: ${error.message || error}`, 'error');
        } finally {
            this.setServiceBusy(false);
        }
    }

    async handleLogout(event) {
        event.preventDefault();
        this.setServiceBusy(true);
        this.setStatus('Logging out…');
        try {
            await this.api.logout();
            await this.callbacks.onSessionChanged?.();
            await this.refreshAll(this.state, { quiet: true });
            this.setStatus('Logged out.', 'success');
        } catch (error) {
            console.error(error);
            this.setStatus(`Logout failed: ${error.message || error}`, 'error');
        } finally {
            this.setServiceBusy(false);
        }
    }

    async handleRatingSubmit(event) {
        event.preventDefault();
        if (!this.state?.loggedIn || !this.state.playerName) {
            this.setStatus('Login to save a rating.', 'error');
            return;
        }
        const rating = Number(this.ratingSelect?.value);
        if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
            this.setStatus('Choose a rating between 1 and 5.', 'error');
            return;
        }
        this.setServiceBusy(true);
        this.setStatus('Saving rating…');
        try {
            await this.api.setRating(rating);
            await this.refreshAll(this.state, { quiet: true });
            this.setStatus('Rating saved.', 'success');
        } catch (error) {
            console.error(error);
            this.setStatus(`Rating failed: ${error.message || error}`, 'error');
        } finally {
            this.setServiceBusy(false);
        }
    }

    async handleCommentSubmit(event) {
        event.preventDefault();
        if (!this.state?.loggedIn || !this.state.playerName) {
            this.setStatus('Login to add a comment.', 'error');
            return;
        }
        const comment = (this.commentText?.value || '').trim().slice(0, MAX_COMMENT_LENGTH);
        if (!comment) {
            this.setStatus('Write a short comment first.', 'error');
            this.commentText?.focus();
            return;
        }
        this.setServiceBusy(true);
        this.setStatus('Adding comment…');
        try {
            await this.api.addComment(comment);
            if (this.commentText) {
                this.commentText.value = '';
            }
            await this.refreshAll(this.state, { quiet: true });
            this.setStatus('Comment added.', 'success');
        } catch (error) {
            console.error(error);
            this.setStatus(`Comment failed: ${error.message || error}`, 'error');
        } finally {
            this.setServiceBusy(false);
        }
    }

    setServiceBusy(busy) {
        this.serviceBusy = Boolean(busy);
        const loggedIn = Boolean(this.state?.loggedIn && this.state.playerName);
        setDisabledWithin(this.loginForm, this.serviceBusy || loggedIn);
        setDisabledWithin(this.logoutForm, this.serviceBusy || !loggedIn);
        setDisabledWithin(this.ratingForm, this.serviceBusy || !loggedIn);
        setDisabledWithin(this.commentForm, this.serviceBusy || !loggedIn);
        if (this.refreshButton) {
            this.refreshButton.disabled = this.serviceBusy;
        }
    }

    setStatus(message, type = '') {
        if (!this.statusElement) {
            return;
        }
        this.statusElement.textContent = message || '';
        this.statusElement.classList.toggle('error', type === 'error');
        this.statusElement.classList.toggle('success', type === 'success');
    }
}

class HUDController {
    constructor() {
        this.shell = $('pc-shell');
        this.levelSelect = $('level-select');
        this.resetButton = $('reset-button');
        this.nextButton = $('next-button');
        this.messageElement = $('hud-message');
        this.busyIndicator = $('busy-indicator');
        this.moveButtons = Array.from(document.querySelectorAll('[data-direction]'));
        this.state = null;
        this.busyPhase = 'loading';
        this.scoreSavedThisMove = false;
    }

    bind({ onReset, onNext, onLevel }) {
        this.resetButton.addEventListener('click', () => onReset());
        this.nextButton.addEventListener('click', () => onNext());
        this.levelSelect.addEventListener('change', () => onLevel(Number(this.levelSelect.value)));
    }

    populateLevels(levels) {
        this.levelSelect.innerHTML = '';
        for (const level of levels) {
            const option = document.createElement('option');
            option.value = String(level.index);
            option.textContent = `Level ${level.number} — ${level.name}`;
            this.levelSelect.appendChild(option);
        }
    }

    setBusyPhase(phase) {
        this.busyPhase = phase;
        this.render();
    }

    renderState(state, options = {}) {
        this.state = state;
        this.scoreSavedThisMove = Boolean(options.scoreSavedThisMove);
        this.render();
    }

    render() {
        const state = this.state;
        const hasState = Boolean(state);
        const phase = this.busyPhase;
        const controlsLocked = phase !== 'idle';
        const animationOnly = phase === 'animating';
        const busy = phase !== 'idle';

        this.levelSelect.disabled = !hasState || controlsLocked;
        this.resetButton.disabled = !hasState || phase === 'loading' || phase === 'resetting' || phase === 'requesting-move';
        this.nextButton.disabled = !hasState || controlsLocked || state?.lastLevel || state?.gameState !== 'SOLVED';
        for (const button of this.moveButtons) {
            button.disabled = !hasState || controlsLocked || state?.gameState !== 'PLAYING';
        }
        if (this.busyIndicator) {
            this.busyIndicator.hidden = !busy;
        }
        if (this.shell) {
            this.shell.classList.toggle('is-busy', busy);
        }
        if (animationOnly) {
            this.resetButton.disabled = false;
        }

        if (!state) {
            this.setMessage('Loading CubeRoll…');
            return;
        }

        $('hud-level').textContent = `${state.currentLevelNumber}/${state.levelCount} — ${state.levelName}`;
        $('hud-moves').textContent = String(state.moveCount);
        $('hud-score').textContent = String(state.score);
        $('hud-painters').textContent = String(state.remainingPainters);
        $('hud-state').textContent = state.gameState;
        $('hud-player').textContent = state.loggedIn ? state.playerName : 'anonymous';
        this.levelSelect.value = String(state.currentLevelIndex);

        let message = state.message || 'Ready.';
        if (this.scoreSavedThisMove) {
            message += ` Score ${state.lastSavedScore} saved for ${state.lastScorePlayer}.`;
        } else if (state.scoreSaved) {
            message += ` Saved score: ${state.lastSavedScore}.`;
        }
        if (phase === 'requesting-move') {
            message = `Asking Java API… ${message}`;
        } else if (phase === 'animating') {
            message = `Animating outcome… ${message}`;
        } else if (phase === 'resetting') {
            message = `Resetting… ${message}`;
        } else if (phase === 'changing-level') {
            message = `Changing level… ${message}`;
        } else if (phase === 'advancing') {
            message = `Loading next level… ${message}`;
        }
        this.setMessage(message, state.gameState);
        this.shell.classList.toggle('failed', state.gameState === 'FAILED');
        this.shell.classList.toggle('solved', state.gameState === 'SOLVED');
        this.shell.classList.toggle('is-failed', state.gameState === 'FAILED');
        this.shell.classList.toggle('is-solved', state.gameState === 'SOLVED');
        this.shell.setAttribute('aria-busy', phase === 'idle' ? 'false' : 'true');
    }

    setMessage(message, gameState = this.state?.gameState) {
        this.messageElement.textContent = message;
        this.messageElement.classList.toggle('failed', gameState === 'FAILED');
        this.messageElement.classList.toggle('solved', gameState === 'SOLVED');
    }

    showError(error) {
        console.error(error);
        this.setMessage(`PlayCanvas/API error: ${error.message || error}`, 'FAILED');
    }
}

class InputController {
    constructor(hud, onMove) {
        this.hud = hud;
        this.onMove = onMove;
        this.bind();
    }

    bind() {
        document.addEventListener('keydown', (event) => {
            const target = event.target;
            if (target instanceof HTMLElement && target.matches('input, textarea, select, button')) {
                return;
            }
            const direction = KEY_TO_DIRECTION[event.key];
            if (!direction) {
                return;
            }
            event.preventDefault();
            this.onMove(direction);
        });

        for (const button of this.hud.moveButtons) {
            button.addEventListener('pointerdown', (event) => {
                event.preventDefault();
                if (button.disabled) {
                    return;
                }
                this.onMove(button.dataset.direction);
            });
        }
    }
}

class CubeRoll3dGame {
    constructor() {
        this.api = new ApiClient(API_ROOT);
        this.materials = new MaterialFactory();
        this.hud = new HUDController();
        this.community = new CommunityController(this.api);
        this.runtime = new PlayCanvasRuntime($('application-canvas'), this.materials);
        this.board = new BoardRenderer(this.runtime.app, this.materials);
        this.cube = new CubeRenderer(this.runtime.app, this.materials);
        this.animations = new AnimationController(this.runtime.app, this.runtime, this.board, this.cube, this.materials);
        this.input = new InputController(this.hud, (direction) => this.submitMove(direction));
        this.state = null;
        this.levels = [];
        this.busyPhase = 'loading';

        this.hud.bind({
            onReset: () => this.resetLevel(),
            onNext: () => this.nextLevel(),
            onLevel: (levelIndex) => this.selectLevel(levelIndex)
        });
        this.community.bind({
            onSessionChanged: () => this.reloadSessionState()
        });
        this.setBusyPhase('loading');
        this.bootstrap().catch((error) => this.handleError(error));
    }

    async bootstrap() {
        const [levels, state] = await Promise.all([
            this.api.levels(),
            this.api.state()
        ]);
        this.levels = levels;
        this.hud.populateLevels(levels);
        this.applyState(state);
        this.community.refreshAll(this.state, { quiet: true });
        this.setBusyPhase('idle');
    }

    setBusyPhase(phase) {
        this.busyPhase = phase;
        this.hud.setBusyPhase(phase);
    }

    async reloadSessionState() {
        const state = await this.api.state();
        this.syncSessionState(state);
        return this.state;
    }

    syncSessionState(rawState) {
        const state = normalizeStateForRendering(rawState);
        this.state = state;
        this.hud.renderState(state);
        this.community.syncState(state);
    }

    applyState(rawState, options = {}) {
        const state = normalizeStateForRendering(rawState);
        this.state = state;
        this.board.rebuild(state);
        const outcomeEffects = new Set(options.outcome?.effects || []);
        const hideCubeAfterFall = state.gameState === 'FAILED' && outcomeEffects.has('FELL');
        this.cube.rebuild(state, { hidden: hideCubeAfterFall });
        this.runtime.updateCameraForState(state);
        this.hud.renderState(state, { scoreSavedThisMove: options.scoreSavedThisMove });
        this.community.syncState(state);
        if (options.scoreSavedThisMove) {
            this.community.refreshAll(state, { quiet: true });
        }
    }

    async submitMove(direction) {
        if (!direction || this.busyPhase !== 'idle' || !this.state || this.state.gameState !== 'PLAYING') {
            return;
        }

        const token = this.animations.begin();
        this.setBusyPhase('requesting-move');
        try {
            const response = await this.api.move(direction);
            if (!this.animations.isCurrent(token)) {
                return;
            }
            this.setBusyPhase('animating');
            const animated = await this.animations.animateOutcome(response.outcome, response.state, token);
            if (!animated || !this.animations.isCurrent(token)) {
                return;
            }
            this.applyState(response.state, {
                outcome: response.outcome,
                scoreSavedThisMove: response.scoreSavedThisMove
            });
            this.setBusyPhase('idle');
        } catch (error) {
            if (this.animations.isCurrent(token)) {
                this.handleError(error);
                await this.refreshStateAfterError();
                this.setBusyPhase('idle');
            }
        }
    }

    async resetLevel() {
        if (!this.state || this.busyPhase === 'loading' || this.busyPhase === 'requesting-move' || this.busyPhase === 'resetting') {
            return;
        }
        this.animations.cancelAll();
        const token = this.animations.begin();
        this.setBusyPhase('resetting');
        try {
            const state = await this.api.reset();
            if (!this.animations.isCurrent(token)) {
                return;
            }
            this.applyState(state);
            this.setBusyPhase('idle');
        } catch (error) {
            if (this.animations.isCurrent(token)) {
                this.handleError(error);
                this.setBusyPhase('idle');
            }
        }
    }

    async nextLevel() {
        if (this.busyPhase !== 'idle' || !this.state || this.state.lastLevel || this.state.gameState !== 'SOLVED') {
            return;
        }
        this.animations.cancelAll();
        const token = this.animations.begin();
        this.setBusyPhase('advancing');
        try {
            const state = await this.api.next();
            if (!this.animations.isCurrent(token)) {
                return;
            }
            this.applyState(state);
            this.setBusyPhase('idle');
        } catch (error) {
            if (this.animations.isCurrent(token)) {
                this.handleError(error);
                this.setBusyPhase('idle');
            }
        }
    }

    async selectLevel(levelIndex) {
        if (this.busyPhase !== 'idle' || Number.isNaN(levelIndex)) {
            return;
        }
        this.animations.cancelAll();
        const token = this.animations.begin();
        this.setBusyPhase('changing-level');
        try {
            const state = await this.api.selectLevel(levelIndex);
            if (!this.animations.isCurrent(token)) {
                return;
            }
            this.applyState(state);
            this.setBusyPhase('idle');
        } catch (error) {
            if (this.animations.isCurrent(token)) {
                this.handleError(error);
                await this.refreshStateAfterError();
                this.setBusyPhase('idle');
            }
        }
    }

    async refreshStateAfterError() {
        try {
            const state = await this.api.state();
            this.applyState(state);
        } catch (ignored) {
            // Keep the visible API error; there is no fresher state to show.
        }
    }

    handleError(error) {
        this.hud.showError(error);
    }
}


function setupPanelToggles() {
    const shell = $('pc-shell');
    if (!shell) {
        return;
    }

    const controls = {
        hud: {
            close: $('close-hud-button'),
            open: $('open-hud-button'),
            className: 'hud-collapsed'
        },
        community: {
            close: $('close-community-button'),
            open: $('open-community-button'),
            className: 'community-collapsed'
        }
    };

    const setPanelOpen = (panelName, open) => {
        const panel = controls[panelName];
        if (!panel) {
            return;
        }
        shell.classList.toggle(panel.className, !open);
        if (panel.open) {
            panel.open.setAttribute('aria-expanded', String(open));
        }
        if (panel.close) {
            panel.close.setAttribute('aria-expanded', String(open));
        }
    };

    const isNarrow = window.matchMedia('(max-width: 920px)');

    if (controls.hud.close) {
        controls.hud.close.addEventListener('click', () => setPanelOpen('hud', false));
    }
    if (controls.hud.open) {
        controls.hud.open.addEventListener('click', () => setPanelOpen('hud', true));
    }
    if (controls.community.close) {
        controls.community.close.addEventListener('click', () => setPanelOpen('community', false));
    }
    if (controls.community.open) {
        controls.community.open.addEventListener('click', () => setPanelOpen('community', true));
    }

    const applyInitialLayout = () => {
        if (isNarrow.matches) {
            setPanelOpen('hud', false);
            setPanelOpen('community', false);
        } else {
            setPanelOpen('hud', true);
            setPanelOpen('community', true);
        }
    };

    applyInitialLayout();

    const onMediaChange = (event) => {
        if (!event.matches) {
            setPanelOpen('hud', true);
            setPanelOpen('community', true);
        }
    };

    if (typeof isNarrow.addEventListener === 'function') {
        isNarrow.addEventListener('change', onMediaChange);
    } else if (typeof isNarrow.addListener === 'function') {
        isNarrow.addListener(onMediaChange);
    }
}

function showWebGLFallback(message = null) {
    const fallback = $('webgl-fallback');
    const canvas = $('application-canvas');
    const shell = $('pc-shell');
    if (shell) {
        shell.classList.add('webgl-missing');
    }
    if (fallback) {
        fallback.hidden = false;
        if (message) {
            const detail = fallback.querySelector('span') || fallback.querySelector('p:not(.eyebrow)');
            if (detail) {
                detail.textContent = message;
            }
        }
    }
    if (canvas) {
        canvas.hidden = true;
    }
}

window.addEventListener('DOMContentLoaded', () => {
    if (!supportsWebGL()) {
        showWebGLFallback();
        return;
    }
    // Responsive side panels are initialized by the HTML script before PlayCanvas starts.
    try {
        new CubeRoll3dGame();
    } catch (error) {
        console.error(error);
        showWebGLFallback(`The PlayCanvas scene could not start: ${error.message || error}. The classic Java-rendered game is still available.`);
    }
});
