package com.yalantis.ucrop

import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.model.AspectRatio
import com.yalantis.ucrop.util.SelectedStateListDrawable
import com.yalantis.ucrop.view.CropImageView
import com.yalantis.ucrop.view.GestureCropImageView
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.view.TransformImageView.TransformImageListener
import com.yalantis.ucrop.view.UCropView
import com.yalantis.ucrop.view.widget.AspectRatioTextView
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView.ScrollingListener
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
class UCropActivity : AppCompatActivity() {
    @IntDef(NONE, SCALE, ROTATE, ALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GestureTypes

    private var mToolbarTitle: String? = null
    private var mToolbarGravity = DEFAULT_TOOLBAR_GRAVITY

    // Toolbar title text size
    private var mToolbarTextSize = DEFAULT_TOOLBAR_TEXT_SIZE

    // Enables dynamic coloring
    private var mToolbarColor = 0
    private var mStatusBarColor = 0
    private var mActiveControlsWidgetColor = 0
    private var mToolbarWidgetColor = 0

    @ColorInt
    private var mRootViewBackgroundColor = 0

    @ColorInt
    private var mWrapperControlsColor = 0

    @DrawableRes
    private var mToolbarCancelDrawable = 0

    @DrawableRes
    private var mToolbarCropDrawable = 0
    private var mLogoColor = 0
    private var mShowBottomControls = false
    private var mShowLoader = true
    private var mUCropView: UCropView? = null
    private var mToolbarTextView: TextView? = null
    private var mToolbarView: Toolbar? = null
    private var mGestureCropImageView: GestureCropImageView? = null
    private var mOverlayView: OverlayView? = null
    private var mWrapperStateAspectRatio: ViewGroup? = null
    private var mWrapperStateRotate: ViewGroup? = null
    private var mWrapperStateScale: ViewGroup? = null
    private var mWrapperStateBrightness: ViewGroup? = null
    private var mWrapperStateContrast: ViewGroup? = null
    private var mWrapperStateSaturation: ViewGroup? = null
    private var mWrapperStateSharpness: ViewGroup? = null
    private var mLayoutAspectRatio: ViewGroup? = null
    private var mLayoutRotate: ViewGroup? = null
    private var mLayoutScale: ViewGroup? = null
    private var mLayoutBrightnessBar: ViewGroup? = null
    private var mLayoutContrastBar: ViewGroup? = null
    private var mLayoutSaturationBar: ViewGroup? = null
    private var mLayoutSharpnessBar: ViewGroup? = null
    private val mCropAspectRatioViews: MutableList<ViewGroup> = ArrayList()
    private var mTextViewRotateAngle: TextView? = null
    private var mTextViewScalePercent: TextView? = null
    private var mTextViewBrightness: TextView? = null
    private var mTextViewContrast: TextView? = null
    private var mTextViewSaturation: TextView? = null
    private var mTextViewSharpness: TextView? = null
    private var mBlockingView: View? = null
    private var mControlsTransition: Transition? = null
    private var mCompressFormat = DEFAULT_COMPRESS_FORMAT
    private var mCompressQuality = DEFAULT_COMPRESS_QUALITY
    private var mAllowedGestures = intArrayOf(SCALE, ROTATE, ALL)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ucrop_activity_photobox)

        val intent = intent
        setupViews(intent)

        enableEdgeToEdge(
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                ColorUtils.calculateLuminance(mToolbarColor) < 0.5
            },
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setImageData(intent)
        setInitialState()
        addBlockingView()
        setupEdgeToEdge()
    }

    private fun setupEdgeToEdge() {
        val toolbar = mToolbarView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(toolbar.parent as View) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            val tv = TypedValue()
            val actionBarHeight =
                if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                    TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
                } else {
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56f, resources.displayMetrics).roundToInt()
                }

            toolbar.updateLayoutParams {
                height = actionBarHeight + insets.top
            }

            toolbar.setPadding(insets.left, insets.top, insets.right, 0)

            val controls = v.findViewById<View>(R.id.wrapper_states)
            if (controls != null) {
                controls.updateLayoutParams {
                    height = resources.getDimensionPixelSize(R.dimen.ucrop_height_wrapper_states) + insets.bottom
                }
                controls.setPadding(0, 0, 0, insets.bottom)
            } else {
                v.setPadding(0, 0, 0, insets.bottom)
            }

            windowInsets
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
                menuItemLoaderIcon.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        mToolbarWidgetColor,
                        BlendModeCompat.SRC_ATOP,
                    )
                menuItemLoader.icon = menuItemLoaderIcon
            } catch (e: IllegalStateException) {
                Log.i(
                    TAG,
                    String.format(
                        "%s - %s",
                        e.message,
                        getString(R.string.ucrop_mutate_exception_hint),
                    ),
                )
            }
            (menuItemLoader.icon as Animatable?)!!.start()
        }
        val menuItemCrop = menu.findItem(R.id.menu_crop)
        val menuItemCropIcon = ContextCompat.getDrawable(this, mToolbarCropDrawable)
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate()
            menuItemCropIcon.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    mToolbarWidgetColor,
                    BlendModeCompat.SRC_ATOP,
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
            cropAndSaveImage()
            return true
        } else if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        if (mGestureCropImageView != null) {
            mGestureCropImageView!!.cancelAllAnimations()
        }
    }

    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private fun setImageData(intent: Intent) {
        val inputUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI)
            }

        val outputUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI)
            }

        processOptions(intent)
        if (inputUri != null && outputUri != null) {
            try {
                mGestureCropImageView!!.setImageUri(inputUri, outputUri)
            } catch (e: Exception) {
                setResultError(e)
                finish()
            }
        } else {
            setResultError(NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)))
            finish()
        }
    }

    /**
     * This method extracts [#optionsBundle][com.yalantis.ucrop.UCrop.Options] from incoming intent
     * and setups Activity, [OverlayView] and [CropImageView] properly.
     */
    private fun processOptions(intent: Intent) {
        // Bitmap compression options
        val compressionFormatName =
            intent.getStringExtra(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME)
        var compressFormat: CompressFormat? = null
        if (!TextUtils.isEmpty(compressionFormatName)) {
            compressFormat = CompressFormat.valueOf(compressionFormatName!!)
        }
        mCompressFormat = compressFormat ?: DEFAULT_COMPRESS_FORMAT
        mCompressQuality =
            intent.getIntExtra(UCrop.Options.EXTRA_COMPRESSION_QUALITY, DEFAULT_COMPRESS_QUALITY)

        // Gestures options
        val allowedGestures = intent.getIntArrayExtra(UCrop.Options.EXTRA_ALLOWED_GESTURES)
        if (allowedGestures != null && allowedGestures.size == TABS_COUNT) {
            mAllowedGestures = allowedGestures
        }

        // Crop image view options
        mGestureCropImageView!!.maxBitmapSize =
            intent.getIntExtra(
                UCrop.Options.EXTRA_MAX_BITMAP_SIZE,
                CropImageView.DEFAULT_MAX_BITMAP_SIZE,
            )
        mGestureCropImageView!!.setMaxScaleMultiplier(
            intent.getFloatExtra(
                UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER,
                CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER,
            ),
        )
        mGestureCropImageView!!.setImageToWrapCropBoundsAnimDuration(
            intent
                .getIntExtra(
                    UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
                    CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
                ).toLong(),
        )

        // Overlay view options
        mOverlayView!!.isFreestyleCropEnabled =
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_FREE_STYLE_CROP,
                OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE,
            )
        mOverlayView!!.setDimmedColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_DIMMED_LAYER_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_dimmed),
            ),
        )
        mOverlayView!!.setCircleDimmedLayer(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER,
                OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER,
            ),
        )
        mOverlayView!!.setShowCropFrame(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_FRAME,
                OverlayView.DEFAULT_SHOW_CROP_FRAME,
            ),
        )
        mOverlayView!!.setCropFrameColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_crop_frame),
            ),
        )
        mOverlayView!!.setCropFrameStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width),
            ),
        )
        mOverlayView!!.setShowCropGrid(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_GRID,
                OverlayView.DEFAULT_SHOW_CROP_GRID,
            ),
        )
        mOverlayView!!.setCropGridRowCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT,
                OverlayView.DEFAULT_CROP_GRID_ROW_COUNT,
            ),
        )
        mOverlayView!!.setCropGridColumnCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT,
                OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT,
            ),
        )
        mOverlayView!!.setCropGridColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_crop_grid),
            ),
        )
        mOverlayView!!.setCropGridCornerColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_CORNER_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_crop_grid),
            ),
        )
        mOverlayView!!.setCropGridStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width),
            ),
        )

        // Aspect ratio options
        val aspectRatioX = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_X, -1f)
        val aspectRatioY = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_Y, -1f)
        val aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        val aspectRatioList: ArrayList<AspectRatio>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, AspectRatio::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
            }

        if (aspectRatioX >= 0 && aspectRatioY >= 0) {
            if (mWrapperStateAspectRatio != null) {
                mWrapperStateAspectRatio!!.visibility = View.GONE
            }
            val targetAspectRatio = aspectRatioX / aspectRatioY
            mGestureCropImageView!!.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size) {
            val targetAspectRatio =
                aspectRatioList[aspectRationSelectedByDefault].aspectRatioX / aspectRatioList[aspectRationSelectedByDefault].aspectRatioY
            mGestureCropImageView!!.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else {
            mGestureCropImageView!!.targetAspectRatio = CropImageView.SOURCE_IMAGE_ASPECT_RATIO
        }

        // Result bitmap max size options
        val maxSizeX = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_X, 0)
        val maxSizeY = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_Y, 0)
        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView!!.setMaxResultImageSizeX(maxSizeX)
            mGestureCropImageView!!.setMaxResultImageSizeY(maxSizeY)
        }

        if (mWrapperStateBrightness != null) {
            mWrapperStateBrightness!!.visibility =
                if (getIntent().getBooleanExtra(
                        UCrop.Options.EXTRA_BRIGHTNESS,
                        true,
                    )
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        if (mWrapperStateContrast != null) {
            mWrapperStateContrast!!.visibility =
                if (getIntent().getBooleanExtra(
                        UCrop.Options.EXTRA_CONTRAST,
                        true,
                    )
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        if (mWrapperStateSaturation != null) {
            mWrapperStateSaturation!!.visibility =
                if (getIntent().getBooleanExtra(
                        UCrop.Options.EXTRA_SATURATION,
                        true,
                    )
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        if (mWrapperStateSharpness != null) {
            if (getIntent().getBooleanExtra(
                    UCrop.Options.EXTRA_SHARPNESS,
                    true,
                )
            ) {
                mWrapperStateSharpness!!.visibility = View.VISIBLE
            } else {
                mWrapperStateSharpness!!.visibility = View.GONE
            }
        }
    }

    private fun setupViews(intent: Intent) {
        mStatusBarColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_STATUS_BAR_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_statusbar),
            )
        mToolbarColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_TOOL_BAR_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_toolbar),
            )
        mActiveControlsWidgetColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE,
                ContextCompat.getColor(this, R.color.ucrop_color_active_controls_color),
            )
        mToolbarGravity =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_TITLE_GRAVITY_TOOLBAR,
                Gravity.CENTER,
            )
        mToolbarTextSize =
            intent.getFloatExtra(
                UCrop.Options.EXTRA_UCROP_TITLE_SIZE_TOOLBAR,
                20F,
            )
        mToolbarWidgetColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR,
                ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget),
            )
        mToolbarCancelDrawable =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE,
                R.drawable.ucrop_ic_cross,
            )
        mToolbarCropDrawable =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE,
                R.drawable.ucrop_ic_done,
            )
        mToolbarTitle = intent.getStringExtra(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR)
        mToolbarTitle =
            if (mToolbarTitle != null) mToolbarTitle else resources.getString(R.string.ucrop_label_edit_photo)
        mLogoColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_LOGO_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_logo),
            )
        mShowBottomControls =
            !intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)
        mRootViewBackgroundColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_crop_background),
            )
        mWrapperControlsColor =
            intent.getIntExtra(
                UCrop.Options.EXTRA_UCROP_WRAPPER_CONTROLS_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_wrapper_controls),
            )
        setupAppBar()
        initiateRootViews()
        if (mShowBottomControls) {
            val viewGroup = findViewById<ViewGroup>(R.id.ucrop_photobox)
            val wrapper = viewGroup.findViewById<ViewGroup>(R.id.controls_wrapper)
            wrapper.visibility = View.VISIBLE
            LayoutInflater.from(this).inflate(R.layout.ucrop_controls, wrapper, true)
            // Set wrapper controls color
            val wrapperControls = findViewById<FrameLayout>(R.id.wrapper_controls)
            val wrapperControlsBackground = wrapperControls?.getChildAt(0) as? ImageView
            wrapperControlsBackground?.let { imageView ->
                val cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    12f,
                    resources.displayMetrics
                )
                val drawable = GradientDrawable().apply {
                    cornerRadii = floatArrayOf(
                        cornerRadius, cornerRadius, // topLeft X, Y
                        cornerRadius, cornerRadius, // topRight X, Y
                        0f, 0f, // bottomRight X, Y
                        0f, 0f, // bottomLeft X, Y
                    )
                    setColor(mWrapperControlsColor)
                }
                imageView.background = drawable
            }
            mControlsTransition = AutoTransition()
            mControlsTransition!!.setDuration(CONTROLS_ANIMATION_DURATION)
            mWrapperStateAspectRatio = findViewById(R.id.state_aspect_ratio)
            mWrapperStateAspectRatio!!.setOnClickListener(mStateClickListener)
            mWrapperStateRotate = findViewById(R.id.state_rotate)
            mWrapperStateRotate!!.setOnClickListener(mStateClickListener)
            mWrapperStateScale = findViewById(R.id.state_scale)
            mWrapperStateScale!!.setOnClickListener(mStateClickListener)
            mWrapperStateBrightness = findViewById(R.id.state_brightness)
            mWrapperStateBrightness!!.setOnClickListener(mStateClickListener)
            mWrapperStateContrast = findViewById(R.id.state_contrast)
            mWrapperStateContrast!!.setOnClickListener(mStateClickListener)
            mWrapperStateSaturation = findViewById(R.id.state_saturation)
            mWrapperStateSaturation!!.setOnClickListener(mStateClickListener)
            mWrapperStateSharpness = findViewById(R.id.state_sharpness)
            mWrapperStateSharpness!!.setOnClickListener(mStateClickListener)
            mLayoutAspectRatio = findViewById(R.id.layout_aspect_ratio)
            mLayoutRotate = findViewById(R.id.layout_rotate_wheel)
            mLayoutScale = findViewById(R.id.layout_scale_wheel)
            mLayoutBrightnessBar = findViewById(R.id.layout_brightness_bar)
            mLayoutContrastBar = findViewById(R.id.layout_contrast_bar)
            mLayoutSaturationBar = findViewById(R.id.layout_saturation_bar)
            mLayoutSharpnessBar = findViewById(R.id.layout_sharpness_bar)
            setupAspectRatioWidget(intent)
            setupRotateWidget()
            setupScaleWidget()
            setupStatesWrapper()
            setupBrightnessWidget()
            setupContrastWidget()
            setupSaturationWidget()
            setupSharpnessWidget()
        }
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private fun setupAppBar() {
        setStatusBarColor(mStatusBarColor)
        mToolbarView = findViewById(R.id.toolbar)

        // Set all of the Toolbar coloring
        mToolbarView!!.setBackgroundColor(mToolbarColor)
        mToolbarView!!.setTitleTextColor(mToolbarWidgetColor)

        mToolbarTextView = mToolbarView!!.findViewById(R.id.toolbar_title)
        // Set the title size
        mToolbarTextView!!.textSize = mToolbarTextSize
        mToolbarTextView!!.setTextColor(mToolbarWidgetColor)
        mToolbarTextView!!.text = mToolbarTitle

        // Set the title alignment mode
        val lp =
            Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT,
            )
        lp.gravity = mToolbarGravity
        mToolbarTextView!!.layoutParams = lp

        // Color buttons inside the Toolbar
        val stateButtonDrawable =
            ContextCompat
                .getDrawable(this, mToolbarCancelDrawable)!!
                .mutate()

        stateButtonDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                mToolbarWidgetColor,
                BlendModeCompat.SRC_ATOP,
            )

        mToolbarView!!.navigationIcon = stateButtonDrawable
        setSupportActionBar(mToolbarView)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initiateRootViews() {
        mUCropView = findViewById(R.id.ucrop)
        mGestureCropImageView = mUCropView!!.cropImageView
        mOverlayView = mUCropView!!.overlayView
        mGestureCropImageView!!.setTransformImageListener(mImageListener)
        (findViewById<View>(R.id.image_view_logo) as ImageView).setColorFilter(
            mLogoColor,
            PorterDuff.Mode.SRC_ATOP,
        )
        findViewById<View>(R.id.ucrop_frame).setBackgroundColor(mRootViewBackgroundColor)
        if (!mShowBottomControls) {
            val params =
                findViewById<View>(R.id.ucrop_frame).layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = 0
            findViewById<View>(R.id.ucrop_frame).requestLayout()
        }
    }

    private val mImageListener: TransformImageListener =
        object : TransformImageListener {
            override fun onRotate(currentAngle: Float) {
                setAngleText(currentAngle)
            }

            override fun onScale(currentScale: Float) {
                setScaleText(currentScale)
            }

            override fun onBrightness(currentBrightness: Float) {
                setBrightnessText(currentBrightness)
            }

            override fun onContrast(currentContrast: Float) {
                setContrastText(currentContrast)
            }

            override fun onSaturation(currentSaturation: Float) {
                setSaturationText(currentSaturation)
            }

            override fun onSharpness(currentSharpness: Float) {
                setSharpnessText(currentSharpness)
            }

            override fun onLoadComplete() {
                mUCropView!!
                    .animate()
                    .alpha(1f)
                    .setDuration(300)
                    .interpolator =
                    AccelerateInterpolator()
                mBlockingView!!.isClickable = false
                mShowLoader = false
                supportInvalidateOptionsMenu()
            }

            override fun onLoadFailure(e: Exception) {
                setResultError(e)
                finish()
            }
        }

    /**
     * Use [.mActiveControlsWidgetColor] for color filter
     */
    private fun setupStatesWrapper() {
        val stateScaleImageView = findViewById<ImageView>(R.id.image_view_state_scale)
        val stateRotateImageView = findViewById<ImageView>(R.id.image_view_state_rotate)
        val stateAspectRatioImageView = findViewById<ImageView>(R.id.image_view_state_aspect_ratio)
        val stateBrightnessImageView = findViewById<ImageView>(R.id.image_view_state_brightness)
        val stateContrastImageView = findViewById<ImageView>(R.id.image_view_state_contrast)
        val stateSaturationImageView = findViewById<ImageView>(R.id.image_view_state_saturation)
        val stateSharpnessImageView = findViewById<ImageView>(R.id.image_view_state_sharpness)
        stateScaleImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateScaleImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateRotateImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateRotateImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateAspectRatioImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateAspectRatioImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateBrightnessImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateBrightnessImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateContrastImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateContrastImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateSaturationImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateSaturationImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
        stateSharpnessImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateSharpnessImageView.drawable,
                mActiveControlsWidgetColor,
            ),
        )
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    private fun setStatusBarColor(
        @ColorInt color: Int,
    ) {
        val window = window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color
        }
    }

    private fun setupAspectRatioWidget(intent: Intent) {
        var aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        var aspectRatioList: ArrayList<AspectRatio>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS, AspectRatio::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
            }

        if (aspectRatioList.isNullOrEmpty()) {
            aspectRationSelectedByDefault = 2
            aspectRatioList = ArrayList()
            aspectRatioList.add(AspectRatio(null, 1f, 1f))
            aspectRatioList.add(AspectRatio(null, 3f, 4f))
            aspectRatioList.add(
                AspectRatio(
                    getString(R.string.ucrop_label_original).uppercase(
                        Locale.getDefault(),
                    ),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO,
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO,
                ),
            )
            aspectRatioList.add(AspectRatio(null, 3f, 2f))
            aspectRatioList.add(AspectRatio(null, 16f, 9f))
        }
        val wrapperAspectRatioList = findViewById<LinearLayout>(R.id.layout_aspect_ratio)
        var wrapperAspectRatio: FrameLayout
        var aspectRatioTextView: AspectRatioTextView
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (aspectRatio in aspectRatioList) {
            wrapperAspectRatio =
                layoutInflater.inflate(R.layout.ucrop_aspect_ratio, null) as FrameLayout
            wrapperAspectRatio.layoutParams = lp
            aspectRatioTextView = wrapperAspectRatio.getChildAt(0) as AspectRatioTextView
            aspectRatioTextView.setActiveColor(mActiveControlsWidgetColor)
            aspectRatioTextView.setAspectRatio(aspectRatio)
            wrapperAspectRatioList.addView(wrapperAspectRatio)
            mCropAspectRatioViews.add(wrapperAspectRatio)
        }
        mCropAspectRatioViews[aspectRationSelectedByDefault].isSelected = true
        for (cropAspectRatioView in mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener { v ->
                mGestureCropImageView!!.targetAspectRatio =
                    ((v as ViewGroup).getChildAt(0) as AspectRatioTextView).getAspectRatio(v.isSelected())
                mGestureCropImageView!!.setImageToWrapCropBounds()
                if (!v.isSelected()) {
                    for (cropAspectRatioView in mCropAspectRatioViews) {
                        cropAspectRatioView.isSelected = cropAspectRatioView === v
                    }
                }
            }
        }
    }

    private fun setupRotateWidget() {
        mTextViewRotateAngle = findViewById(R.id.text_view_rotate)
        (findViewById<View>(R.id.rotate_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        mGestureCropImageView!!.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.rotate_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
        findViewById<View>(R.id.wrapper_reset_rotate).setOnClickListener { resetRotation() }
        findViewById<View>(R.id.wrapper_rotate_by_angle).setOnClickListener { rotateByAngle(90) }
        setAngleTextColor(mActiveControlsWidgetColor)
    }

    private fun setupScaleWidget() {
        mTextViewScalePercent = findViewById(R.id.text_view_scale)
        (findViewById<View>(R.id.scale_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        if (delta > 0) {
                            mGestureCropImageView!!.zoomInImage(
                                mGestureCropImageView!!.currentScale +
                                    delta *
                                    (
                                        (mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) /
                                            SCALE_WIDGET_SENSITIVITY_COEFFICIENT
                                    ),
                            )
                        } else {
                            mGestureCropImageView!!.zoomOutImage(
                                mGestureCropImageView!!.currentScale +
                                    delta *
                                    (
                                        (mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) /
                                            SCALE_WIDGET_SENSITIVITY_COEFFICIENT
                                    ),
                            )
                        }
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.scale_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
        setScaleTextColor(mActiveControlsWidgetColor)
    }

    private fun setupBrightnessWidget() {
        mTextViewBrightness = findViewById(R.id.text_view_brightness)
        (findViewById<View>(R.id.brightness_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        mGestureCropImageView!!.postBrightness(delta / BRIGHTNESS_WIDGET_SENSITIVITY_COEFFICIENT)
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.brightness_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
    }

    private fun setupContrastWidget() {
        mTextViewContrast = findViewById(R.id.text_view_contrast)
        (findViewById<View>(R.id.contrast_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        mGestureCropImageView!!.postContrast(delta / CONTRAST_WIDGET_SENSITIVITY_COEFFICIENT)
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.contrast_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
    }

    private fun setupSaturationWidget() {
        mTextViewSaturation = findViewById(R.id.text_view_saturation)
        (findViewById<View>(R.id.saturation_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        mGestureCropImageView!!.postSaturation(delta / SATURATION_WIDGET_SENSITIVITY_COEFFICIENT)
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.saturation_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
    }

    private fun setupSharpnessWidget() {
        mTextViewSharpness = findViewById(R.id.text_view_sharpness)
        (findViewById<View>(R.id.sharpness_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(
                object : ScrollingListener {
                    override fun onScroll(
                        delta: Float,
                        totalDistance: Float,
                    ) {
                        mGestureCropImageView!!.postSharpness(delta / SHARPNESS_WIDGET_SENSITIVITY_COEFFICIENT)
                    }

                    override fun onScrollEnd() {
                        mGestureCropImageView!!.setImageToWrapCropBounds()
                    }

                    override fun onScrollStart() {
                        mGestureCropImageView!!.cancelAllAnimations()
                    }
                },
            )
        (findViewById<View>(R.id.sharpness_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor,
        )
    }

    private fun setAngleText(angle: Float) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle!!.text = String.format(Locale.getDefault(), "%.1f°", angle)
        }
    }

    private fun setAngleTextColor(textColor: Int) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle!!.setTextColor(textColor)
        }
    }

    private fun setScaleText(scale: Float) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent!!.text =
                String.format(Locale.getDefault(), "%d%%", (scale * 100).toInt())
        }
    }

    private fun setBrightnessText(brightness: Float) {
        if (mTextViewBrightness != null) {
            mTextViewBrightness!!.text =
                String.format(Locale.getDefault(), "%d", brightness.toInt())
        }
    }

    private fun setContrastText(contrast: Float) {
        if (mTextViewContrast != null) {
            mTextViewContrast!!.text =
                String.format(Locale.getDefault(), "%d", contrast.toInt())
        }
    }

    private fun setSaturationText(saturation: Float) {
        if (mTextViewSaturation != null) {
            mTextViewSaturation!!.text =
                String.format(Locale.getDefault(), "%d", saturation.toInt())
        }
    }

    private fun setSharpnessText(sharpness: Float) {
        if (mTextViewSharpness != null) {
            mTextViewSharpness!!.text = String.format(Locale.getDefault(), "%d", sharpness.toInt())
        }
    }

    private fun setScaleTextColor(textColor: Int) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent!!.setTextColor(textColor)
        }
    }

    private fun resetRotation() {
        mGestureCropImageView!!.postRotate(-mGestureCropImageView!!.currentAngle)
        mGestureCropImageView!!.setImageToWrapCropBounds()
    }

    private fun rotateByAngle(angle: Int) {
        mGestureCropImageView!!.postRotate(angle.toFloat())
        mGestureCropImageView!!.setImageToWrapCropBounds()
    }

    private val mStateClickListener =
        View.OnClickListener { v ->
            if (!v.isSelected) {
                setWidgetState(v.id)
            }
        }

    private fun setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio!!.visibility == View.VISIBLE) {
                setWidgetState(R.id.state_aspect_ratio)
            } else {
                setWidgetState(R.id.state_scale)
            }
        } else {
            setAllowedGestures(0)
        }
    }

    private fun setWidgetState(
        @IdRes stateViewId: Int,
    ) {
        if (!mShowBottomControls) return
        mWrapperStateAspectRatio!!.isSelected = stateViewId == R.id.state_aspect_ratio
        mWrapperStateRotate!!.isSelected = stateViewId == R.id.state_rotate
        mWrapperStateScale!!.isSelected = stateViewId == R.id.state_scale
        mWrapperStateBrightness!!.isSelected = stateViewId == R.id.state_brightness
        mWrapperStateContrast!!.isSelected = stateViewId == R.id.state_contrast
        mWrapperStateSaturation!!.isSelected = stateViewId == R.id.state_saturation
        mWrapperStateSharpness!!.isSelected = stateViewId == R.id.state_sharpness
        mLayoutAspectRatio!!.visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mLayoutRotate!!.visibility =
            if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
        mLayoutScale!!.visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
        mLayoutBrightnessBar!!.visibility =
            if (stateViewId == R.id.state_brightness) View.VISIBLE else View.GONE
        mLayoutContrastBar!!.visibility =
            if (stateViewId == R.id.state_contrast) View.VISIBLE else View.GONE
        mLayoutSaturationBar!!.visibility =
            if (stateViewId == R.id.state_saturation) View.VISIBLE else View.GONE
        mLayoutSharpnessBar!!.visibility =
            if (stateViewId == R.id.state_sharpness) View.VISIBLE else View.GONE
        changeSelectedTab(stateViewId)
        if (stateViewId == R.id.state_brightness ||
            stateViewId == R.id.state_contrast ||
            stateViewId == R.id.state_saturation ||
            stateViewId == R.id.state_sharpness ||
            stateViewId == R.id.state_scale
        ) {
            setAllowedGestures(0)
        } else if (stateViewId == R.id.state_rotate) {
            setAllowedGestures(1)
        } else {
            setAllowedGestures(2)
        }
    }

    private fun changeSelectedTab(stateViewId: Int) {
        TransitionManager.beginDelayedTransition(
            (findViewById<View>(R.id.ucrop_photobox) as ViewGroup),
            mControlsTransition,
        )
        mWrapperStateScale!!.findViewById<View>(R.id.text_view_scale).visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
        mWrapperStateAspectRatio!!.findViewById<View>(R.id.text_view_crop).visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mWrapperStateRotate!!.findViewById<View>(R.id.text_view_rotate).visibility =
            if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
        mWrapperStateBrightness!!.findViewById<View>(R.id.text_view_brightness).visibility =
            if (stateViewId == R.id.state_brightness) View.VISIBLE else View.GONE
        mWrapperStateContrast!!.findViewById<View>(R.id.text_view_contrast).visibility =
            if (stateViewId == R.id.state_contrast) View.VISIBLE else View.GONE
        mWrapperStateSaturation!!.findViewById<View>(R.id.text_view_saturation).visibility =
            if (stateViewId == R.id.state_saturation) View.VISIBLE else View.GONE
        mWrapperStateSharpness!!.findViewById<View>(R.id.text_view_sharpness).visibility =
            if (stateViewId == R.id.state_sharpness) View.VISIBLE else View.GONE
    }

    private fun setAllowedGestures(tab: Int) {
        mGestureCropImageView!!.isScaleEnabled =
            mAllowedGestures[tab] == ALL ||
            mAllowedGestures[tab] == SCALE
        mGestureCropImageView!!.isRotateEnabled =
            mAllowedGestures[tab] == ALL ||
            mAllowedGestures[tab] == ROTATE
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private fun addBlockingView() {
        if (mBlockingView == null) {
            mBlockingView = View(this)
            val lp =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar)
            mBlockingView!!.layoutParams = lp
            mBlockingView!!.isClickable = true
        }
        (findViewById<View>(R.id.ucrop_photobox) as RelativeLayout).addView(mBlockingView)
    }

    private fun cropAndSaveImage() {
        mBlockingView!!.isClickable = true
        mShowLoader = true
        supportInvalidateOptionsMenu()
        lifecycleScope.launch {
            mGestureCropImageView!!.cropAndSaveImage(
                mCompressFormat,
                mCompressQuality,
                object : BitmapCropCallback {
                    override fun onBitmapCropped(
                        resultUri: Uri,
                        offsetX: Int,
                        offsetY: Int,
                        imageWidth: Int,
                        imageHeight: Int,
                    ) {
                        setResultUri(
                            resultUri,
                            mGestureCropImageView!!.targetAspectRatio,
                            offsetX,
                            offsetY,
                            imageWidth,
                            imageHeight,
                        )
                        finish()
                    }

                    override fun onCropFailure(t: Throwable) {
                        setResultError(t)
                        finish()
                    }
                },
            )
        }
    }

    protected fun setResultUri(
        uri: Uri?,
        resultAspectRatio: Float,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        val bundle = intent.extras?.getBundle("EXTRA-BUNDLE")

        if (bundle != null) {
            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                    .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                    .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                    .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                    .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                    .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                    .putExtra("EXTRA-BUNDLE", bundle),
            )
        } else {
            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                    .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                    .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                    .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                    .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                    .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY),
            )
        }
    }

    protected fun setResultError(throwable: Throwable?) {
        setResult(UCrop.RESULT_ERROR, Intent().putExtra(UCrop.EXTRA_ERROR, throwable))
    }

    companion object {
        const val DEFAULT_COMPRESS_QUALITY = 90
        val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        const val NONE = 0
        const val SCALE = 1
        const val ROTATE = 2
        const val ALL = 3
        private const val TAG = "UCropActivity"
        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42
        private const val DEFAULT_TOOLBAR_TEXT_SIZE = 14f
        private const val DEFAULT_TOOLBAR_GRAVITY = Gravity.START
        private const val BRIGHTNESS_WIDGET_SENSITIVITY_COEFFICIENT = 3
        private const val CONTRAST_WIDGET_SENSITIVITY_COEFFICIENT = 4
        private const val SATURATION_WIDGET_SENSITIVITY_COEFFICIENT = 3
        private const val SHARPNESS_WIDGET_SENSITIVITY_COEFFICIENT = 400

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
