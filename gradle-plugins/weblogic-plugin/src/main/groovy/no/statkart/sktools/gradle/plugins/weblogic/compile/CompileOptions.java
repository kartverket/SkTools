package no.statkart.sktools.gradle.plugins.weblogic.compile;

import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.util.HashMap;
import java.util.Map;

public class CompileOptions {
    private final Map<String, Object> optionsMap = new HashMap<>(16);

    private boolean fork;
    private boolean listFiles = true;
    boolean verbose;
    boolean deprecation;
    boolean nowarn;
    boolean debug = true;
    boolean failOnError = true;

    String encoding;


    public Map<String, Object> optionMap() {
        optionsMap.put("fork", fork);
        optionsMap.put("listFiles", listFiles);
        optionsMap.put("verbose", verbose);
        optionsMap.put("deprecation", deprecation);
        optionsMap.put("nowarn", nowarn);
        optionsMap.put("debug", debug);
        optionsMap.put("failOnError", failOnError);

        if (encoding == null) {
            optionsMap.remove("encoding");
        } else {
            optionsMap.put("encoding", encoding);
        }

        return optionsMap;
    }


    @Input
    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    @Console
    public boolean isListFiles() {
        return listFiles;
    }

    public void setListFiles(boolean listFiles) {
        this.listFiles = listFiles;
    }

    @Console
    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Console
    public boolean isDeprecation() {
        return deprecation;
    }

    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    @Console
    public boolean isNowarn() {
        return nowarn;
    }

    public void setNowarn(boolean nowarn) {
        this.nowarn = nowarn;
    }

    @Input
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Input
    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @Input
    @Optional
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

}
