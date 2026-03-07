package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.actions.Action;

import java.util.List;

public class VariableWatch {
    private String namespace;
    private String variable;
    private List<Action> actions;
    public String getNamespace() { return namespace; }
    public String getVariable() { return variable; }
    public List<Action> getActions() { return actions; }
}
