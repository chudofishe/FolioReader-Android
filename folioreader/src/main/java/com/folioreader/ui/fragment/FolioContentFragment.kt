package com.folioreader.ui.fragment

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.folioreader.Config
import com.folioreader.Constants
import com.folioreader.FolioReader
import com.folioreader.R
import com.folioreader.model.DisplayUnit
import com.folioreader.model.event.MediaOverlayPlayPauseEvent
import com.folioreader.model.locators.ReadLocator
import com.folioreader.model.locators.SearchLocator
import com.folioreader.ui.activity.FolioActivity
import com.folioreader.ui.activity.FolioActivityCallback
import com.folioreader.ui.adapter.FolioPageFragmentAdapter
import com.folioreader.ui.view.DirectionalViewpager
import com.folioreader.util.AppUtil
import com.folioreader.util.FileUtil
import org.greenrobot.eventbus.EventBus
import org.readium.r2.shared.Link
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.parser.CbzParser
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.parser.PubBox
import org.readium.r2.streamer.server.Server
import java.lang.ref.WeakReference

class FolioContentFragment : Fragment(), FolioActivityCallback {

    private var direction: Config.Direction = Config.Direction.VERTICAL
    private var mFolioPageViewPager: DirectionalViewpager? = null
    private lateinit var bookFileName: String
    private var mBookId: String? = null
    private var mEpubSourceType: FolioActivity.EpubSourceType? = null
    private var mEpubFilePath: String? = null
    private var mEpubRawId: Int = 0
    private var pubBox: PubBox? = null
    private var portNumber: Int = Constants.DEFAULT_PORT_NUMBER
    private var r2StreamerServer: Server? = null
    private var streamerUri: Uri? = null
    private var spine: List<Link>? = null
    private var searchUri: Uri? = null
    private var currentChapterIndex: Int = 0
    private var mFolioPageFragmentAdapter: FolioPageFragmentAdapter? = null
    private var searchLocator: SearchLocator? = null
    private var entryReadLocator: ReadLocator? = null
    private var lastReadLocator: ReadLocator? = null
    private var density: Float = 0.toFloat()
    private var displayMetrics: DisplayMetrics? = null



    private val currentFragment: FolioPageFragment?
        get() = if (mFolioPageFragmentAdapter != null && mFolioPageViewPager != null) {
            mFolioPageFragmentAdapter!!
                .getItem(mFolioPageViewPager!!.currentItem) as FolioPageFragment
        } else {
            null
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        arguments?.let {
            mBookId = it.getString(FolioReader.EXTRA_BOOK_ID)
            mEpubSourceType = it.getSerializable(FolioActivity.INTENT_EPUB_SOURCE_TYPE) as FolioActivity.EpubSourceType?
            if (mEpubSourceType == FolioActivity.EpubSourceType.RAW) {
                mEpubRawId = it.getInt(FolioActivity.INTENT_EPUB_SOURCE_PATH)
            } else {
                mEpubFilePath = it.getString(FolioActivity.INTENT_EPUB_SOURCE_PATH)
            }
            portNumber = it.getInt(FolioReader.EXTRA_PORT_NUMBER, Constants.DEFAULT_PORT_NUMBER)
            portNumber = AppUtil.getAvailablePortNumber(portNumber)
            entryReadLocator = it.getParcelable(FolioActivity.EXTRA_READ_LOCATOR)
            setConfig(it)
        }

        val display = requireActivity().windowManager.defaultDisplay
        displayMetrics = resources.displayMetrics
        display.getRealMetrics(displayMetrics)
        density = displayMetrics!!.density

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                Constants.getWriteExternalStoragePerms(),
                Constants.WRITE_EXTERNAL_STORAGE_REQUEST
            )
        } else {
            setupBook()
        }

        return inflater.inflate(R.layout.fragment_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configFolio()
    }

    private fun setupBook() {
        Log.v(FolioActivity.LOG_TAG, "-> setupBook")
        initBook()
        onBookInitSuccess()
//        try {
//
//        } catch (e: Exception) {
//            Log.e(FolioActivity.LOG_TAG, "-> Failed to initialize book", e)
//        }
    }

    private fun initBook() {
        Log.v(FolioActivity.LOG_TAG, "-> initBook")

        bookFileName = FileUtil.getEpubFilename(requireContext(), mEpubSourceType!!, mEpubFilePath, mEpubRawId)
        val path = FileUtil.saveEpubFileAndLoadLazyBook(requireContext(), mEpubSourceType, mEpubFilePath,
            mEpubRawId, bookFileName
        )
//        val path = mEpubFilePath
        val extension: Publication.EXTENSION
        var extensionString: String? = null
        try {
            extensionString = FileUtil.getExtensionUppercase(path)
            extension = Publication.EXTENSION.valueOf(extensionString)
        } catch (e: IllegalArgumentException) {
            throw Exception("-> Unknown book file extension `$extensionString`", e)
        }

        pubBox = when (extension) {
            Publication.EXTENSION.EPUB -> {
                val epubParser = EpubParser()
                epubParser.parse(path!!, "")
            }
            Publication.EXTENSION.CBZ -> {
                val cbzParser = CbzParser()
                cbzParser.parse(path!!, "")
            }
            else -> {
                null
            }
        }

        r2StreamerServer = Server(portNumber)
        r2StreamerServer!!.addEpub(
            pubBox!!.publication, pubBox!!.container,
            "/" + bookFileName!!, null
        )

        r2StreamerServer!!.start()

        FolioReader.initRetrofit(streamerUrl)
    }

    private fun onBookInitSuccess() {

        val publication = pubBox!!.publication
        spine = publication.readingOrder

        if (mBookId == null) {
            mBookId = if (!publication.metadata.identifier.isEmpty()) {
                publication.metadata.identifier
            } else {
                if (!publication.metadata.title.isEmpty()) {
                    publication.metadata.title.hashCode().toString()
                } else {
                    bookFileName!!.hashCode().toString()
                }
            }
        }

        // searchUri currently not in use as it's uri is constructed through Retrofit,
        // code kept just in case if required in future.
//        for (link in publication.links) {
//            if (link.rel.contains("search")) {
//                searchUri = Uri.parse("http://" + link.href!!)
//                break
//            }
//        }
        if (searchUri == null)
            searchUri = Uri.parse(streamerUrl + "search")

//        configFolio()
    }

    private fun configFolio() {

        mFolioPageViewPager = view?.findViewById(R.id.folioPageViewPager)
        // Replacing with addOnPageChangeListener(), onPageSelected() is not invoked
        mFolioPageViewPager!!.setOnPageChangeListener(object : DirectionalViewpager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                Log.v(FolioActivity.LOG_TAG, "-> onPageSelected -> DirectionalViewpager -> position = $position")

//                EventBus.getDefault().post(
//                    MediaOverlayPlayPauseEvent(
//                        spine!![currentChapterIndex].href, false, true
//                    )
//                )
//                mediaControllerFragment!!.setPlayButtonDrawable()
                currentChapterIndex = position
            }

            override fun onPageScrollStateChanged(state: Int) {

                if (state == DirectionalViewpager.SCROLL_STATE_IDLE) {
                    val position = mFolioPageViewPager!!.currentItem
                    Log.v(
                        FolioActivity.LOG_TAG, "-> onPageScrollStateChanged -> DirectionalViewpager -> " +
                                "position = " + position
                    )

                    var folioPageFragment = mFolioPageFragmentAdapter!!.getItem(position - 1) as FolioPageFragment?
                    if (folioPageFragment != null) {
                        folioPageFragment.scrollToLast()
                        if (folioPageFragment.mWebview != null)
                            folioPageFragment.mWebview!!.dismissPopupWindow()
                    }

                    folioPageFragment = mFolioPageFragmentAdapter!!.getItem(position + 1) as FolioPageFragment?
                    if (folioPageFragment != null) {
                        folioPageFragment.scrollToFirst()
                        if (folioPageFragment.mWebview != null)
                            folioPageFragment.mWebview!!.dismissPopupWindow()
                    }
                }
            }
        })

        mFolioPageViewPager!!.setDirection(direction)
        mFolioPageFragmentAdapter = FolioPageFragmentAdapter(
            childFragmentManager,
            spine, bookFileName, mBookId
        )
        mFolioPageViewPager!!.adapter = mFolioPageFragmentAdapter

        // In case if SearchActivity is recreated due to screen rotation then FolioActivity
        // will also be recreated, so searchLocator is checked here.
        if (searchLocator != null) {

            currentChapterIndex = getChapterIndex(Constants.HREF, searchLocator!!.href)
            mFolioPageViewPager!!.currentItem = currentChapterIndex
            val folioPageFragment = currentFragment ?: return
            folioPageFragment.highlightSearchLocator(searchLocator!!)
            searchLocator = null

        } else {
            currentChapterIndex = getChapterIndex(entryReadLocator)
            mFolioPageViewPager!!.currentItem = currentChapterIndex
        }
    }

    private fun setConfig(args: Bundle) {
        val intentConfig = args.getParcelable(Config.INTENT_CONFIG) as Config
        val overrideConfig = args.getBoolean(Config.EXTRA_OVERRIDE_CONFIG, false)
        val savedConfig = AppUtil.getSavedConfig(requireContext())


        val config: Config = if (savedConfig == null) {
            intentConfig
        } else {
            if (overrideConfig) {
                intentConfig
            } else {
                savedConfig
            }
        }

        AppUtil.saveConfig(requireContext(), config)
        direction = config.direction
    }

    override fun getStreamerUrl(): String {

        if (streamerUri == null) {
            streamerUri = Uri.parse(String.format(
                Constants.STREAMER_URL_TEMPLATE,
                Constants.LOCALHOST, portNumber, bookFileName))
        }
        return streamerUri.toString()
    }

    private fun getChapterIndex(readLocator: ReadLocator?): Int {

        if (readLocator == null) {
            return 0
        } else if (!TextUtils.isEmpty(readLocator.href)) {
            return getChapterIndex(Constants.HREF, readLocator.href)
        }

        return 0
    }

    private fun getChapterIndex(caseString: String, value: String): Int {
        for (i in spine!!.indices) {
            when (caseString) {
                Constants.HREF -> if (spine!![i].href == value)
                    return i
            }
        }
        return 0
    }

    override fun getCurrentChapterIndex(): Int  = currentChapterIndex

    override fun getEntryReadLocator(): ReadLocator? = entryReadLocator

    override fun goToChapter(href: String): Boolean {
        for (link in spine!!) {
            if (href.contains(link.href!!)) {
                currentChapterIndex = spine!!.indexOf(link)
                mFolioPageViewPager!!.currentItem = currentChapterIndex
                val folioPageFragment = currentFragment
                folioPageFragment!!.scrollToFirst()
                folioPageFragment.scrollToAnchorId(href)
                return true
            }
        }
        return false
    }

    override fun getDirection(): Config.Direction {
        return direction
    }

    override fun onDirectionChange(newDirection: Config.Direction) {
        Log.v(FolioActivity.LOG_TAG, "-> onDirectionChange")

        var folioPageFragment: FolioPageFragment? = currentFragment ?: return
        entryReadLocator = folioPageFragment!!.getLastReadLocator()
        val searchLocatorVisible = folioPageFragment.searchLocatorVisible

        direction = newDirection

        mFolioPageViewPager!!.setDirection(newDirection)
        mFolioPageFragmentAdapter = FolioPageFragmentAdapter(
            childFragmentManager,
            spine, bookFileName, mBookId
        )
        mFolioPageViewPager!!.adapter = mFolioPageFragmentAdapter
        mFolioPageViewPager!!.currentItem = currentChapterIndex

        folioPageFragment = currentFragment ?: return
        searchLocatorVisible?.let {
            folioPageFragment.highlightSearchLocator(searchLocatorVisible)
        }
    }



    override fun storeLastReadLocator(lastReadLocator: ReadLocator?) {
        Log.v(FolioActivity.LOG_TAG, "-> storeLastReadLocator")
        this.lastReadLocator = lastReadLocator
    }

    override fun onTap() {

    }

    override fun setDayMode() {
        TODO("Not yet implemented")
    }

    override fun setNightMode() {
        TODO("Not yet implemented")
    }

    override fun getTopDistraction(unit: DisplayUnit?): Int {
        TODO("Not yet implemented")
    }

    override fun getBottomDistraction(unit: DisplayUnit?): Int {
        TODO("Not yet implemented")
    }

    override fun getViewportRect(unit: DisplayUnit?): Rect {
        val viewportRect = computeViewportRect()
        when (unit) {
            DisplayUnit.PX -> return viewportRect

            DisplayUnit.DP -> {
                viewportRect.left /= density.toInt()
                viewportRect.top /= density.toInt()
                viewportRect.right /= density.toInt()
                viewportRect.bottom /= density.toInt()
                return viewportRect
            }

            DisplayUnit.CSS_PX -> {
                viewportRect.left = Math.ceil((viewportRect.left / density).toDouble()).toInt()
                viewportRect.top = Math.ceil((viewportRect.top / density).toDouble()).toInt()
                viewportRect.right = Math.ceil((viewportRect.right / density).toDouble()).toInt()
                viewportRect.bottom = Math.ceil((viewportRect.bottom / density).toDouble()).toInt()
                return viewportRect
            }

            else -> throw IllegalArgumentException("-> Illegal argument -> unit = $unit")
        }
    }

    private fun computeViewportRect(): Rect {
        return Rect()
    }

}