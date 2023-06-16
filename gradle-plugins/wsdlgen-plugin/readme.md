# WSDL genererings plugin
Plugin som oppretter en task `wsdlGen` som generer WSDL-er fra `main`-sourcsettets runtime-og-compile-classpath.

## Bruk
```groovy
plugins {
    id 'sktools.wsdlgen'
}
```
Man må selv velge hva som skal gjøres med taskens output, f.eks
```groovy
tasks.jar {
    from (tasks.wsdlGen)
}
```

Changelog
------------
## Unreleased Changes

## 6.0 Release Notes
* [SKTOOLS-246] Bumper standard jaxws versjon til `2.3.6`



## Standardverdier / Tilpassningsmuligheter
```groovy
tasks.wsdlGen {
    // Classpath der klasser man skal generere WSDL-er fra finnes
    compileClasspath = sourceSets.main.runtimeClasspath + sourceSets.main.compileClasspath
    // Målmappe
    destinationDirectory = files("$buildDir/wsdlGen")
}
```

## Manuelt oppsett
Alternativt så kan man gjøre
```groovy
plugins {
    id 'sktools.wsdlgen' apply: false
}
```
og opprette en task
```groovy
task myWsdlGen(type: no.statkart.sktools.gradle.plugins.wsdlgen.WsdlGenTask) {
    // ... se standardverdier for oppsett
}
```
