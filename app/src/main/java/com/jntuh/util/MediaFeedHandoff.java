package com.jntuh.util;

import com.jntuh.item.MediaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight in-memory handoff for opening the full-screen feed from the Explore
 * grid. Passing a large list (with all its fields) through an Intent is fragile and
 * slow; instead the grid drops the already-loaded page here and the viewer picks it
 * up once. Cleared after it's consumed so it can't leak stale data.
 */
public final class MediaFeedHandoff {

    private static List<MediaItem> pendingItems;
    private static int startPosition;
    private static int pageIndex = 1;
    private static int totalPages = 1;
    private static String mediaTypeFilter = "";

    private MediaFeedHandoff() {
    }

    public static void set(List<MediaItem> items, int startPosition, int pageIndex,
                           int totalPages, String mediaTypeFilter) {
        MediaFeedHandoff.pendingItems = (items != null) ? new ArrayList<>(items) : null;
        MediaFeedHandoff.startPosition = startPosition;
        MediaFeedHandoff.pageIndex = pageIndex;
        MediaFeedHandoff.totalPages = totalPages;
        MediaFeedHandoff.mediaTypeFilter = (mediaTypeFilter != null) ? mediaTypeFilter : "";
    }

    public static boolean hasData() {
        return pendingItems != null && !pendingItems.isEmpty();
    }

    public static List<MediaItem> getItems() {
        return pendingItems;
    }

    public static int getStartPosition() {
        return startPosition;
    }

    public static int getPageIndex() {
        return pageIndex;
    }

    public static int getTotalPages() {
        return totalPages;
    }

    public static String getMediaTypeFilter() {
        return mediaTypeFilter;
    }

    public static void clear() {
        pendingItems = null;
        startPosition = 0;
        pageIndex = 1;
        totalPages = 1;
        mediaTypeFilter = "";
    }
}
