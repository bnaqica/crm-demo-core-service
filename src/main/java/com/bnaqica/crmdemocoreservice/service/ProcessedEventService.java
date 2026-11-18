package com.bnaqica.crmdemocoreservice.service;

import java.util.UUID;

/** CRUD for {@code processed_event}. Joins the caller's transaction and fails without one. */
public interface ProcessedEventService {

    /** Records the event id; false if it was already recorded, so the event must be skipped. */
    boolean markProcessed(UUID eventId);
}
