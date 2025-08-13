package boilerplate.rendering;

public class CameraAction {
    public interface Func {
        void call(float speed);
    }

    public int key;
    Func callback;

    public CameraAction(int key, Func actionFunc) {
        this.key = key;
        this.callback = actionFunc;
    }
}
