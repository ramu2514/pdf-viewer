package com.avrapps.pdfviewer.viewer_fragment;

public interface CancellableTaskDefinition<Params, Result> {
	Result doInBackground(Params... params);

	void doCancel();

	void doCleanup();
}
