package com.avrapps.pdfviewer.viewer_fragment;

import com.artifex.mupdf.fitz.Quad;

public class SearchTaskResult {
	static private SearchTaskResult singleton;
	public final String txt;
	public final int pageNumber;
	public final Quad[] searchBoxes;

	SearchTaskResult(String _txt, int _pageNumber, Quad[] _searchBoxes) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
	}

	static public SearchTaskResult get() {
		return singleton;
	}

	static public void set(SearchTaskResult r) {
		singleton = r;
	}
}
