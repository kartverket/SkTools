package no.statkart.sktools.gradle.testutils.builder

class DbToolsProjectBuilder<T extends DbToolsProjectBuilder> extends GradleProjectBuilder<T> {

    public static DbToolsProjectBuilder<? extends DbToolsProjectBuilder> builder() {
        return new DbToolsProjectBuilder();
    }


    public T applyDbUtilsPlugin() {
        closures.add {
            apply plugin: 'sktools-dbtools-plugin'
        }
        return this
    }
}
