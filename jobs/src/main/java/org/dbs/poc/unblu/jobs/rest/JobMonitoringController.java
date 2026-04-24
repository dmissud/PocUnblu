package org.dbs.poc.unblu.jobs.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur REST pour le monitoring des jobs Quartz.
 *
 * <p>Expose des endpoints pour consulter l'état et l'historique des jobs planifiés.</p>
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobMonitoringController {

    private final Scheduler scheduler;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Récupère le statut de tous les jobs Quartz.
     *
     * @return Liste des jobs avec leur statut
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJobsStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> jobs = new ArrayList<>();

            // Récupérer tous les jobs
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    Map<String, Object> jobInfo = new HashMap<>();

                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                    jobInfo.put("name", jobKey.getName());
                    jobInfo.put("group", jobKey.getGroup());
                    jobInfo.put("description", jobDetail.getDescription());
                    jobInfo.put("jobClass", jobDetail.getJobClass().getSimpleName());

                    // Informations sur les triggers
                    List<Map<String, Object>> triggerInfos = new ArrayList<>();
                    for (Trigger trigger : triggers) {
                        Map<String, Object> triggerInfo = new HashMap<>();
                        triggerInfo.put("name", trigger.getKey().getName());
                        triggerInfo.put("state", scheduler.getTriggerState(trigger.getKey()).name());

                        Date nextFireTime = trigger.getNextFireTime();
                        Date previousFireTime = trigger.getPreviousFireTime();

                        triggerInfo.put("nextFireTime", nextFireTime != null ?
                                FORMATTER.format(nextFireTime.toInstant()) : null);
                        triggerInfo.put("previousFireTime", previousFireTime != null ?
                                FORMATTER.format(previousFireTime.toInstant()) : null);

                        if (trigger instanceof CronTrigger cronTrigger) {
                            triggerInfo.put("cronExpression", cronTrigger.getCronExpression());
                        }

                        triggerInfos.add(triggerInfo);
                    }

                    jobInfo.put("triggers", triggerInfos);
                    jobs.add(jobInfo);
                }
            }

            response.put("schedulerName", scheduler.getSchedulerName());
            response.put("schedulerInstanceId", scheduler.getSchedulerInstanceId());
            response.put("isStarted", scheduler.isStarted());
            response.put("isInStandbyMode", scheduler.isInStandbyMode());
            response.put("jobs", jobs);
            response.put("timestamp", FORMATTER.format(Instant.now()));

            return ResponseEntity.ok(response);

        } catch (SchedulerException e) {
            log.error("Erreur lors de la récupération du statut des jobs", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Impossible de récupérer le statut des jobs: " + e.getMessage()));
        }
    }

    /**
     * Récupère les jobs actuellement en cours d'exécution.
     *
     * @return Liste des jobs en cours d'exécution
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> getRunningJobs() {
        try {
            List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
            List<Map<String, Object>> jobInfos = new ArrayList<>();

            for (JobExecutionContext context : runningJobs) {
                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("jobName", context.getJobDetail().getKey().getName());
                jobInfo.put("jobGroup", context.getJobDetail().getKey().getGroup());
                jobInfo.put("fireTime", FORMATTER.format(context.getFireTime().toInstant()));
                jobInfo.put("runTime", context.getJobRunTime());
                jobInfos.add(jobInfo);
            }

            return ResponseEntity.ok(Map.of(
                    "runningJobs", jobInfos,
                    "count", jobInfos.size(),
                    "timestamp", FORMATTER.format(Instant.now())
            ));

        } catch (SchedulerException e) {
            log.error("Erreur lors de la récupération des jobs en cours", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Impossible de récupérer les jobs en cours: " + e.getMessage()));
        }
    }
}

// Made with Bob
