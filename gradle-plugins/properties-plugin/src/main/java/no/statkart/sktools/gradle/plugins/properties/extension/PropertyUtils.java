package no.statkart.sktools.gradle.plugins.properties.extension;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension for enkel håndtering av properties i prosjekter. Denne kan utvide Gradle sin DSL.
 *
 * @author Leif Lislegård
 * @since 1.2
 */
public class PropertyUtils {
    private static final Logger LOG = Logging.getLogger(PropertyUtils.class);
    static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

    private final Project project;
    private boolean strict;

    public PropertyUtils(Project project) {
        this.project = project;
        strict = project != project.getRootProject();   //root kan ha properties som ikke er expandet.
    }



    /**
     * Convenient way of retrieving project properties
     */
    public Map<String, Object> projectProperties() {
        HashMap<String, Object> filteredProjectProperties = new HashMap<String, Object>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof CharSequence) {
                filteredProjectProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return filteredProjectProperties;
    }

    /**
     * Leser inn properties ifra fil.
     *
     * Merk at dersom samme property blir lest inn flere ganger så er det den siste parameteriserte filen som har presedens.
     *
     * @param path Samme som {@link Project#file(Object)}.
     * @return properties lest ifra fil(er), eller tomt map dersom ingen filer funnet.
     */
    public Map<String, ?> fromFile(Object... path) throws IOException {
        Properties props = new Properties();
        for (Object o : path) {
            File file = project.file(o);
            if (file.exists()) {
                props.load(new FileReader(file));
            }
        }

        return (Map) props;
    }


    /**
     * Legger properties til prosjektet.
     * @param propertiess properties som ønskes satt på prosjektet
     */
    public void assignPropertiesToProject(Map<String, ?>... propertiess) {
        ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();

        for (Map<String, ?> properties : propertiess) {
            for (Map.Entry<String, ?> entry : properties.entrySet()) {
                ext.set(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Ekspanderer alle properties registrert for prosjektet.
     */
    public void expandProjectProperties() {
        expandProjectProperties(strict);
    }
    /**
     * Ekspanderer alle properties registrert for prosjektet.
     * <p/>
     * Dersom {@link #strict} så vil exception kastes dersom ikke alle properties kunne resolves.
     */
    public void expandProjectProperties(boolean strict) {
        LOG.debug("expanding properties for project {}", project.getPath());
        ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();
        Matcher matcher = null; //felles matcher instans

        //plukker ut interessange properties
        HashMap<String, String> props = new HashMap<String, String>();
        for (Map.Entry<String, ?> entry : project.getExtensions().getExtraProperties().getProperties().entrySet()) { //SKTOOLS-70: ta ikke med properties ifra prosjektets vedhengsobjekter (extensions)
            Object value = entry.getValue();
            if (value instanceof CharSequence) {
                matcher = (matcher == null) ? SUBSTITUTION_PATTERN.matcher((CharSequence) value) : matcher.reset((CharSequence) value);
                if (matcher.find()) {
                    props.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        expandPropertiesImpl(strict, props);

        for (Map.Entry<String, ?> entry : props.entrySet()) {
            //oppdaterer ext properties, eller property for prosjekt
            if (ext.has(entry.getKey())) {
                ext.set(entry.getKey(), entry.getValue());
            } else {
                project.setProperty(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Ekspanderer alle properties. Verdier vil bli forsøkt søkt opp via parameterisert sett så via prosjektet.
     * @param properties properties som skal ekspanderes
     * @throws GradleException dersom {@link #strict} og ikke alle properties kunne expandes
     */
    public void expandProperties(Map properties) throws GradleException {
        LOG.debug("expanding custom properties on project {}", project.getPath());
        expandPropertiesImpl(strict, properties);
    }

    public void expandProperties(Map properties, boolean strict) throws GradleException {
        LOG.debug("expanding custom properties on project {}", project.getPath());
        expandPropertiesImpl(strict, properties);
    }


    /**
     * TODO: Løse problem med sirkulære property referanser.
     * Annta at props har følgende verdier:
     * key1=${key2}
     * key2=${key3}
     * key3=${key1}
     */
    private void expandPropertiesImpl(boolean strict, Map<String, String> props) {
        LOG.debug("strict mode is {}", strict);

        Matcher matcher = null; //felles matcher instans

        if (props.isEmpty()) {
            return; // no properties to expand
        }

        //expanderer props
        while (true) {
            LinkedHashMap<String, Object> unresolvedProperties = new LinkedHashMap<String, Object>();
            HashMap<String, Object> resolvedProperties = new HashMap<String, Object>();

            for (Map.Entry<String, String> entry : props.entrySet()) {
                String value = entry.getValue();
                matcher = (matcher == null) ? SUBSTITUTION_PATTERN.matcher(value) : matcher.reset(value);

                StringBuffer expandedValue = null; //null if substitution is unresolved

                if (matcher.find()) {
                    do {
                        if (expandedValue == null) expandedValue = new StringBuffer();
                        String propertyName = matcher.group(1);
                        boolean isSame = propertyName.equals(entry.getKey());
                        String replacement = null;
                        if (!isSame && resolvedProperties.containsKey(propertyName)) {
                            replacement = resolvedProperties.get(propertyName).toString();
                        } else if (!isSame && props.containsKey(propertyName)) {
                            replacement = props.get(propertyName).toString();
                        } else if (project.hasProperty(propertyName)) {
                            replacement = project.property(propertyName).toString(); //SKTOOLS-70: ta ikke med properties ifra prosjektets vedhengsobjekter (extensions)
                        } else {
                            unresolvedProperties.put(entry.getKey(), value);
                            expandedValue = null;
                            break;  //unresolved
                        }
                        matcher.appendReplacement(expandedValue, Matcher.quoteReplacement(replacement));
                    } while (matcher.find());

                    if (expandedValue != null) {
                        matcher.appendTail(expandedValue);
                        resolvedProperties.put(entry.getKey(), expandedValue.toString());
                    }
                }

            }

            if (resolvedProperties.isEmpty()) {
                if (unresolvedProperties.isEmpty()) {
                    break; //alt resolvet
                } else {
                    if (strict) {
                        String msg = String.format("Unable to resolve top-level properties: %s \n due to unresolved references to: %s", unresolvedProperties.keySet(), unresolvedProperties.values());
                        LOG.error(msg);
                        throw new GradleException(msg);
                    }
                }
            }

            //oppdaterer resolved properties til prosjektet
            for (Map.Entry<String, Object> entry : resolvedProperties.entrySet()) {
                LOG.debug("...expanding property {} -> {}", entry.getKey(), entry.getValue());
                props.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }


}
