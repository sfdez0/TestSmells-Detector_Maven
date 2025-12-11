package com.tsdmvn;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// Se enlaza automáticamente a la fase de TEST
@Mojo(name = "tsdetect", defaultPhase = LifecyclePhase.TEST)
public class TestSmellMojo extends AbstractMojo {

    // Parámetro para la ruta del archivo CSV de entrada
    @Parameter(property = "rutaCsv", defaultValue = "${project.basedir}/test-list.csv")
    private File archivoCsv;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("--- Iniciando Análisis de Test Smells ---");

        // Comprobamos que existe el CSV de entrada para tsDetect
        if (!archivoCsv.exists()) {
            getLog().warn("No se encontró el archivo CSV en: " + archivoCsv.getAbsolutePath());
            getLog().warn("Por favor, crea el archivo o configura la ruta en el pom.xml");
            return;
        }

        try {
            // Extraemos el JAR desde los recursos del plugin a un archivo temporal
            File tempJar = extractJarFromResources();

            // Construimos el comando para ejecutar tsDetect
            // Equivalente a: java -jar /tmp/TestSmellDetector.jar -p /ruta/del/proyecto
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(tempJar.getAbsolutePath());
            command.add(archivoCsv.getAbsolutePath());

            // Configuramos el proceso
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            // Ejecutamos el proceso
            Process process = builder.start();

            // Leemos la salida y la mostramos en la consola de Maven
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().info("[tsDetect] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                getLog().warn("tsDetect finalizó con código: " + exitCode);
            } else {
                getLog().info("Análisis de Test Smells finalizado.");
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error crítico ejecutando el plugin", e);
        }
    }

    /**
     * Método auxiliar para sacar el .jar de dentro del plugin y ponerlo en el sistema de archivos
     */
    private File extractJarFromResources() throws IOException, MojoExecutionException {
        // La ruta debe coincidir con lo que pusiste en src/main/resources
        InputStream link = getClass().getResourceAsStream("/TestSmellDetector.jar");
        
        if (link == null) {
            throw new MojoExecutionException("No se encontró el archivo /TestSmellDetector.jar dentro de los recursos del plugin.");
        }

        // Crear archivo temporal
        File tempFile = File.createTempFile("TestSmellDetector_tool", ".jar");
        tempFile.deleteOnExit(); // Se borrará automáticamente cuando acabe Maven

        // Copiar contenido
        Files.copy(link, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        getLog().debug("Herramienta extraída en: " + tempFile.getAbsolutePath());
        return tempFile;
    }
}
