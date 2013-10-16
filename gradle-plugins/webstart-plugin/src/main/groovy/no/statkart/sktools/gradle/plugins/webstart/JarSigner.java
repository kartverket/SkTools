package no.statkart.sktools.gradle.plugins.webstart;

import groovy.lang.Closure;
import no.statkart.sktools.gradle.plugins.webstart.util.FileHashIdent;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.codehaus.groovy.runtime.MethodClosure;
import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

    private Map<String, String> manifestAttributes = new LinkedHashMap<String, String>();

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

            File manifestAddendum = null;
            if (!manifestAttributes.isEmpty()) {
                manifestAddendum = new File(getTemporaryDir(), "addendum.mf");

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(manifestAddendum, false), "UTF-8"));
                try {
                    for (Map.Entry<String, String> entry : manifestAttributes.entrySet()) {
                        writer.format("%s: %s\n", entry.getKey(), entry.getValue());
                    }
                } finally {
                    writer.close();
                }
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
                        signJar(tempFile, manifestAddendum);

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

    private void signJar(final File jarFile, final File manifestAddendum) {

        if (manifestAddendum != null) {
            logger.info(String.format("Updating manifest for %s ...", jarFile.getPath()));

            getProject().exec(new Closure(this) {
                public void doCall() {
                    ExecSpec execSpec = (ExecSpec) getDelegate();

                    execSpec.setWorkingDir(jarFile.getParent());
                    execSpec.setIgnoreExitValue(false);
                    execSpec.setExecutable("jar.exe");
                    execSpec.args("ufm", jarFile.getName(), manifestAddendum.getAbsolutePath());
                }
            });
        }

        logger.info(String.format("Signing file %s ...", jarFile.getPath()));

        getProject().exec(new Closure(this) {
            public void doCall() {
                ExecSpec execSpec = (ExecSpec) getDelegate();

                execSpec.setWorkingDir(jarFile.getParentFile());
                execSpec.setIgnoreExitValue(false);
                execSpec.setExecutable("jarsigner.exe");
                execSpec.args(
                        "-keystore", getCertificateFile().getAbsolutePath(),
                        "-storepass", getPassword(),
                        jarFile.getName(),    //file to sign in same dir - see ...setDir(File)
                        getAlias()
                );

                if (logger.isInfoEnabled()) {
                    execSpec.args("-verbose");
                }
            }
        });

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

    @Input
    public Map<String, String> getManifestAttributes() {
        return manifestAttributes;
    }

    public void setManifestAttributes(Map<String, String> manifestAttributes) {
        this.manifestAttributes = manifestAttributes;
    }

    public void manifestAttributes(Map<String, String> attibutes) {
        manifestAttributes.putAll(attibutes);
    }

    public void manifestAttribute(String name, String value) {
        manifestAttributes.put(name, value);
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
                return unsignedFiles;
            }

            FileHashIdent certificateFileIdent = new FileHashIdent(getCertificateFile(), FileHashIdent.createChecksum(getCertificateFile(), getAlias()));
            File certDirectory = new File(getCacheDir(), certificateFileIdent.hash());

            List<File> signedFiles = new ArrayList<File>(unsignedFiles.size());
            for (File unsignedFile : unsignedFiles) {
                File signedFile = new File(certDirectory, unsignedFile.getName());
                signedFiles.add(signedFile);
            }

            return signedFiles;
        } catch (Exception e) {
            throw new GradleException("Error calculating (un)signed output files from " + JarSigner.class.getName(), e);
        }
    }
}
