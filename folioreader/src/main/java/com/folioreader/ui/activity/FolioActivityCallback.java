package com.folioreader.ui.activity;

import android.graphics.Rect;
import com.folioreader.Config;
import com.folioreader.model.DisplayUnit;
import com.folioreader.model.locators.ReadLocator;

import java.lang.ref.WeakReference;

public interface FolioActivityCallback {

    int getCurrentChapterIndex();

    ReadLocator getEntryReadLocator();

    boolean goToChapter(String href);

    Config.Direction getDirection();

    void onDirectionChange(Config.Direction newDirection);

    void storeLastReadLocator(ReadLocator lastReadLocator);

    void onTap();

    void setDayMode();

    void setNightMode();

    void setTotalPages(Integer pages);

    void onPageChanged(Integer currentPage, Integer totalPages);

    int getTopDistraction(final DisplayUnit unit);

    int getBottomDistraction(final DisplayUnit unit);

    Rect getViewportRect(final DisplayUnit unit);

    String getStreamerUrl();
}
