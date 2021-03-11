package com.wangyunxiu.lib.spinededview

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.widget.AppCompatEditText


class SpinedEditView : AppCompatEditText {
    private var mPopup: ListPopupWindow? = null
    private var mAdapter: ListAdapter? = null
    private var mDropDownAnchorId = 0
    private var mDropDownDrawable: Drawable? = null
    private var mOnDismissListener: PopupWindow.OnDismissListener? = null
    private var mOnShowListener: OnShowListener? = null
    private var mItemClickListener: OnItemClickListener? = null
    private val mItemSelectedListener: OnItemSelectedListener? = null
    private var mItemConverter: ItemConverter? = null
    /**
     * Checks whether the drop-down is dismissed when a item is clicked.
     *
     * @return isDropDownDismissedOnCompletion
     */
    /**
     * Sets whether the drop-down is dismissed when a suggestion is clicked. This is
     * true by default.
     *
     * @param dropDownDismissedOnCompletion Whether to dismiss the drop-down.
     */
    var isDropDownDismissedOnCompletion = true
    private var mLastDismissTime = 0L
    private var mDropDownTouchedDown = false
    private var mOpenBefore = false
    private var mIsEditable = true
    private var mKeyListener: KeyListener? = null

    var dropDownDrawableWidth=0;
    var dropDownDrawableHeight=0;
    var dropDownDrawableSpacing=0;
    constructor(context: Context) : super(context) {
        initFromAttributes(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initFromAttributes(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initFromAttributes(context, attrs, defStyleAttr, 0)
    }

    fun dp2px(dp:Float):Float=dp * context.resources.displayMetrics.density
    fun px2dp(px:Float):Float =px / context.resources.displayMetrics.density

    private fun init() {
        mDropDownDrawable = resources.getDrawable(R.drawable.picker)
        dropDownDrawableWidth=dp2px(25f).toInt()
        dropDownDrawableHeight=dp2px(25f).toInt()
        dropDownDrawableSpacing=dp2px(15f).toInt()
    }

    private fun initFromAttributes(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        init()
        val a = getContext().obtainStyledAttributes(
            attrs,
            R.styleable.SpinedEdit,
            defStyleAttr,
            defStyleRes
        )
        mPopup = ListPopupWindow(context, attrs)
        mPopup!!.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        mPopup!!.promptPosition = ListPopupWindow.POSITION_PROMPT_BELOW
        val selector = a.getDrawable(R.styleable.SpinedEdit_dropDownSelector)
        if (selector != null) {
            mPopup!!.setListSelector(selector)
        }
        val dropDownAnimStyleResId = a.getResourceId(R.styleable.SpinedEdit_dropDownAnimStyle, -1)
        if (dropDownAnimStyleResId > 0) {
            dropDownAnimationStyle = dropDownAnimStyleResId
        }
        var mmDropDownDrawable = a.getDrawable(R.styleable.SpinedEdit_dropDownDrawable)
        if(mmDropDownDrawable!=null){
            mDropDownDrawable=mmDropDownDrawable
        }

        dropDownDrawableSpacing =
            a.getDimensionPixelOffset(R.styleable.SpinedEdit_dropDownDrawableSpacing, 0)
        if (mDropDownDrawable != null) {
             dropDownDrawableWidth =
                a.getDimensionPixelOffset(R.styleable.SpinedEdit_dropDownDrawableWidth, dropDownDrawableWidth)
             dropDownDrawableHeight =
                a.getDimensionPixelOffset(R.styleable.SpinedEdit_dropDownDrawableHeight, dropDownDrawableHeight)
            setDropDownDrawable(mDropDownDrawable!!, dropDownDrawableWidth, dropDownDrawableHeight)
        }

        // Get the anchor's id now, but the view won't be ready, so wait to actually get the
        // view and store it in mDropDownAnchorView lazily in getDropDownAnchorView later.
        // Defaults to NO_ID, in which case the getDropDownAnchorView method will simply return
        // this TextView, as a default anchoring point.
        mDropDownAnchorId = a.getResourceId(
            R.styleable.SpinedEdit_dropDownAnchor,
            NO_ID
        )


        // For dropdown width, the developer can specify a specific width, or MATCH_PARENT
        // (for full screen width) or WRAP_CONTENT (to match the width of the anchored view).
        mPopup!!.width = a.getLayoutDimension(
            R.styleable.SpinedEdit_dropDownWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mPopup!!.height = a.getLayoutDimension(
            R.styleable.SpinedEdit_dropDownHeight,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mPopup!!.setOnItemClickListener(DropDownItemClickListener())
        mPopup!!.setOnDismissListener {
            mLastDismissTime = SystemClock.elapsedRealtime()
            if (mOnDismissListener != null) {
                mOnDismissListener!!.onDismiss()
            }
        }
        a.recycle()
        mIsEditable = keyListener != null
        isFocusable = true
        addTextChangedListener(MyWatcher())
        Log.d(TAG, "mIsEditable = $mIsEditable")
    }

    override fun onCheckIsTextEditor(): Boolean {
        val isEdit = super.onCheckIsTextEditor()
        Log.d(TAG, "onCheckIsTextEditor = $isEdit")
        return isEdit
    }

    /**
     * set whether it can be edited
     *
     * @param isEditable isEditable
     */
    fun setEditable(isEditable: Boolean) {
        if (mIsEditable == isEditable) return
        mIsEditable = isEditable
        if (isEditable) {
            if (mKeyListener != null) {
                keyListener = mKeyListener
            }
        } else {
            mKeyListener = keyListener
            keyListener = null
        }
    }

    /**
     *
     * Sets the current width for the auto-complete drop down list. This can
     * be a fixed width, or [ViewGroup.LayoutParams.MATCH_PARENT] to fill the screen, or
     * [ViewGroup.LayoutParams.WRAP_CONTENT] to fit the width of its anchor view.
     *
     * @param width the width to use
     */
    var dropDownWidth: Int
        get() = mPopup!!.width
        set(width) {
            mPopup!!.width = width
        }

    /**
     *
     * Sets the current height for the auto-complete drop down list. This can
     * be a fixed height, or [ViewGroup.LayoutParams.MATCH_PARENT] to fill
     * the screen, or [ViewGroup.LayoutParams.WRAP_CONTENT] to fit the height
     * of the drop down's content.
     *
     * @param height the height to use
     */
    var dropDownHeight: Int
        get() = mPopup!!.height
        set(height) {
            mPopup!!.height = height
        }
    /**
     *
     * Returns the id for the view that the auto-complete drop down list is anchored to.
     *
     * @return the view's id, or [View.NO_ID] if none specified
     */
    /**
     *
     * Sets the view to which the auto-complete drop down list should anchor. The view
     * corresponding to this id will not be loaded until the next time it is needed to avoid
     * loading a view which is not yet instantiated.
     *
     * @param id the id to anchor the drop down list view to
     */
    var dropDownAnchor: Int
        get() = mDropDownAnchorId
        set(id) {
            mDropDownAnchorId = id
            mPopup!!.anchorView = null
        }
    val dropDownBackground: Drawable?
        get() = mPopup!!.background

    /**
     *
     * Sets the background of the auto-complete drop-down list.
     *
     * @param d the drawable to set as the background
     */
    fun setDropDownBackgroundDrawable(d: Drawable?) {
        mPopup!!.setBackgroundDrawable(d)
    }

    /**
     *
     * Sets the background of the auto-complete drop-down list.
     *
     * @param id the id of the drawable to set as the background
     */
    fun setDropDownBackgroundResource(id: Int) {
        mPopup!!.setBackgroundDrawable(context.resources.getDrawable(id))
    }

    /**
     *
     * Sets the vertical offset used for the auto-complete drop-down list.
     *
     * @param offset the vertical offset
     */
    var dropDownVerticalOffset: Int
        get() = mPopup!!.verticalOffset
        set(offset) {
            mPopup!!.verticalOffset = offset
        }

    /**
     *
     * Sets the horizontal offset used for the auto-complete drop-down list.
     *
     * @param offset the horizontal offset
     */
    var dropDownHorizontalOffset: Int
        get() = mPopup!!.horizontalOffset
        set(offset) {
            mPopup!!.horizontalOffset = offset
        }

    /**
     *
     * Sets the animation style of the auto-complete drop-down list.
     *
     *
     * If the drop-down is showing, calling this method will take effect only
     * the next time the drop-down is shown.
     *
     * @param animationStyle animation style to use when the drop-down appears
     * and disappears.  Set to -1 for the default animation, 0 for no
     * animation, or a resource identifier for an explicit animation.
     */
    var dropDownAnimationStyle: Int
        get() = mPopup!!.animationStyle
        set(animationStyle) {
            mPopup!!.animationStyle = animationStyle
        }

    fun clearListSelection() {
        mPopup!!.clearListSelection()
    }

    var listSelection: Int
        get() = mPopup!!.selectedItemPosition
        set(position) {
            mPopup!!.setSelection(position)
        }

    fun selectItem(index: Int) {
        if (mAdapter != null && index >= 0 && index < mAdapter!!.count) {
            selectItem(mAdapter!!.getItem(index))
        }
    }

    private fun selectItem(selectedItem: Any?) {
        if (selectedItem != null) {
            replaceText(convertSelectionToString(selectedItem))
        }
    }

    fun setDropDownDrawable(drawable: Drawable) {
        setDropDownDrawable(drawable, -1, -1)
    }

    fun setDropDownDrawable(drawable: Drawable, width: Int, height: Int) {
        mDropDownDrawable = drawable
        if (width >= 0 && height >= 0) {
            drawable.bounds = Rect(0, 0, width, height)
            setCompoundDrawables(null, null, drawable, null)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        }
    }

    val isPopupShowing: Boolean
        get() = mPopup!!.isShowing

    fun showDropDown() {
        if (mPopup!!.anchorView == null) {
            if (mDropDownAnchorId != NO_ID) {
                mPopup!!.anchorView = rootView.findViewById(mDropDownAnchorId)
            } else {
                mPopup!!.anchorView = this
            }
        }
        if (!isPopupShowing) {
            // Make sure the list does not obscure the IME when shown for the first time.
            mPopup!!.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        }
        requestFocus()
        mPopup!!.show()
        mPopup!!.listView!!.overScrollMode = OVER_SCROLL_ALWAYS
        if (mOnShowListener != null) {
            mOnShowListener!!.onShow()
        }
    }

    fun dismissDropDown() {
        mPopup!!.dismiss()
    }

    fun setItems(items:List<String>){
        mAdapter= ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,items)
        mPopup!!.setAdapter(mAdapter)
    }
    fun setAdapter(adapter: ListAdapter?) {
        mAdapter = adapter
        mPopup!!.setAdapter(mAdapter)
    }

//    var dropDownDrawableSpacing: Int
//        get() = compoundDrawablePadding
//        set(spacing) {
//            compoundDrawablePadding = spacing
//        }

    fun setOnDismissListener(dismissListener: PopupWindow.OnDismissListener?) {
        mOnDismissListener = dismissListener
    }

    fun setOnShowListener(showListener: OnShowListener?) {
        mOnShowListener = showListener
    }

    fun setOnItemClickListener(l: OnItemClickListener?) {
        mItemClickListener = l
    }

    fun setItemConverter(itemConverter: ItemConverter?) {
        mItemConverter = itemConverter
    }

    protected fun performCompletion(selectedView: View? = null, position: Int = -1, id: Long = -1) {
        var selectedView = selectedView
        var position = position
        var id = id
        if (isPopupShowing) {
            val selectedItem: Any?
            selectedItem = if (position < 0) {
                mPopup!!.selectedItem
            } else {
                mAdapter!!.getItem(position)
            }
            if (selectedItem == null) {
                if (DEBUG) {
                    Log.w(TAG, "performCompletion: no selected item")
                }
                return
            }
            selectItem(selectedItem)
            if (mItemClickListener != null) {
                val list = mPopup
                if (selectedView == null || position < 0) {
                    selectedView = list!!.selectedView
                    position = list.selectedItemPosition
                    id = list.selectedItemId
                }
                mItemClickListener!!.onItemClick(list!!.listView, selectedView, position, id)
            }
        }
        if (isDropDownDismissedOnCompletion) {
            dismissDropDown()
        }
    }

    protected fun replaceText(text: CharSequence?) {
        clearComposingText()
        setText(text)
        // make sure we keep the caret at the end of the text view
        val spannable = getText()
        Selection.setSelection(spannable, spannable!!.length)
    }

    protected fun convertSelectionToString(selectedItem: Any): CharSequence {
        return if (mItemConverter != null) {
            mItemConverter!!.convertItemToString(selectedItem)
        } else {
            selectedItem.toString()
        }
    }

    override fun onDisplayHint(hint: Int) {
        super.onDisplayHint(hint)
        when (hint) {
            INVISIBLE -> dismissDropDown()
        }
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val result = super.setFrame(l, t, r, b)
        if (isPopupShowing) {
            showDropDown()
        }
        return result
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) {
            dismissDropDown()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        dismissDropDown()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent() event = $event")
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInDropDownClickArea(event)) {
                    mDropDownTouchedDown = true
                    return true
                } else {
                    mDropDownTouchedDown = false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mDropDownTouchedDown && isInDropDownClickArea(event)) {
                    if (SystemClock.elapsedRealtime() - mLastDismissTime > TIMEOUT_POPUP_DISMISS) {
                        clearFocus()
                        showDropDown()
                        return true
                    } else {
                        dismissDropDown()
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isInDropDownClickArea(event: MotionEvent): Boolean {
        val areaLeft = if (mIsEditable) width - compoundPaddingRight else 0
        val areaRight = width
        val areaTop = 0
        val areaBottom = height
        if (DEBUG) {
            Log.d(
                TAG, String.format(
                    "x = %d, y = %d, areaLeft = %d, areaRight = %d, areaTop = %d, areaBottom = %d",
                    event.x.toInt(), event.y.toInt(), areaLeft, areaRight, areaTop, areaBottom
                )
            )
        }
        return if (event.x > areaLeft && event.x < areaRight && event.y > areaTop && event.y < areaBottom) {
            true
        } else {
            false
        }
    }

    fun doBeforeTextChanged() {

        // when text is changed, inserted or deleted, we attempt to show
        // the drop down
        mOpenBefore = isPopupShowing
        if (DEBUG) {
            Log.v(
                TAG,
                "before text changed: open=$mOpenBefore"
            )
        }
    }

    fun doAfterTextChanged() {

        // if the list was open before the keystroke, but closed afterwards,
        // then something in the keystroke processing (an input filter perhaps)
        // called performCompletion() and we shouldn't do any more processing.
        if (DEBUG) {
            Log.v(
                TAG, "after text changed: openBefore=" + mOpenBefore
                        + " open=" + isPopupShowing
            )
        }
        if (mOpenBefore && !isPopupShowing) {
            return
        }
        if (isPopupShowing) {
            dismissDropDown()
        }
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing) {
            // special case for the back key, we do not even try to send it
            // to the drop down list but instead, consume it immediately
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = keyDispatcherState
                state?.startTracking(event, this)
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = keyDispatcherState
                state?.handleUpEvent(event)
                if (event.isTracking && !event.isCanceled) {
                    dismissDropDown()
                    return true
                }
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val consumed = mPopup!!.onKeyUp(keyCode, event)
        if (consumed) {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_TAB -> {
                    if (event.hasNoModifiers()) {
                        performCompletion()
                    }
                    return true
                }
            }
        }
        if (isPopupShowing && keyCode == KeyEvent.KEYCODE_TAB && event.hasNoModifiers()) {
            performCompletion()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (mPopup!!.onKeyDown(keyCode, event)) {
            return true
        }
        if (isPopupShowing && keyCode == KeyEvent.KEYCODE_TAB && event.hasNoModifiers()) {
            return true
        }
        val handled = super.onKeyDown(keyCode, event)
        if (handled && isPopupShowing) {
            clearListSelection()
        }
        return handled
    }

    override fun setCompoundDrawables(
        left: Drawable?,
        top: Drawable?,
        right: Drawable?,
        bottom: Drawable?
    ) {
        super.setCompoundDrawables(
            left,
            top,
            if (mDropDownDrawable != null) mDropDownDrawable else right,
            bottom
        )
    }

    /**
     * Convert list item to string, which will be showed in EditText
     */
    interface ItemConverter {
        fun convertItemToString(selectedItem: Any?): String
    }

    /**
     * Callback when popup window showed
     */
    interface OnShowListener {
        fun onShow()
    }

    /**
     * This is used to watch for edits to the text view.
     */
    private inner class MyWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            doAfterTextChanged()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            doBeforeTextChanged()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    private inner class DropDownItemClickListener : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
            performCompletion(v, position, id)
        }
    }

    companion object {
        private const val DEBUG = false
        private const val TAG = "SpinedEdit"
        private const val TIMEOUT_POPUP_DISMISS = 200L
    }
}