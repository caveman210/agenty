package com.agenty.triage.agent;

/* *
 * Makes up record formats for the Incident
 * Payload to look uniform? Might need to look
 * into it.
 * */

public record IncidentPayload (
	String service,
	String logs,
	String environment
	) {}
