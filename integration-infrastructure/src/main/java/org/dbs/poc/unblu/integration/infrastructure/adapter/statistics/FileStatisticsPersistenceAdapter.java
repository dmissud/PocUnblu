package org.dbs.poc.unblu.integration.infrastructure.adapter.statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;
import org.dbs.poc.unblu.integration.domain.port.out.StatisticsPersistencePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Adaptateur de persistance des statistiques dans un fichier JSON (Bloc 1 — Integration).
 *
 * <p>Sauvegarde les statistiques calculées dans un fichier JSON horodaté
 * dans le répertoire configuré.</p>
 */
@Component
@Slf4j
public class FileStatisticsPersistenceAdapter implements StatisticsPersistencePort {

    private final String outputDirectory;
    private final String filenamePattern;
    private final ObjectMapper objectMapper;

    public FileStatisticsPersistenceAdapter(
            @Value("${statistics.output.directory:./statistics}") String outputDirectory,
            @Value("${statistics.output.filename-pattern:conversation-stats-%s.json}") String filenamePattern) {
        this.outputDirectory = outputDirectory;
        this.filenamePattern = filenamePattern;
        this.objectMapper = createObjectMapper();

        // Créer le répertoire de sortie s'il n'existe pas
        try {
            Path directory = Paths.get(outputDirectory);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Répertoire de statistiques créé: {}", directory.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Erreur lors de la création du répertoire de statistiques: {}", outputDirectory, e);
        }
    }

    @Override
    public void save(ConversationStatistics statistics) {
        try {
            String filename = generateFilename(statistics.generatedAt());
            Path filePath = Paths.get(outputDirectory, filename);

            // Sérialiser en JSON avec indentation
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(statistics);

            // Écrire dans le fichier
            Files.writeString(filePath, json);

            log.info("Statistiques sauvegardées dans: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde des statistiques", e);
            throw new StatisticsPersistenceException("Impossible de sauvegarder les statistiques", e);
        }
    }

    /**
     * Génère le nom de fichier basé sur la date et l'heure.
     * Format: conversation-stats-YYYY-MM-DD_HH-mm.json
     */
    private String generateFilename(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String dateTimeStr = now.format(formatter);
        return String.format(filenamePattern, dateTimeStr);
    }

    /**
     * Crée un ObjectMapper configuré pour les types Java Time.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Exception levée en cas d'erreur de persistance.
     */
    public static class StatisticsPersistenceException extends RuntimeException {
        public StatisticsPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// Made with Bob
