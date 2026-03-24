// DEPRECATED: This file is no longer used.
// 
// The lap pillow system handles rider pose through:
// 1. LapPillowHandler.applyPlayerSleepingState() - Sets player pose to SLEEPING
// 2. LapPillowSeatRenderMixin - Render-only passenger-state override on LivingEntityRenderer
// 3. LapPillowState - Maintains client/server state synchronization
//
// Avoid global entity state spoofing (isPassenger/getVehicle/isSleeping),
// which can interfere with ClientboundSetPassengersPacket handling.
