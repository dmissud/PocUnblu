package org.dbs.poc.unblu.jobs.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbs.poc.unblu.integration.domain.model.statistics.ConversationStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour accéder aux fichiers de statistiques.
 *
 * <p>Permet de lister les fichiers de statistiques disponibles et de récupérer
 * leur contenu pour affichage dans le frontend.</p>
 */
@RestController
@RequestMapping("/api/jobs/statistics")
@Tag(name = "Statistics", description = "API pour accéder aux statistiques de conversations")
@Slf4j
public class StatisticsController {

    private final String statisticsDirectory;
    private final ObjectMapper objectMapper;

    public StatisticsController(
            @Value("${statistics.output.directory:./statistics}") String statisticsDirectory) {
        this.statisticsDirectory = statisticsDirectory;
        this.objectMapper = createObjectMapper();
    }

    /**
     * Liste tous les fichiers de statistiques disponibles.
     *
     * @return Liste des noms de fichiers de statistiques
     */
    @GetMapping("/files")
    @Operation(summary = "Liste les fichiers de statistiques",
               description = "Retourne la liste de tous les fichiers de statistiques disponibles")
    public ResponseEntity<List<String>> listStatisticsFiles() {
        try {
            Path directory = Paths.get(statisticsDirectory);

            if (!Files.exists(directory)) {
                log.warn("Le répertoire de statistiques n'existe pas: {}", directory.toAbsolutePath());
                return ResponseEntity.ok(List.of());
            }

            try (Stream<Path> paths = Files.list(directory)) {
                List<String> files = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString())
                        .sorted((a, b) -> b.compareTo(a)) // Tri décroissant (plus récent en premier)
                        .collect(Collectors.toList());

                log.info("Fichiers de statistiques trouvés: {}", files.size());
                return ResponseEntity.ok(files);
            }
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du répertoire de statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère le contenu d'un fichier de statistiques spécifique.
     *
     * @param filename Nom du fichier de statistiques
     * @return Contenu du fichier de statistiques
     */
    @GetMapping("/files/{filename}")
    @Operation(summary = "Récupère un fichier de statistiques",
               description = "Retourne le contenu d'un fichier de statistiques spécifique")
    public ResponseEntity<ConversationStatistics> getStatisticsFile(@PathVariable String filename) {
        try {
            // Validation du nom de fichier pour éviter les path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Tentative d'accès à un fichier invalide: {}", filename);
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(statisticsDirectory, filename);

            if (!Files.exists(filePath)) {
                log.warn("Fichier de statistiques non trouvé: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // Lire et désérialiser le fichier JSON
            String content = Files.readString(filePath);
            ConversationStatistics statistics = objectMapper.readValue(content, ConversationStatistics.class);

            log.info("Fichier de statistiques récupéré: {}", filename);
            return ResponseEntity.ok(statistics);
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier de statistiques: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
}

// Made with Bob
