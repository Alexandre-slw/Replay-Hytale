rootProject.name = "replay-plugin"

plugins {
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    usePatchline("release")
    useVersion("latest")

    repositories {

    }

    dependencies {

    }

    manifest {
        Group = "Alexandre"
        Name = "Replay"
        Main = "gg.alexandre.replay.ReplayPlugin"
        IncludesAssetPack = true
    }
}