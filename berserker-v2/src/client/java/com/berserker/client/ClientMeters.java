package com.berserker.client;

import com.berserker.SyncPayload;

public final class ClientMeters {
	private ClientMeters() {}
	public static volatile SyncPayload current = SyncPayload.EMPTY;
}
