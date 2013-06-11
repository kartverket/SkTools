package no.statkart.sktools.gradle.plugins.webstart.util;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.gradle.api.AntBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Steg for signering av alle jar avhengigheter.
 *
 * @author Leif Lislegård
 */
public class JarSigner {
    private static Logger logger = LoggerFactory.getLogger(JarSigner.class);
    private File cacheDir;
    /**
     * Holder cache over alle signerte filer, et map per sertifikat.
     * <p>
     * String er her en representasjon av jarfilen som typiskt inneholder navn, group og version.
     */
    final Map<FileHashIdent, Map<String, FileHashIdent>> signedArtifactsForCertificates = new HashMap<FileHashIdent, Map<String, FileHashIdent>>();


    private AntBuilder ant;

    private File certificateFile;
    private String password;
    private String alias;
    private Set<File> jarfilesToSign;




    public JarSigner(File cacheDir) throws IOException {
        this.cacheDir = cacheDir;

        initCache();
    }


    /**
     * Signerer jar filer, eller henter cachede filer basert på lagrede hashverdier.
     * Filer blir kopiert til {@link #getCacheDir()} med undermappe lik hashverdi til sertifikatet
     * </br>
     * @return signerte jarfiler for dependencies som value, parameteriserte filer som keys.
     */
    public Map<File, File> signJars() throws Exception {
        HashMap<File, File> returnedFiles = new HashMap<File, File>();

        //sertifikat ident er streng representasjon av hash verdi + alias  - dette danner da mappenavn i cache dir
        FileHashIdent certificateFileIdent = new FileHashIdent(getCertificateFile(), FileHashIdent.createChecksum(getCertificateFile(), getAlias()));

        File certDirectory = new File(cacheDir, certificateFileIdent.hash());
        certDirectory.mkdirs();

        Map<String, FileHashIdent> signedArtifacts = signedArtifactsForCertificates.get(certificateFileIdent);
        if (signedArtifacts == null) {
            signedArtifacts = new HashMap<String, FileHashIdent>();
            signedArtifactsForCertificates.put(certificateFileIdent, signedArtifacts);
        }

        for (File unsignedJar : jarfilesToSign) {
            FileHashIdent jarFileIdent = new FileHashIdent(unsignedJar);

            File signedJarFile = null;

            //forsøker cache
            FileHashIdent cachedFileIdent = signedArtifacts.get(unsignedJar.getName());
            if (cachedFileIdent != null) {
                if (cachedFileIdent.equals(jarFileIdent)) {
                    signedJarFile = cachedFileIdent.file;
                    logger.info("...using cached jar " + signedJarFile.getAbsolutePath());
                }
            }

            //dersom fortsatt ikke funnet - signer filen
            if (signedJarFile == null) {

                File tempFile = new File(certDirectory, unsignedJar.getName() + ".tmp");
                signedJarFile = new File(certDirectory, unsignedJar.getName());


                //kopiere via URL for å overkomme evt symlink filer.
                FileUtils.copyURLToFile(unsignedJar.toURI().toURL(), tempFile);

                //signing jar
                try {
                    signJar(tempFile);

                    signedJarFile.delete(); //SKIF-209: sletter evt eksisterende fil før move..
                    tempFile.renameTo(signedJarFile);

                } finally {
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }

                //updating cache...
                if (cachedFileIdent != null) {
                    logger.debug("updating cache-entry for " + unsignedJar);
                }
                cachedFileIdent = new FileHashIdent(signedJarFile, jarFileIdent.hash());
                cachedFileIdent.writeChecksumToFile();
                signedArtifacts.put(unsignedJar.getName(), cachedFileIdent);
            }

            returnedFiles.put(unsignedJar, signedJarFile);

        }




        return returnedFiles;
    }

    private void signJar(File jarFile) {

        Project antProject = getAnt().getAntProject();
        ExecTask signJarTask = (ExecTask) antProject.createTask("exec");

        signJarTask.setTaskName("signjar");
        signJarTask.setDir(jarFile.getParentFile());
        signJarTask.setFailIfExecutionFails(true);
        signJarTask.setFailonerror(true);
        signJarTask.setExecutable("jarsigner.exe");
        signJarTask.createArg().setValue("-keystore");
        signJarTask.createArg().setValue(getCertificateFile().getAbsolutePath());
        signJarTask.createArg().setValue("-storepass");
        signJarTask.createArg().setValue(getPassword());
        signJarTask.createArg().setValue(jarFile.getName());    //file to sign in same dir - see ...setDir(File)
        signJarTask.createArg().setValue(getAlias());

        if (logger.isInfoEnabled()) {
            signJarTask.createArg().setValue("-verbose");
        }

        logger.info(String.format("Signing file %s ...", jarFile.getPath()));

        signJarTask.execute();


//        project.ant.exec(executable: 'jarsigner', failonerror: true) {
//            if (project.logger.isEnabled(LogLevel.INFO)) {
//                arg(value: '-verbose')
//            }
//            arg(value: '-keystore')
//            arg(value: keystoreFile)
//            arg(value: '-storepass')
//            arg(value: keystorePassword)
//            arg(value: signedJarFileName)
//            arg(value: alias)
//
    }


    public AntBuilder getAnt() {
        return ant;
    }

    public void setAnt(AntBuilder ant) {
        this.ant = ant;
    }

    public File getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(File certificateFile) {
        this.certificateFile = certificateFile;
    }

    public Set<File> getJarfilesToSign() {
        return jarfilesToSign;
    }

    public void setJarfilesToSign(Set<File> jarfilesToSign) {
        this.jarfilesToSign = jarfilesToSign;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        JarSigner.logger = logger;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    protected String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    void initCache() throws IOException {
        signedArtifactsForCertificates.clear();
        logger.info("Initializing cache...");

        if (cacheDir.exists()) {
            for (File certDirectory : cacheDir.listFiles()) {
                if (certDirectory.isDirectory()) {
                    FileHashIdent certFileIdent = new FileHashIdent(certDirectory, certDirectory.getName());

                    logger.debug(String.format("Found cache-dir for certificate with hash = %s", certDirectory.getName()));

                    Map<String, FileHashIdent> signedArtifacts = signedArtifactsForCertificates.get(certFileIdent);
                    if (signedArtifacts == null) {
                        signedArtifacts = new HashMap<String, FileHashIdent>();
                        signedArtifactsForCertificates.put(certFileIdent, signedArtifacts);
                    }

                    for (File md5File : certDirectory.listFiles()) {
                        if (md5File.isFile() && md5File.getName().endsWith(".md5")) {
                            FileHashIdent signedArtifactFileIdent = FileHashIdent.fileHashIdentFromChecksumFile(md5File);
                            if (signedArtifactFileIdent != null) {
                                String unsignedFileName = signedArtifactFileIdent.file.getName();
                                signedArtifacts.put(unsignedFileName, signedArtifactFileIdent);
                                logger.debug(String.format("   found cached file %s", signedArtifactFileIdent.file.getAbsolutePath()));
                            }
                        }
                    }
                }
            }
        } else {
            logger.info("  Cachedir does not exist!");
        }
        logger.info("...cache initialized!");
    }

}
