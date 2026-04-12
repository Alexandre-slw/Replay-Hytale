package gg.alexandre.replay.ui.codec;

import javax.annotation.Nonnull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface UIKey {

    @Nonnull
    String value();

}
