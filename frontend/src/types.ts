export type ServiceState = 'UP' | 'DEGRADED';

export type ServiceStatus = {
  name: 'frontend' | 'backend' | 'database' | 'ai' | string;
  status: ServiceState;
  message: string;
};

export type SystemHealth = {
  status: ServiceState;
  checkedAt: string;
  services: ServiceStatus[];
};
