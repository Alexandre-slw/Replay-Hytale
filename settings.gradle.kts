import dev.scaffoldit.hytale.Patchline

rootProject.name = "replay-plugin"

plugins {
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    usePatchline(Patchline.RELEASE.name)
    useVersion("0.5.0")
}