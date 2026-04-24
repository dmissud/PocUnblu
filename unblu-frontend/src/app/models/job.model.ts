export interface JobTriggerInfo {
  name: string;
  state: string;
  nextFireTime: string | null;
  previousFireTime: string | null;
  cronExpression?: string;
}

export interface JobInfo {
  name: string;
  group: string;
  description: string;
  jobClass: string;
  triggers: JobTriggerInfo[];
}

export interface JobsStatusResponse {
  schedulerName: string;
  schedulerInstanceId: string;
  isStarted: boolean;
  isInStandbyMode: boolean;
  jobs: JobInfo[];
  timestamp: string;
}

export interface RunningJobInfo {
  jobName: string;
  jobGroup: string;
  fireTime: string;
  runTime: number;
}

export interface RunningJobsResponse {
  runningJobs: RunningJobInfo[];
  count: number;
  timestamp: string;
}

// Made with Bob
