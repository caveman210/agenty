package com.agenty.triage.agent;

public record TriageResult (
		String severity,
		String causeCategory,
		String actionRec,
		double interferenceLatency
		){}
