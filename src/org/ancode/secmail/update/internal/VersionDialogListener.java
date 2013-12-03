package org.ancode.secmail.update.internal;

public interface VersionDialogListener {
	void doUpdate(boolean laterOnWifi);

	void doIgnore();
}
