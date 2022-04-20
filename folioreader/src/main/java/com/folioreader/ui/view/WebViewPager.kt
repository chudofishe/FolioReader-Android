package com.folioreader.ui.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.OneShotPreDrawListener
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.folioreader.Config
import com.folioreader.R
import com.folioreader.model.event.ReloadDataEvent
import com.folioreader.util.AppUtil
import org.greenrobot.eventbus.EventBus

class WebViewPager : ViewPager {

    companion object {
        @JvmField
        val LOG_TAG: String = WebViewPager::class.java.simpleName
    }

    private var horizontalPageCount: Int = 0
    var folioWebView: FolioWebView? = null
    private var takeOverScrolling: Boolean = false
    var isScrolling: Boolean = false
        private set
    private var uiHandler: Handler? = null
    private var gestureDetector: GestureDetectorCompat? = null

    private var lastGestureType: LastGestureType? = null
    private var scrollState: Int = 0

    private var initialPageSet = false

    private var initialPage = 0

    private lateinit var config: Config

    private enum class LastGestureType {
        OnSingleTapUp, OnLongPress, OnFling, OnScroll
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        uiHandler = Handler()
        gestureDetector = GestureDetectorCompat(context, GestureListener())

        val config = AppUtil.getSavedConfig(context)
        this.config = config!!

        addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // Log.d(LOG_TAG, "-> onPageScrolled -> position = " + position +
                // ", positionOffset = " + positionOffset + ", positionOffsetPixels = " + positionOffsetPixels);
//                if (!initialPageSet) {
//                    initialPageSet = true
//                    currentItem = 5
//                }

                isScrolling = true

                if (position == horizontalPageCount - 1 && scrollState == SCROLL_STATE_DRAGGING) {
                    folioWebView?.onNextChapter()
                }

                if (position == 0 && scrollState == SCROLL_STATE_DRAGGING && positionOffset == 0f) {
                    folioWebView?.onPreviousChapter()
                }

                if (takeOverScrolling && folioWebView != null) {
                    val scrollX = folioWebView!!.getScrollXPixelsForPage(position) + positionOffsetPixels
                    //Log.d(LOG_TAG, "-> onPageScrolled -> scrollX = " + scrollX);
                    folioWebView!!.scrollTo(scrollX, 0)
                }

                if (positionOffsetPixels == 0) {
                    //Log.d(LOG_TAG, "-> onPageScrolled -> takeOverScrolling = false");
                    takeOverScrolling = false
                    isScrolling = false
                }
            }

            override fun onPageSelected(position: Int) {
                Log.v(LOG_TAG, "-> onPageSelected -> $position")
//                currentItem = 5
                folioWebView?.onPageChanged(position, horizontalPageCount)
            }

            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
            }
        })
    }

    fun calculatePage(progress: Float, totalPages: Int): Int {
        val res: Int
        if (progress != 0f || progress != 1f) {
            for (i in 2 .. totalPages) {
                if (i >= (i - 1 / totalPages).toFloat() && i <= (i + 1 / totalPages).toFloat()) {
                    return i
                }
            }
        }
        return if (progress == 1f) 1 else 0
    }

    fun setHorizontalPageCount(horizontalPageCount: Int) {
        //Log.d(LOG_TAG, "-> horizontalPageCount = " + horizontalPageCount);

        this.horizontalPageCount = horizontalPageCount

        adapter = WebViewPagerAdapter(populateViewsList())

        if (folioWebView == null)
            folioWebView = (parent as View).findViewById(R.id.folioWebView)

        if (config.progress == -2f) {
            initialPage = horizontalPageCount - 1
            folioWebView!!.loadUrl("javascript:scrollToLast()")
        } else {
            initialPage = config.progress.toInt()
        }
        uiHandler!!.post {currentItem = initialPage}
    }

    private fun populateViewsList(): List<View> {
        val list = mutableListOf<View>()
        repeat(horizontalPageCount) {
            val view = LayoutInflater.from(this.context)
                .inflate(R.layout.view_webview_pager, this, false)
            list.add(view)
        }
        return list
    }

    @JavascriptInterface
    fun setCurrentPage(pageIndex: Int) {
        Log.v(LOG_TAG, "-> setCurrentItem -> pageIndex = $pageIndex")
//        uiHandler!!.post {currentItem = pageIndex}
    }

    @JavascriptInterface
    fun setPageToLast() {

        uiHandler!!.post { currentItem = horizontalPageCount - 1 }
    }

    @JavascriptInterface
    fun setPageToFirst() {

        uiHandler!!.post { currentItem = 0 }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
            super@WebViewPager.onTouchEvent(e)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            //Log.d(LOG_TAG, "-> onSingleTapUp");
            lastGestureType = LastGestureType.OnSingleTapUp
            return false
        }

        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
            //Log.d(LOG_TAG, "-> onLongPress -> " + e);
            lastGestureType = LastGestureType.OnLongPress
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            //Log.v(LOG_TAG, "-> onScroll -> e1 = " + e1 + ", e2 = " + e2 + ", distanceX = " + distanceX + ", distanceY = " + distanceY);
            lastGestureType = LastGestureType.OnScroll
            return false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            //Log.d(LOG_TAG, "-> onFling -> e1 = " + e1 + ", e2 = " + e2 + ", velocityX = " + velocityX + ", velocityY = " + velocityY);
            lastGestureType = LastGestureType.OnFling
            return false
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //Log.d(LOG_TAG, "-> onTouchEvent -> " + AppUtil.actionToString(event.getAction()));

        if (event == null)
            return false

        // Rare condition in fast scrolling
        if (gestureDetector == null)
            return false

        val gestureReturn = gestureDetector!!.onTouchEvent(event)
        if (gestureReturn)
            return true

        val superReturn = super.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            if (lastGestureType == LastGestureType.OnScroll || lastGestureType == LastGestureType.OnFling) {
                //Log.d(LOG_TAG, "-> onTouchEvent -> takeOverScrolling = true, " + "lastGestureType = " + lastGestureType);
                takeOverScrolling = true
            }
            lastGestureType = null
        }

        return superReturn
    }

    inner class WebViewPagerAdapter(private val viewsList: List<View>) : PagerAdapter() {


        override fun getCount(): Int {
            return viewsList.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = viewsList[position]
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }
}
