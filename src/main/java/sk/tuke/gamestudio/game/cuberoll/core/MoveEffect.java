package sk.tuke.gamestudio.game.cuberoll.core;

/**
 * High-level facts produced by a move. These effects are renderer-agnostic and
 * allow a frontend to choose its own animations without duplicating game rules.
 */
public enum MoveEffect {
    IGNORED_NOT_PLAYING,
    FINISH_LOCKED,
    ROLLED,
    MOVED,
    FELL,
    WRONG_GATE_COLOR,
    PAINTER_USED,
    GATE_OPENED,
    SOLVED
}
