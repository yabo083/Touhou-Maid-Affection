package com.github.touhoumaidaffection.handler;

final class KissInteractionPolicy {
    private KissInteractionPolicy() {
    }

    static boolean shouldHandle(boolean rightClickEnabled, boolean sneaking, boolean mainHandEmpty,
                                boolean carryOnLoaded, boolean offhandEmpty) {
        if (!rightClickEnabled || !sneaking || !mainHandEmpty) {
            return false;
        }
        return !carryOnLoaded || !offhandEmpty;
    }
}
