package no.statkart.sktools.gradle.plugins.webstart;

import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Steg for signering av alle jar avhengigheter.
 *
 * @author Leif Lislegård
 * @author Tor Egil R. Strand
 */
public class JarSigner extends ConventionTask {
    private static Logger logger = LoggerFactory.getLogger(JarSigner.class);
    private File cacheDir;
    /**
     * Holder cache over alle signerte filer, et map per sertifikat.
     * <p/>
     * String er her en representasjon av jarfilen som typiskt inneholder navn, group og version.
     */
    private Map<FileHashIdent, Map<String, FileHashIdent>> signedArtifactsForCertificates;

    private File certificateFile;
    private String password;
    private String alias;

    private Object jarFilesToSign;


    public Map<FileHashIdent, Map<String, FileHashIdent>> getSignedArtifactsForCertificates() {
        if (signedArtifactsForCertificates == null) {
            try {
                initCache();
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize cache of signed jars", e);
            }
        }

        return signedArtifactsForCertificates;
    }

    /**
     * Signerer jar filer, eller henter cachede filer basert på lagrede hashverdier.
     * Filer blir kopiert til {@link #getCacheDir()} med undermappe lik hashverdi til sertifikatet
     * </br>
     */
    @TaskAction
    public void signJars() throws Exception {
        if (getCertificateFile() != null) {
            //sertifikat ident er streng representasjon av hash verdi + alias  - dette danner da mappenavn i cache dir
            FileHashIdent certificateFileIdent = new FileHashIdent(getCertificateFile(), FileHashIdent.createChecksum(getCertificateFile(), getAlias()));

            File certDirectory = new File(getCacheDir(), certificateFileIdent.hash());
            certDirectory.mkdirs();

            Map<String, FileHashIdent> signedArtifacts = getSignedArtifactsForCertificates().get(certificateFileIdent);
            if (signedArtifacts == null) {
                signedArtifacts = new HashMap<String, FileHashIdent>();
                getSignedArtifactsForCertificates().put(certificateFileIdent, signedArtifacts);
            }

            Set<File> filesToSign = getProject().files(getJarFilesToSign()).getFiles();
            for (File unsignedJar : filesToSign) {
                FileHashIdent jarFileIdent = new FileHashIdent(unsignedJar);

                File signedJarFile = null;

                //forsøker cache
                FileHashIdent cachedFileIdent = signedArtifacts.get(unsignedJar.getName());
                if (cachedFileIdent != null) {
                    if (cachedFileIdent.equals(jarFileIdent)) {
                        signedJarFile = cachedFileIdent.getFile();
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
            }
        } else {
            logger.info("signing not turned on");
        }
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

    @InputFile
    @Optional
    public File getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(File certificateFile) {
        this.certificateFile = certificateFile;
    }

    public File getCacheDir() {
        if (cacheDir == null) {
            cacheDir = new File(getProject().getBuildDir(), "sign-cache");
        }
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Input
    @Optional
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Input
    @Optional
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    void initCache() throws IOException {
        signedArtifactsForCertificates = new HashMap<FileHashIdent, Map<String, FileHashIdent>>();
        logger.info("Initializing cache...");

        if (getCacheDir().exists()) {
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
                                String unsignedFileName = signedArtifactFileIdent.getFile().getName();
                                signedArtifacts.put(unsignedFileName, signedArtifactFileIdent);
                                logger.debug(String.format("   found cached file %s", signedArtifactFileIdent.getFile().getAbsolutePath()));
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

    @InputFiles
    @SkipWhenEmpty
    public Object getJarFilesToSign() {
        System.out.println("Sign inputs: " + getProject().files(jarFilesToSign).getFiles());
        return jarFilesToSign;
    }

    public void setJarFilesToSign(Object jarFilesToSign) {
        this.jarFilesToSign = jarFilesToSign;
    }

    @OutputFiles
    public Collection<File> getSignedJars() {
        try {
            Set<File> unsignedFiles = getProject().files(jarFilesToSign).getFiles();

            if (getCertificateFile() == null) {
                // Kan ikke signere noe uten sertifikat
                System.out.println("Unsign outputs: " + unsignedFiles);
                return unsignedFiles;
            }

            FileHashIdent certificateFileIdent = new FileHashIdent(getCertificateFile(), FileHashIdent.createChecksum(getCertificateFile(), getAlias()));
            File certDirectory = new File(getCacheDir(), certificateFileIdent.hash());

            List<File> signedFiles = new ArrayList<File>(unsignedFiles.size());
            for (File unsignedFile : unsignedFiles) {
                File signedFile = new File(certDirectory, unsignedFile.getName());
                signedFiles.add(signedFile);
            }

            System.out.println("Signed outputs: " + signedFiles);
            return signedFiles;
        } catch (Exception e) {
            throw new GradleException("Error calculating (un)signed output files from " + JarSigner.class.getName(), e);
        }
    }
}
