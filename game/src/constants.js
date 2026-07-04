// Shared tuning constants for TAME.
export const SEED = 20260704;

export const WORLD_SIZE = 170;          // ground plane is WORLD_SIZE x WORLD_SIZE
export const WORLD_RADIUS = 78;         // circular playable boundary
export const SPAWN_CLEAR_RADIUS = 13;   // props keep out of the center spawn area

export const ANIMAL_COUNT = 10;
export const TAME_RANGE = 2.2;          // metres
export const TAME_FACING_DOT = 0.3;     // player must roughly face the animal
export const TAME_DURATION = 0.6;       // seconds for the taming moment

export const CARAVAN_SPACING = 1.6;     // metres behind the previous caravan member

export const PLAYER_WALK_SPEED = 4.0;
export const PLAYER_RUN_SPEED = 8.0;

export const ANIMAL_WALK_SPEED = 2.1;
export const ANIMAL_FLEE_SPEED = 6.6;
export const FLEE_NOTICE_RADIUS = 7.0;  // running player inside this radius scares animals
export const FLEE_PANIC_RADIUS = 1.2;   // moving quickly this close always scares them

export const CAM_MIN_DIST = 4;
export const CAM_MAX_DIST = 14;

export const MAX_DT = 0.05;             // clamp dt to 50 ms
