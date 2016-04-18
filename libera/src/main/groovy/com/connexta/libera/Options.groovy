package com.connexta.libera

/**
 * Class that encapsulates the release execution options.
 */
class Options {
    final boolean gitPush
    final boolean dryRun
    final boolean force
    final boolean quickBuild

    Options(map) {
        gitPush = map.gitPush
        dryRun = map.dryRun
        force = map.force
        quickBuild = map.quickBuild
    }

    Options(OptionAccessor options) {
        this.gitPush = options.p ?: false
        this.dryRun = options.t ?: false
        this.force = options.f ?: false
        this.quickBuild = options.q ?: false
    }
}
