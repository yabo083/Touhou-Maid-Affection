// DEPRECATED: This file is no longer used.
// 
// The lap pillow system handles rider pose through:
// 1. LapPillowHandler.applyPlayerSleepingState() - Sets player pose to SLEEPING
// 2. LivingEntityLapPillowSleepMixin - Configures correct bed orientation for sleeping
// 3. LapPillowState - Maintains client/server state synchronization
//
// The player's SLEEPING pose automatically overrides the sitting animation,
// making the shouldRiderSit() override unnecessary.
