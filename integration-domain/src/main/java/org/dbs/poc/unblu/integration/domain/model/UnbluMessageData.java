package org.dbs.poc.unblu.integration.domain.model;

import java.time.Instant;

public record UnbluMessageData(String messageId, String text, String senderPersonId, Instant sentAt) {}
