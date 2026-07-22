package cn.remix.module.impl.render;

import cn.remix.module.Category;
import cn.remix.module.Module;
import cn.remix.module.value.impl.NumberValue;

public final class MotionCamera extends Module {
    public final NumberValue interpolation = new NumberValue("Interpolation", 0.01f, 0.01f, 0.4f, 0.01f);

    public MotionCamera() {
        super("MotionCamera", Category.Render);
    }
}
