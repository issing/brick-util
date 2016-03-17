package net.isger.util.reflect;

import java.util.Vector;

import net.isger.util.hitch.Director;

public class Actor extends Director {

    private static final String KEY_ACTIONS = "brick.util.reflect.actions";

    private static final String ACTION_PATH = "net/isger/util/reflect/action";

    // private static final Logger LOG;

    // private static final Actor ACTOR;

    private Vector<Action> actions;

    static {
        // LOG = LoggerFactory.getLogger(Actor.class);
        // ACTOR = new Actor()
    }

    protected Actor() {
        actions = new Vector<Action>();
    }

    protected String directPath() {
        return directPath(KEY_ACTIONS, ACTION_PATH);
    }

    protected void directInflux() {
    }

    public static Object act(Class<?> clazz) {
        return null;
    }

    public void add(Action action) {
        actions.add(action);
    }
}
