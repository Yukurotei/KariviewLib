package it.yuruni.kariview.client.data.actions;

import it.yuruni.kariview.client.animation.states.AnimationContext;

import java.util.List;

public class ShowVanillaHudAction implements Action {
    private List<String> hudElements;

    public List<String> getHudElements() {
        return hudElements;
    }

    @Override
    public void execute(AnimationContext ctx) {
    }
}
