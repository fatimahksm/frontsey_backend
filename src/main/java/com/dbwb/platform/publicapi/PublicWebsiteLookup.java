package com.dbwb.platform.publicapi;

import java.util.UUID;

/**
 * The public payload plus the id of the website it came from.
 *
 * The controller needs both: the envelope to answer with, and the id to
 * attribute the visit to. It used to get them from two separate calls, so
 * every public page load resolved the same slug twice. The id is null exactly
 * when no website matched the slug - there is then nothing to record a visit
 * against.
 */
public record PublicWebsiteLookup(UUID websiteId, PublicWebsiteEnvelope envelope) {
}
