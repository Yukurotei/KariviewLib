package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.data.VariableManager;

import java.util.List;
import java.util.Map;

public class BranchAction implements Action {
    private String namespace;
    private String variable;
    private Map<String, BranchCase> cases;
    @SerializedName("default")
    private BranchCase defaultCase;

    @Override
    public void execute(AnimationContext ctx) {
        String value = VariableManager.get(namespace, variable);
        BranchCase chosen = null;
        if (value != null && cases != null) {
            chosen = cases.get(value);
        }
        if (chosen == null) {
            chosen = defaultCase;
        }
        if (chosen == null) return;
        if (chosen.getActions() != null) {
            for (Action action : chosen.getActions()) {
                action.execute(ctx);
            }
        }
    }

    public static class BranchCase {
        private List<Action> actions;
        public List<Action> getActions() { return actions; }
    }
}
