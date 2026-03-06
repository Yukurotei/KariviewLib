package it.yuruni.kariview.client.data.actions;

import it.yuruni.kariview.client.animation.states.AnimationContext;

public interface Action {
    void execute(AnimationContext ctx);
}
