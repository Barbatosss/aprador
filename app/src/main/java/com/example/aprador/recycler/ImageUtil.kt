package com.example.aprador.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import java.io.File
import java.io.InputStream
import androidx.core.graphics.scale
import androidx.core.net.toUri

object ImageUtil {

    // Constants for image processing
    private const val MAX_IMAGE_DIMENSION = 1024
    private const val RECYCLER_IMAGE_SIZE = 300
    private const val DETAIL_IMAGE_WIDTH = 400
    private const val DETAIL_IMAGE_HEIGHT = 300
    private const val JPEG_QUALITY = 85

    /**
     * Load image from URI with proper error handling and memory management (CENTER CROP)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun loadImageFromUri(
        context: Context,
        uri: Uri,
        targetWidth: Int = DETAIL_IMAGE_WIDTH,
        targetHeight: Int = DETAIL_IMAGE_HEIGHT
    ): Bitmap? {
        return try {
            val bitmap = decodeBitmapFromUri(context, uri, MAX_IMAGE_DIMENSION)
            bitmap?.let {
                val correctedBitmap = correctOrientation(context, it, uri)
                resizeAndCropBitmap(correctedBitmap, targetWidth, targetHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load image from URI preserving aspect ratio (FIT CENTER)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun loadImageFromUriPreserveAspect(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        return try {
            val bitmap = decodeBitmapFromUri(context, uri, MAX_IMAGE_DIMENSION)
            bitmap?.let {
                val correctedBitmap = correctOrientation(context, it, uri)
                resizeBitmapPreserveAspect(correctedBitmap, maxWidth, maxHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load image from file path with proper error handling and memory management (CENTER CROP)
     */
    fun loadImageFromPath(
        path: String,
        targetWidth: Int = DETAIL_IMAGE_WIDTH,
        targetHeight: Int = DETAIL_IMAGE_HEIGHT
    ): Bitmap? {
        return try {
            if (!File(path).exists()) return null

            val bitmap = decodeBitmapFromPath(path, MAX_IMAGE_DIMENSION)
            bitmap?.let {
                val correctedBitmap = correctOrientationFromPath(it, path)
                resizeAndCropBitmap(correctedBitmap, targetWidth, targetHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load image from file path preserving aspect ratio (FIT CENTER)
     */
    fun loadImageFromPathPreserveAspect(
        path: String,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        return try {
            if (!File(path).exists()) return null

            val bitmap = decodeBitmapFromPath(path, MAX_IMAGE_DIMENSION)
            bitmap?.let {
                val correctedBitmap = correctOrientationFromPath(it, path)
                resizeBitmapPreserveAspect(correctedBitmap, maxWidth, maxHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load image for recycler view with consistent sizing
     */
    fun loadImageForRecyclerView(context: Context, imagePath: String): Bitmap? {
        return try {
            // Check if it's a URI or file path
            when {
                imagePath.startsWith("content://") -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        loadImageFromUri(context,
                            imagePath.toUri(), RECYCLER_IMAGE_SIZE, RECYCLER_IMAGE_SIZE)
                    } else {
                        loadImageFromPath(imagePath, RECYCLER_IMAGE_SIZE, RECYCLER_IMAGE_SIZE)
                    }
                }
                else -> loadImageFromPath(imagePath, RECYCLER_IMAGE_SIZE, RECYCLER_IMAGE_SIZE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Set image to ImageView with proper scaling
     */
    fun setImageToView(
        imageView: ImageView,
        bitmap: Bitmap?,
        scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            imageView.setImageBitmap(bitmap)
            imageView.scaleType = scaleType
            imageView.background = null
        } else {
            // Set placeholder or default image
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    private fun decodeBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // First pass: get image dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate sample size
                val sampleSize = calculateInSampleSize(options, maxDimension, maxDimension)

                // Second pass: decode with sample size
                context.contentResolver.openInputStream(uri)?.use { actualStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inJustDecodeBounds = false
                        // Use ARGB_8888 instead of RGB_565 for better quality
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inDither = false
                        // Removed deprecated inPurgeable and inInputShareable
                    }
                    BitmapFactory.decodeStream(actualStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBitmapFromPath(path: String, maxDimension: Int): Bitmap? {
        return try {
            // First pass: get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            // Calculate sample size
            val sampleSize = calculateInSampleSize(options, maxDimension, maxDimension)

            // Second pass: decode with sample size
            options.apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
                // Use ARGB_8888 instead of RGB_565 for better quality
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false
                // Removed deprecated inPurgeable and inInputShareable
            }

            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun correctOrientation(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun correctOrientationFromPath(bitmap: Bitmap, path: String): Bitmap {
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            // Clean up original bitmap if different
            if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Resize and crop bitmap to exact dimensions (CENTER CROP behavior)
     */
    private fun resizeAndCropBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return try {
            if (bitmap.isRecycled) return bitmap

            // Calculate scale to fit the image properly
            val scaleX = targetWidth.toFloat() / bitmap.width
            val scaleY = targetHeight.toFloat() / bitmap.height
            val scale = maxOf(scaleX, scaleY)

            // Calculate new dimensions
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            // Scale the bitmap
            val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

            // Calculate crop position (center crop)
            val cropX = maxOf(0, (scaledWidth - targetWidth) / 2)
            val cropY = maxOf(0, (scaledHeight - targetHeight) / 2)

            // Create final cropped bitmap
            val finalBitmap = Bitmap.createBitmap(
                scaledBitmap,
                cropX,
                cropY,
                minOf(targetWidth, scaledWidth),
                minOf(targetHeight, scaledHeight)
            )

            // Clean up intermediate bitmaps
            if (scaledBitmap != bitmap && scaledBitmap != finalBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            if (bitmap != finalBitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }

            finalBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Resize bitmap preserving aspect ratio (FIT CENTER behavior)
     */
    private fun resizeBitmapPreserveAspect(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return try {
            if (bitmap.isRecycled) return bitmap

            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // Calculate scale to fit within max dimensions while preserving aspect ratio
            val scaleX = maxWidth.toFloat() / originalWidth
            val scaleY = maxHeight.toFloat() / originalHeight
            val scale = minOf(scaleX, scaleY)

            // Only scale down, never scale up beyond original size
            val finalScale = minOf(scale, 1.0f)

            // Calculate new dimensions
            val newWidth = (originalWidth * finalScale).toInt()
            val newHeight = (originalHeight * finalScale).toInt()

            // Return original bitmap if no scaling needed
            if (newWidth == originalWidth && newHeight == originalHeight) {
                return bitmap
            }

            // Scale the bitmap
            val scaledBitmap = bitmap.scale(newWidth, newHeight)

            // Clean up original bitmap if different
            if (scaledBitmap != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }

            scaledBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Recycle bitmap safely
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        try {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}