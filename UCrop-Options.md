# UCrop Options:

    var uCrop = of(uri, Uri.fromFile(File(cacheDir, destinationFileName)))
    val options = UCrop.Options()
    
    ... set all options here ...
    
    uCrop = uCrop.withOptions(options)
    uCrop.start(this)

### Set image format:

    options.setCompressionFormat(Bitmap.CompressFormat.JPEG) 

**or**

    options.setCompressionFormat(Bitmap.CompressFormat.PNG) 

### Set compression quality:

    options.setCompressionQuality(Chosse a number between 0 and 100 (in %)) 

### Freestyle-Crop (if you want no aspect ratio):

    options.setFreeStyleCropEnabled(true/false)  

### Set titlebar textsize:
    options.setToolbarTitleTextSize(Float)

### Set titlebar text gravity:

    options.setToolbarTitleTextGravity(Gravity.START/CENTER/END)  

### Hide bottom controls:

    options.setHideBottomControls(true/false)  

## Feature options

####  Brightness-Feature:

    options.setBrightnessEnabled(true/false)  

#### Contrast-Feature:

    options.setContrastEnabled(true/false)  

#### Saturation-Feature:

    options.setSaturationEnabled(true/false)  

#### Sharpness-Feature:

    options.setSharpnessEnabled(true/false)  


## Other options:

    options.setMaxBitmapSize(640)  
    options.setMaxScaleMultiplier(5);  
    options.setImageToCropBoundsAnimDuration(666);  
    options.setDimmedLayerColor(Color.CYAN);  
    options.setCircleDimmedLayer(true);  
    options.setShowCropFrame(false);  
    options.setCropGridStrokeWidth(20);  
    options.setCropGridColor(Color.GREEN);  
    options.setCropGridColumnCount(2);  
    options.setCropGridRowCount(1);  
    options.setToolbarCropDrawable(Drawable);  
    options.setToolbarCancelDrawable(Drawable);  


## Color palette

    options.setToolbarColor(Int);  
    options.setStatusBarColor(Int);  
    options.setToolbarWidgetColor(Int);  
    options.setRootViewBackgroundColor(Int);  
    options.setActiveControlsWidgetColor(Int);  
    options.setWrapperControlsColor(Int);  


## Aspect ratio options

	uCrop.useSourceImageAspectRatio() // aspect from image
	
	uCrop.withAspectRatio(Float, Float) // set 9, 16 for 9:16 etc.

	uCrop.withMaxResultSize(maxWidth (Int), maxHeight (Int)) // image can not be bigger than these values
	
    options.setAspectRatioOptions(2,  // 2 is the index of the default selected ratio
	  AspectRatio("WOW", 1F, 2F),  
	  AspectRatio("MUCH", 3F, 4F),  
	  AspectRatio("RATIO", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),  
	  AspectRatio("SO", 16F, 9F),  
	  AspectRatio("ASPECT", 1F, 1F)
    )
    
    options.withAspectRatio(CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO) 
    options.useSourceImageAspectRatio()
