package boilerplate.rendering;

public class CameraKeyAction {
    public interface Func {
        void call(float speed);
    }

    public int key;
    Func callback;

    public CameraKeyAction(int key, Func actionFunc) {
        this.key = key;
        this.callback = actionFunc;
    }
}
