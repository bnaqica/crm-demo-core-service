package com.bnaqica.crmdemocoreservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs and Boot's {@code TaskScheduler}, used by the outbox relay and catalog retries. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
