package com.ohmerhe.kolley.image

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import android.widget.ImageView

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.ohmerhe.kolley.request.OkHttpStack
import com.squareup.okhttp.OkHttpClient

/**
 * Created by ohmer on 1/12/16.
 */
object Image {
    private var mImageLoaderConfig = ImageLoaderConfig.Builder().build()
    private val mDefaultDisplayOption = ImageDisplayOption.Builder().build()
    private var mRequestQueue: RequestQueue? = null
    var imageLoader: ImageLoader? = null
        private set
    private var mImageCache: ImageLoader.ImageCache? = null

    fun config(context: Context, config: ImageLoaderConfig.Builder.() -> Unit = {}) {
        val builder = ImageLoaderConfig.Builder()
        builder.config()
        mImageLoaderConfig = builder.build()
        mRequestQueue = getRequestQueue(context.applicationContext)
        mImageCache = LRUCache(mImageLoaderConfig)
        imageLoader = ImageLoader(mRequestQueue, mImageCache)
    }

    private fun getRequestQueue(context: Context): RequestQueue {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context.applicationContext, OkHttpStack(OkHttpClient()))
        }
        return mRequestQueue!!
    }

    fun display(init: ImageDisplayRequest.() -> Unit) {
        val displayImageRequest = ImageDisplayRequest()
        displayImageRequest.init()
        displayImage(displayImageRequest._url, displayImageRequest._imageView, displayImageRequest._DisplayOption)
    }

    private fun displayImage(url: String?, imageView: ImageView?, displayOption: ImageDisplayOption? =
    mDefaultDisplayOption) {
        var displayOption = displayOption
        if (TextUtils.isEmpty(url) || imageView == null) {
            return
        }
        if (displayOption == null) {
            displayOption = mDefaultDisplayOption
        }
        val imageListener = ImageLoader.getImageListener(imageView, displayOption.imageResForEmptyUri, displayOption.imageResOnFail)
        imageLoader!!.get(url, imageListener, displayOption.maxWidth, displayOption.maxHeight, displayOption.scaleType)
    }


    fun load(init: ImageLoadRequest.() -> Unit) {
        val imageLoadRequst = ImageLoadRequest()
        imageLoadRequst.init()
        val imageListener = object : ImageListener {
            override fun onLoadSuccess(bitmap: Bitmap) {
                imageLoadRequst._success(bitmap)
            }

            override fun onLoadFailed(error: VolleyError) {
                imageLoadRequst._fail(error)
            }
        }
        loadImage(imageLoadRequst._url, imageListener, imageLoadRequst._ImageLoadOption)
    }

    /**
     * Issues a bitmap request with the given URL if that image is not available
     * in the cache, and returns a bitmap container that contains all of the data
     * relating to the request (as well as the default image if the requested
     * image is not available).
     * @param requestUrl The url of the remote image
     * *
     * @param imageListener The listener to call when the remote image is loaded
     * *
     * @param option
     * *
     * @return A container object that contains all of the properties of the request, as well as
     * *     the currently available image (default if remote is not loaded).
     */
    private fun loadImage(requestUrl: String?, imageListener: ImageListener,
                          option: ImageLoadRequest.ImageLoadOption): ImageLoader.ImageContainer {
        return imageLoader!!.get(requestUrl, object : ImageLoader.ImageListener {
            override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
                if (response.bitmap != null) {
                    imageListener.onLoadSuccess(response.bitmap)
                }
            }

            override fun onErrorResponse(error: VolleyError) {
                imageListener.onLoadFailed(error)
            }
        }, option.maxWidth, option.maxHeight, option.scaleType)
    }

    private interface ImageListener {
        fun onLoadSuccess(bitmap: Bitmap)
        fun onLoadFailed(error: VolleyError)
    }
}

class ImageDisplayRequest {
    internal var _DisplayOption: ImageDisplayOption? = null
    var _imageView: ImageView? = null
    var _url: String? = null

    fun url(url: String){
        _url = url
    }

    fun view(imageView: ImageView){
        _imageView = imageView
    }

    fun options(option: ImageDisplayOption.Builder.() -> Unit) {
        val builder = ImageDisplayOption.Builder()
        builder.option()
        _DisplayOption = builder.build()
    }
}

class ImageLoadRequest {
    internal var _ImageLoadOption: ImageLoadOption = ImageLoadOption()
    var _url: String? = null
    internal var _fail: (VolleyError) -> Unit = {}
    internal var _success: (Bitmap) -> Unit = { }

    fun options(option: ImageLoadOption.() -> Unit) {
        _ImageLoadOption = ImageLoadOption()
        _ImageLoadOption?.option()
    }

    fun url(url: String){
        _url = url
    }

    fun fail(onError: (VolleyError) -> Unit) {
        _fail = onError
    }

    fun success(onSuccess: (Bitmap) -> Unit) {
        _success = onSuccess
    }

    data class ImageLoadOption(var maxWidth: Int = 0, var maxHeight: Int = 0, var scaleType: ImageView.ScaleType =
    ImageView.ScaleType.CENTER_CROP)
}