package no.statkart.sktools.gradle.plugins.filterresources;

import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.language.jvm.tasks.ProcessResources;

public class FilterResourcesTask extends ProcessResources {
    public FilterResourcesTask() {
    }

    public AbstractCopyTask srcDir(Object srcDir) {
        return from(srcDir);
    }

    public AbstractCopyTask srcDirs(Object... srcDirs) {
        return from(srcDirs);
    }

    public AbstractCopyTask source(FileTree src) {
        return from(src);
    }

    public void setSrcDirs(Iterable<Object> srcDirs) {
        for (Object srcDir : srcDirs) {
            from(srcDir);
        }
    }

    /**
     * Configures the path for filtered resources output.
     *
     * @since 1.2 - SKIF-173
     */
    public void setDestinationDir(Object filteredResourcesDir) {
        into(filteredResourcesDir);
    }

}
