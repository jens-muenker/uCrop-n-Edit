package com.yalantis.ucrop.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.ImageState
import com.yalantis.ucrop.util.BitmapLoadUtils
import com.yalantis.ucrop.util.ColorFilterGenerator
import com.yalantis.ucrop.util.FileUtils
import com.yalantis.ucrop.util.ImageHeaderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Crops part of image that fills the crop bounds.
 *
 *
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
class BitmapCropTask(
    context: Context,
    private var mViewBitmap: Bitmap?,
    imageState: ImageState,
    cropParameters: CropParameters,
    private val mCropCallback: BitmapCropCallback?
) {

    private val contextRef = WeakReference(context)
    private val mCropRect = imageState.cropRect
    private val mCurrentImageRect = imageState.currentImageRect

    private var mCurrentScale = imageState.currentScale
    private val mCurrentAngle = imageState.currentAngle
    private val mMaxResultImageSizeX = cropParameters.maxResultImageSizeX
    private val mMaxResultImageSizeY = cropParameters.maxResultImageSizeY

    private val mCompressFormat = cropParameters.compressFormat
    private val mCompressQuality = cropParameters.compressQuality

    private val mImageInputPath = cropParameters.imageInputPath
    private val mImageOutputPath = cropParameters.imageOutputPath
    private val mImageInputUri = cropParameters.contentImageInputUri
    private val mImageOutputUri = cropParameters.contentImageOutputUri
    private val mExifInfo = cropParameters.exifInfo

    private val mBrightness = cropParameters.brightness
    private val mContrast = cropParameters.contrast
    private val mSaturation = cropParameters.saturation
    private val mSharpness = cropParameters.sharpness

    private var mCroppedImageWidth = 0
    private var mCroppedImageHeight = 0
    private var cropOffsetX = 0
    private var cropOffsetY = 0

    suspend fun execute(): Throwable? = withContext(Dispatchers.IO) {
        if (mViewBitmap == null) {
            return@withContext NullPointerException("ViewBitmap is null")
        } else if (mViewBitmap!!.isRecycled) {
            return@withContext NullPointerException("ViewBitmap is recycled")
        } else if (mCurrentImageRect.isEmpty) {
            return@withContext NullPointerException("CurrentImageRect is empty")
        }

        val resizeScale = resize()

        return@withContext try {
            crop(resizeScale)

            if (mBrightness != 0.0f || mContrast != 0.0f || mSaturation != 0.0f || mSharpness != 0.0f) {
                val sourceBitmap = BitmapFactory.decodeFile(mImageOutputPath)
                val alteredBitmap =
                    createBitmap(sourceBitmap.width, sourceBitmap.height, sourceBitmap.config!!)

                val cm = ColorMatrix().apply {
                    ColorFilterGenerator.adjustBrightness(this, mBrightness)
                    ColorFilterGenerator.adjustContrast(this, mContrast)
                    ColorFilterGenerator.adjustSaturation(this, mSaturation)
                }

                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(cm)
                }

                Canvas(alteredBitmap).drawBitmap(sourceBitmap, Matrix(), paint)

                if (mSharpness != 0.0f) {
                    val context = contextRef.get() ?: return@withContext IOException("Context is null")
                    val rs = RenderScript.create(context)

                    val inAllocation = Allocation.createFromBitmap(rs, sourceBitmap)
                    val outAllocation = Allocation.createFromBitmap(rs, alteredBitmap)

                    val sharpnessScript = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs)).apply {
                        setInput(inAllocation)
                        setCoefficients(floatArrayOf(
                            0f, -mSharpness, 0f,
                            -mSharpness, 1f + (4 * mSharpness), -mSharpness,
                            0f, -mSharpness, 0f
                        ))
                    }
                    sharpnessScript.forEach(outAllocation)
                    outAllocation.copyTo(alteredBitmap)

                    inAllocation.destroy()
                    outAllocation.destroy()
                    sharpnessScript.destroy()
                    rs.destroy()
                }

                FileOutputStream(File(mImageOutputPath)).use { fos ->
                    alteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
            }

            mViewBitmap = null
            null
        } catch (throwable: Throwable) {
            throwable
        }
    }.also { result ->
        withContext(Dispatchers.Main) {
            if (result == null) {
                val uri = Uri.fromFile(File(mImageOutputPath))
                mCropCallback?.onBitmapCropped(uri, cropOffsetX, cropOffsetY, mCroppedImageWidth, mCroppedImageHeight)
            } else {
                mCropCallback?.onCropFailure(result)
            }
        }
    }

    private fun resize(): Float {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(mImageInputPath, options)

        val swapSides = mExifInfo.exifDegrees == 90 || mExifInfo.exifDegrees == 270
        val scaleX = (if (swapSides) options.outHeight else options.outWidth) / mViewBitmap!!.width.toFloat()
        val scaleY = (if (swapSides) options.outWidth else options.outHeight) / mViewBitmap!!.height.toFloat()

        var resizeScale = min(scaleX, scaleY)
        mCurrentScale /= resizeScale

        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale

            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                resizeScale = min(mMaxResultImageSizeX / cropWidth, mMaxResultImageSizeY / cropHeight)
                mCurrentScale /= resizeScale
            }
        }

        return resizeScale
    }

    private suspend fun crop(resizeScale: Float): Boolean = withContext(Dispatchers.IO) {
        val context = contextRef.get() ?: return@withContext false

        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale

            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                mViewBitmap = mViewBitmap!!.scale(
                    (mViewBitmap!!.width * resizeScale).toInt(),
                    (mViewBitmap!!.height * resizeScale).toInt(),
                    false
                ).also {
                    if (mViewBitmap !== it) mViewBitmap!!.recycle()
                }

                mCurrentScale /= resizeScale
            }
        }

        if (mCurrentAngle != 0f) {
            val tempMatrix = Matrix().apply {
                setRotate(mCurrentAngle, mViewBitmap!!.width / 2f, mViewBitmap!!.height / 2f)
            }

            mViewBitmap = Bitmap.createBitmap(mViewBitmap!!, 0, 0, mViewBitmap!!.width, mViewBitmap!!.height, tempMatrix, true).also {
                if (mViewBitmap !== it) mViewBitmap!!.recycle()
            }
        }

        // Get actual bitmap dimensions after rotation
        val bitmapWidth = mViewBitmap!!.width
        val bitmapHeight = mViewBitmap!!.height
        
        // Calculate the scale between view coordinates and bitmap coordinates
        // The bitmap might have changed size after rotation, so we need to calculate the scale
        // based on the actual current image rect dimensions
        val viewToBitmapScaleX = bitmapWidth / mCurrentImageRect.width()
        val viewToBitmapScaleY = bitmapHeight / mCurrentImageRect.height()
        
        // Calculate crop coordinates in bitmap space
        // mCropRect and mCurrentImageRect are in view coordinates
        val cropLeftInView = mCropRect.left - mCurrentImageRect.left
        val cropTopInView = mCropRect.top - mCurrentImageRect.top
        
        // Convert from view coordinates to bitmap coordinates
        cropOffsetX = (cropLeftInView * viewToBitmapScaleX).toInt()
        cropOffsetY = (cropTopInView * viewToBitmapScaleY).toInt()
        mCroppedImageWidth = (mCropRect.width() * viewToBitmapScaleX).toInt()
        mCroppedImageHeight = (mCropRect.height() * viewToBitmapScaleY).toInt()

        // Validate and clamp crop values to ensure they are within bitmap bounds
        // Ensure cropOffsetX and cropOffsetY are non-negative
        cropOffsetX = max(0, cropOffsetX)
        cropOffsetY = max(0, cropOffsetY)
        
        // Ensure cropOffsetX + width doesn't exceed bitmap bounds
        // If cropOffsetX is beyond bounds, adjust it and reduce width accordingly
        if (cropOffsetX >= bitmapWidth) {
            cropOffsetX = max(0, bitmapWidth - 1)
            mCroppedImageWidth = 1
        } else if (cropOffsetX + mCroppedImageWidth > bitmapWidth) {
            mCroppedImageWidth = bitmapWidth - cropOffsetX
        }
        
        // Ensure cropOffsetY + height doesn't exceed bitmap bounds
        // If cropOffsetY is beyond bounds, adjust it and reduce height accordingly
        if (cropOffsetY >= bitmapHeight) {
            cropOffsetY = max(0, bitmapHeight - 1)
            mCroppedImageHeight = 1
        } else if (cropOffsetY + mCroppedImageHeight > bitmapHeight) {
            mCroppedImageHeight = bitmapHeight - cropOffsetY
        }
        
        // Ensure minimum dimensions (must be at least 1 pixel)
        mCroppedImageWidth = max(1, mCroppedImageWidth)
        mCroppedImageHeight = max(1, mCroppedImageHeight)

        val shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight)

        if (shouldCrop) {
            saveImage(Bitmap.createBitmap(mViewBitmap!!, cropOffsetX, cropOffsetY, mCroppedImageWidth, mCroppedImageHeight))
            if (mCompressFormat == Bitmap.CompressFormat.JPEG) {
                copyExifForOutputFile(context)
            }
            true
        } else {
            FileUtils.copyFile(context, mImageInputUri, mImageOutputUri)
            false
        }
    }

    private suspend fun copyExifForOutputFile(context: Context) = withContext(Dispatchers.IO) {
        val hasImageInputUriContentSchema = BitmapLoadUtils.hasContentScheme(mImageInputUri)
        val hasImageOutputUriContentSchema = BitmapLoadUtils.hasContentScheme(mImageOutputUri)

        if (hasImageInputUriContentSchema && hasImageOutputUriContentSchema) {
            ImageHeaderParser.copyExif(context, mCroppedImageWidth, mCroppedImageHeight, mImageInputUri, mImageOutputUri)
        } else if (hasImageInputUriContentSchema) {
            ImageHeaderParser.copyExif(context, mCroppedImageWidth, mCroppedImageHeight, mImageInputUri, mImageOutputPath)
        } else if (hasImageOutputUriContentSchema) {
            ExifInterface(mImageInputPath).also {
                ImageHeaderParser.copyExif(context, it, mCroppedImageWidth, mCroppedImageHeight, mImageOutputUri)
            }
        } else {
            ExifInterface(mImageInputPath).also {
                ImageHeaderParser.copyExif(it, mCroppedImageWidth, mCroppedImageHeight, mImageOutputPath)
            }
        }
    }

    private suspend fun saveImage(croppedBitmap: Bitmap) = withContext(Dispatchers.IO) {
        val context = contextRef.get() ?: return@withContext

        var outputStream: OutputStream? = null
        var outStream: ByteArrayOutputStream? = null

        try {
            outputStream = context.contentResolver.openOutputStream(mImageOutputUri, "wt")
            outStream = ByteArrayOutputStream().also {
                croppedBitmap.compress(mCompressFormat, mCompressQuality, it)
                outputStream?.write(it.toByteArray())
            }
            croppedBitmap.recycle()
        } catch (exc: IOException) {
            Log.e(TAG, exc.localizedMessage ?: "")
        } finally {
            BitmapLoadUtils.close(outputStream)
            BitmapLoadUtils.close(outStream)
        }
    }

    private fun shouldCrop(width: Int, height: Int): Boolean {
        var pixelError = 1
        pixelError += Math.round(max(width, height) / 1000f)
        return (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0)
                || abs(mCropRect.left - mCurrentImageRect.left) > pixelError
                || abs(mCropRect.top - mCurrentImageRect.top) > pixelError
                || abs(mCropRect.bottom - mCurrentImageRect.bottom) > pixelError
                || abs(mCropRect.right - mCurrentImageRect.right) > pixelError
                || mCurrentAngle != 0f
    }

    companion object {
        private const val TAG = "BitmapCropTask"
    }
}
