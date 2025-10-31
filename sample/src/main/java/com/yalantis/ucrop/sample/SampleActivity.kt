package com.yalantis.ucrop.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.net.toUri
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Companion.getError
import com.yalantis.ucrop.UCrop.Companion.getOutput
import com.yalantis.ucrop.UCrop.Companion.of
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragment.UCropResult
import com.yalantis.ucrop.UCropFragmentCallback
import com.yalantis.ucrop.sample.ResultActivity.Companion.startWithUri
import java.io.File
import java.util.Locale
import java.util.Objects
import java.util.Random

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
@Suppress("NAME_SHADOWING")
class SampleActivity : BaseActivity(), UCropFragmentCallback {
    private var mRadioGroupAspectRatio: RadioGroup? = null
    private var mRadioGroupCompressionSettings: RadioGroup? = null
    private var mEditTextMaxWidth: EditText? = null
    private var mEditTextMaxHeight: EditText? = null
    private var mEditTextRatioX: EditText? = null
    private var mEditTextRatioY: EditText? = null
    private var mCheckBoxMaxSize: CheckBox? = null
    private var mSeekBarQuality: SeekBar? = null
    private var mTextViewQuality: TextView? = null
    private var mCheckBoxHideBottomControls: CheckBox? = null
    private var mCheckBoxFreeStyleCrop: CheckBox? = null
    private var toolbar: Toolbar? = null
    private var settingsView: ScrollView? = null
    private val requestMode = BuildConfig.RequestMode
    private var fragment: UCropFragment? = null
    private var mShowLoader = false
    private var mToolbarTitle: String? = null

    @DrawableRes
    private var mToolbarCancelDrawable = 0

    @DrawableRes
    private var mToolbarCropDrawable = 0

    // Enables dynamic coloring
    private var mToolbarColor = 0
    private var mStatusBarColor = 0
    private var mToolbarWidgetColor = 0
    private var mCheckBoxBrigtness: CheckBox? = null
    private var mCheckBoxContrast: CheckBox? = null
    private var mCheckBoxSaturation: CheckBox? = null
    private var mCheckBoxSharpness: CheckBox? = null
    private var mRadioGroupTitlebarTextGravity: RadioGroup? = null
    private var mEditTextTitlebarTextSize: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        setupUI()
    }

    private val mAspectRatioTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            mRadioGroupAspectRatio!!.clearCheck()
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }
    private val mMaxSizeTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            if (s.toString().trim { it <= ' ' }.isNotEmpty()) {
                if (Integer.valueOf(s.toString()) < UCrop.MIN_SIZE) {
                    Toast.makeText(
                        this@SampleActivity,
                        String.format(
                            getString(R.string.format_max_cropped_image_size),
                            UCrop.MIN_SIZE
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupUI() {
        findViewById<View>(R.id.button_crop).setOnClickListener { pickFromGallery() }
        findViewById<View>(R.id.button_random_image).setOnClickListener {
            val random = Random()
            val minSizePixels = 800
            val maxSizePixels = 2400
            val uri = String.format(
                Locale.getDefault(), "https://unsplash.it/%d/%d/?random",
                minSizePixels + random.nextInt(maxSizePixels - minSizePixels),
                minSizePixels + random.nextInt(maxSizePixels - minSizePixels)
            ).toUri()
            startCrop(uri)
        }
        settingsView = findViewById(R.id.settings)
        mRadioGroupAspectRatio = findViewById(R.id.radio_group_aspect_ratio)
        mRadioGroupCompressionSettings = findViewById(R.id.radio_group_compression_settings)
        mCheckBoxMaxSize = findViewById(R.id.checkbox_max_size)
        mEditTextRatioX = findViewById(R.id.edit_text_ratio_x)
        mEditTextRatioY = findViewById(R.id.edit_text_ratio_y)
        mEditTextMaxWidth = findViewById(R.id.edit_text_max_width)
        mEditTextMaxHeight = findViewById(R.id.edit_text_max_height)
        mSeekBarQuality = findViewById(R.id.seekbar_quality)
        mTextViewQuality = findViewById(R.id.text_view_quality)
        mCheckBoxHideBottomControls = findViewById(R.id.checkbox_hide_bottom_controls)
        mCheckBoxFreeStyleCrop = findViewById(R.id.checkbox_freestyle_crop)
        mCheckBoxBrigtness = findViewById(R.id.checkbox_brightness)
        mCheckBoxContrast = findViewById(R.id.checkbox_contrast)
        mCheckBoxSaturation = findViewById(R.id.checkbox_saturation)
        mCheckBoxSharpness = findViewById(R.id.checkbox_sharpness)
        mRadioGroupTitlebarTextGravity =
            findViewById(R.id.radio_group_titlebar_text_gravity_settings)
        mEditTextTitlebarTextSize = findViewById(R.id.edit_text_toolbar_title_size)
        mRadioGroupAspectRatio!!.check(R.id.radio_dynamic)
        mEditTextRatioX!!.addTextChangedListener(mAspectRatioTextWatcher)
        mEditTextRatioY!!.addTextChangedListener(mAspectRatioTextWatcher)
        mRadioGroupCompressionSettings!!.setOnCheckedChangeListener { _, checkedId ->
            mSeekBarQuality!!.isEnabled = checkedId == R.id.radio_jpeg
        }
        mRadioGroupCompressionSettings!!.check(R.id.radio_jpeg)
        mSeekBarQuality!!.progress = UCropActivity.DEFAULT_COMPRESS_QUALITY
        mTextViewQuality!!.text = String.format(
            getString(R.string.format_quality_d),
            mSeekBarQuality!!.progress
        )
        mSeekBarQuality!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mTextViewQuality!!.text = String.format(
                    getString(R.string.format_quality_d),
                    progress
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        mEditTextMaxHeight!!.addTextChangedListener(mMaxSizeTextWatcher)
        mEditTextMaxWidth!!.addTextChangedListener(mMaxSizeTextWatcher)
    }

    private val activityResultLauncherGalleryPick = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            val selectedUri = result.data!!.data
            if (selectedUri != null) {
                startCrop(selectedUri)
            } else {
                Toast.makeText(
                    this@SampleActivity,
                    R.string.toast_cannot_retrieve_selected_image,
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@registerForActivityResult
        }

        if (result.resultCode == UCrop.RESULT_ERROR) {
            handleCropError(result.data!!)
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        activityResultLauncherGalleryPick.launch(Intent.createChooser(
            intent,
            getString(R.string.label_select_picture)
        ))
    }

    private val activityResultLauncherUCrop = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleCropResult(result.data!!)
            return@registerForActivityResult
        }

        if (result.resultCode == UCrop.RESULT_ERROR) {
            handleCropError(result.data!!)
        }
    }

    private fun startCrop(uri: Uri) {
        var destinationFileName = SAMPLE_CROPPED_IMAGE_NAME
        when (mRadioGroupCompressionSettings!!.checkedRadioButtonId) {
            R.id.radio_png -> destinationFileName += ".png"
            R.id.radio_jpeg -> destinationFileName += ".jpg"
        }
        var uCrop = of(uri, Uri.fromFile(File(cacheDir, destinationFileName)))
        uCrop = basisConfig(uCrop)
        uCrop = advancedConfig(uCrop)
        if (requestMode == REQUEST_SELECT_PICTURE_FOR_FRAGMENT) {       //if build variant = fragment
            setupFragment(uCrop)
        } else {                                                        // else start uCrop Activity
            uCrop.start(this@SampleActivity, activityResultLauncherUCrop)
        }
    }

    /**
     * In most cases you need only to set crop aspect ration and max size for resulting image.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private fun basisConfig(uCrop: UCrop): UCrop {
        var uCrop = uCrop
        when (mRadioGroupAspectRatio!!.checkedRadioButtonId) {
            R.id.radio_origin -> uCrop = uCrop.useSourceImageAspectRatio()
            R.id.radio_square -> uCrop = uCrop.withAspectRatio(1f, 1f)
            R.id.radio_dynamic -> {}
            else -> try {
                val ratioX =
                    java.lang.Float.valueOf(mEditTextRatioX!!.text.toString().trim { it <= ' ' })
                val ratioY =
                    java.lang.Float.valueOf(mEditTextRatioY!!.text.toString().trim { it <= ' ' })
                if (ratioX > 0 && ratioY > 0) {
                    uCrop = uCrop.withAspectRatio(ratioX, ratioY)
                }
            } catch (e: NumberFormatException) {
                Log.i(TAG, String.format("Number please: %s", e.message))
            }
        }
        if (mCheckBoxMaxSize!!.isChecked) {
            try {
                val maxWidth =
                    Integer.valueOf(mEditTextMaxWidth!!.text.toString().trim { it <= ' ' })
                val maxHeight =
                    Integer.valueOf(mEditTextMaxHeight!!.text.toString().trim { it <= ' ' })
                if (maxWidth > UCrop.MIN_SIZE && maxHeight > UCrop.MIN_SIZE) {
                    uCrop = uCrop.withMaxResultSize(maxWidth, maxHeight)
                }
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Number please", e)
            }
        }
        return uCrop
    }

    /**
     * Sometimes you want to adjust more options, it's done via [com.yalantis.ucrop.UCrop.Options] class.
     *
     * @param uCrop - ucrop builder instance
     * @return - ucrop builder instance
     */
    private fun advancedConfig(uCrop: UCrop): UCrop {
        val options = UCrop.Options()
        when (mRadioGroupCompressionSettings!!.checkedRadioButtonId) {
            R.id.radio_png -> options.setCompressionFormat(Bitmap.CompressFormat.PNG)
            R.id.radio_jpeg -> options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
            else -> options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
        }
        options.setCompressionQuality(mSeekBarQuality!!.progress)
        options.setHideBottomControls(mCheckBoxHideBottomControls!!.isChecked)
        options.setFreeStyleCropEnabled(mCheckBoxFreeStyleCrop!!.isChecked)
        options.setBrightnessEnabled(mCheckBoxBrigtness!!.isChecked)
        options.setContrastEnabled(mCheckBoxContrast!!.isChecked)
        options.setSaturationEnabled(mCheckBoxSaturation!!.isChecked)
        options.setSharpnessEnabled(mCheckBoxSharpness!!.isChecked)
        if (!mEditTextTitlebarTextSize!!.text.toString().isEmpty()) {
            options.setToolbarTitleTextSize(mEditTextTitlebarTextSize!!.text.toString().toFloat())
        }
        when (mRadioGroupTitlebarTextGravity!!.checkedRadioButtonId) {
            R.id.radio_gravity_start -> options.setToolbarTitleTextGravity(Gravity.START)
            R.id.radio_gravity_center -> options.setToolbarTitleTextGravity(Gravity.CENTER)
            R.id.radio_gravity_end -> options.setToolbarTitleTextGravity(Gravity.END)
            else -> {}
        }

        /*
        If you want to configure how gestures work for all UCropActivity tabs

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        * */

        /*
        This sets max size for bitmap that will be decoded from source Uri.
        More size - more memory allocation, default implementation uses screen diagonal.

        options.setMaxBitmapSize(640);
        * */


        /*

        Tune everything (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧

        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setDimmedLayerColor(Color.CYAN);
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCropGridStrokeWidth(20);
        options.setCropGridColor(Color.GREEN);
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(1);
        options.setToolbarCropDrawable(R.drawable.your_crop_icon);
        options.setToolbarCancelDrawable(R.drawable.your_cancel_icon);

        // Color palette
        options.setToolbarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));

        // Aspect ratio options
        options.setAspectRatioOptions(2,
            AspectRatio("WOW", 1F, 2F),
            AspectRatio("MUCH", 3F, 4F),
            AspectRatio("RATIO", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),
            AspectRatio("SO", 16F, 9F),
            AspectRatio("ASPECT", 1F, 1F))
        //options.withAspectRatio(CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO)
        //options.useSourceImageAspectRatio();

       */return uCrop.withOptions(options)
    }

    private fun handleCropResult(result: Intent) {
        val resultUri = getOutput(result)
        if (resultUri != null) {
            startWithUri(this@SampleActivity, resultUri)
        } else {
            Toast.makeText(
                this@SampleActivity,
                R.string.toast_cannot_retrieve_cropped_image,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleCropError(result: Intent) {
        val cropError = getError(result)
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError)
            Toast.makeText(this@SampleActivity, cropError.message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@SampleActivity, R.string.toast_unexpected_error, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun loadingProgress(showLoader: Boolean) {
        mShowLoader = showLoader
        supportInvalidateOptionsMenu()
    }

    override fun onCropFinish(result: UCropResult?) {
        when (Objects.requireNonNull(result)!!.mResultCode) {
            RESULT_OK -> handleCropResult(
                result!!.mResultData
            )
            UCrop.RESULT_ERROR -> handleCropError(result!!.mResultData)
        }
        removeFragmentFromScreen()
    }

    fun removeFragmentFromScreen() {
        supportFragmentManager.beginTransaction()
            .remove(fragment!!)
            .commit()
        toolbar!!.visibility = View.GONE
        settingsView!!.visibility = View.VISIBLE
    }

    fun setupFragment(uCrop: UCrop) {
        fragment = uCrop.getFragment(uCrop.getIntent(this).extras!!)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment!!, UCropFragment.TAG)
            .commitAllowingStateLoss()
        setupViews(uCrop.getIntent(this).extras)
    }

    fun setupViews(args: Bundle?) {
        settingsView!!.visibility = View.GONE
        mStatusBarColor = args!!.getInt(
            UCrop.Options.EXTRA_STATUS_BAR_COLOR,
            ContextCompat.getColor(this, R.color.ucrop_color_statusbar)
        )
        mToolbarColor = args.getInt(
            UCrop.Options.EXTRA_TOOL_BAR_COLOR,
            ContextCompat.getColor(this, R.color.ucrop_color_toolbar)
        )
        mToolbarCancelDrawable =
            args.getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE, R.drawable.ucrop_ic_cross)
        mToolbarCropDrawable =
            args.getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE, R.drawable.ucrop_ic_done)
        mToolbarWidgetColor = args.getInt(
            UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR,
            ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget)
        )
        mToolbarTitle = args.getString(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR)
        mToolbarTitle =
            if (mToolbarTitle != null) mToolbarTitle else resources.getString(R.string.ucrop_label_edit_photo)
        setupAppBar()
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private fun setupAppBar() {
        setStatusBarColor(mStatusBarColor)
        toolbar = findViewById(R.id.toolbar)

        // Set all of the Toolbar coloring
        toolbar!!.setBackgroundColor(mToolbarColor)
        toolbar!!.setTitleTextColor(mToolbarWidgetColor)
        toolbar!!.visibility = View.VISIBLE
        val toolbarTitle = toolbar!!.findViewById<TextView>(R.id.toolbar_title)
        toolbarTitle.setTextColor(mToolbarWidgetColor)
        toolbarTitle.text = mToolbarTitle

        // Color buttons inside the Toolbar
        val stateButtonDrawable = ContextCompat.getDrawable(baseContext, mToolbarCancelDrawable)
        if (stateButtonDrawable != null) {
            stateButtonDrawable.mutate()
            stateButtonDrawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                mToolbarWidgetColor, BlendModeCompat.SRC_ATOP
            )
            toolbar!!.navigationIcon = stateButtonDrawable
        }
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    private fun setStatusBarColor(@ColorInt color: Int) {
        val window = window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ucrop_menu_activity, menu)

        // Change crop & loader menu icons color to match the rest of the UI colors
        val menuItemLoader = menu.findItem(R.id.menu_loader)
        val menuItemLoaderIcon = menuItemLoader.icon
        if (menuItemLoaderIcon != null) {
            try {
                menuItemLoaderIcon.mutate()
                menuItemLoaderIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    mToolbarWidgetColor, BlendModeCompat.SRC_ATOP
                )
                menuItemLoader.icon = menuItemLoaderIcon
            } catch (e: IllegalStateException) {
                Log.i(
                    this.javaClass.name,
                    String.format(
                        "%s - %s",
                        e.message,
                        getString(R.string.ucrop_mutate_exception_hint)
                    )
                )
            }
            (menuItemLoader.icon as Animatable?)!!.start()
        }
        val menuItemCrop = menu.findItem(R.id.menu_crop)
        val menuItemCropIcon = ContextCompat.getDrawable(
            this,
            if (mToolbarCropDrawable == 0) R.drawable.ucrop_ic_done else mToolbarCropDrawable
        )
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate()
            menuItemCropIcon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                mToolbarWidgetColor, BlendModeCompat.SRC_ATOP
            )
            menuItemCrop.icon = menuItemCropIcon
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_crop).isVisible = !mShowLoader
        menu.findItem(R.id.menu_loader).isVisible = mShowLoader
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_crop) {
            if (fragment != null && fragment!!.isAdded) fragment!!.cropAndSaveImage()
        } else if (item.itemId == android.R.id.home) {
            removeFragmentFromScreen()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "SampleActivity"
        private const val REQUEST_SELECT_PICTURE_FOR_FRAGMENT = 0x02
        private const val SAMPLE_CROPPED_IMAGE_NAME = "SampleCropImage"
    }
}