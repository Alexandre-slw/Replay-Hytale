import dev.scaffoldit.hytale.Patchline

rootProject.name = "replay-plugin"

plugins {
    id("dev.scaffoldit") version "0.2.14"
}

hytale {
    usePatchline(Patchline.PRE_RELEASE.name)
}