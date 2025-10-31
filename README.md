[![](https://jitpack.io/v/jens-muenker/uCrop-n-Edit.svg)](https://jitpack.io/#jens-muenker/uCrop-n-Edit) [![](https://jitpack.io/v/jens-muenker/uCrop-n-Edit/month.svg)](https://jitpack.io/#jens-muenker/uCrop-n-Edit)
#### Native Version

# uCrop'n'Edit - Image Cropping and Editing Library for Android

This repository is a fork of <a href="https://github.com/krokyze/uCrop-n-Edit">uCrop'n'Edit</a>, which is in turn a fork of <a href="https://github.com/Yalantis/uCrop">uCrop</a>. I fixed some bugs, updated the dependencies and gradle, and converted much of the code to Kotlin. Furthermore, I added the option to use an ActivityResultLauncher instead of onActivityResult which is deprecated. In addition, in the changelogs you can see all other features I added.

# Features

<a href="https://github.com/krokyze/uCrop-n-Edit">uCrop'n'Edit</a> extends <a href="https://github.com/Yalantis/uCrop">uCrop</a> by adding the ability to change Brightness, Contrast, Saturation, and Sharpness of images. I updated everything and added a few things. You can see all changes in the changelog.

<img align="right" src="preview.gif" width="150" height="280"/>

* image compression format (e.g. PNG, JPEG), compression
* image compression quality [0 - 100]. PNG which is lossless, will ignore the quality setting.
* change Brightness, Contrast, Saturation, and Sharpness of images
* whether all gestures are enabled simultaneously
* maximum size for Bitmap that is decoded from source Uri and used within crop view. If you want to override the default behaviour.
* toggle whether to show crop frame/guidelines
* setup color/width/count of crop frame/rows/columns
* choose whether you want rectangle or oval(`options.setCircleDimmedLayer(true)`) crop area
* the UI colors (Toolbar, StatusBar, active widget state)
* and more...

# Native vs Non-Native Version

I have split this repository into two separate branches:  
- **Native**: `master`, `develop`
- **Non-Native**: `master-non-native`, `develop-non-native`

### Reason for the Split

The main reason for this split was the need for **WebP** file format support. While it is possible to implement this in the native version using [ImageMagick](https://imagemagick.org) as an additional layer for file handling (as mentioned [here](https://cimg.eu/reference/structcimg__library_1_1CImg.html)), I currently don't have the time to do so.

If someone has the time and interest to implement this, I would greatly appreciate it. Feel free to submit a pull request!

### Key Differences Between the Versions

- **Native Version**:
  - Supports only JPEG, PNG, BMP, and [a few more formats](http://www.cimg.eu/reference/io.html).
  
- **Non-Native Version**:
  - Supports JPEG, PNG, BMP, **WebP**, and possibly other [Android-supported image formats](https://developer.android.com/media/platform/supported-formats#image-formats) (untested).

# Usage

1. Include the library as a local library project in your build.gradle:

    ```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	
	...
	
	dependencies {
	        implementation 'com.github.jens-muenker:uCrop-n-Edit:version'
	}
    ```

2. Add UCropActivity into your AndroidManifest.xml

    ```
    <activity
        android:name="com.yalantis.ucrop.UCropActivity"
        android:screenOrientation="portrait"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar"/>
    ```
    
    
### ActivityResultLauncher


3. Create an ActivityResultLauncher and handle uCrop result.

    ```kotlin
    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri = UCrop.getOutput(result.data!!)!!
        }
    }
    ```

4. The uCrop configuration is created using the builder pattern.

    ```kotlin
    UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(16, 9)
        .withMaxResultSize(maxWidth, maxHeight)
        .start(context, activityResultLauncher)
    ```

[Here](https://github.com/jens-muenker/uCrop-n-Edit/blob/master/UCrop-Options.md) you can find possible options.

# Compatibility

  * Library - Android ICS 5.0+ (API 21) ( java >= 17 ; gradle >= 8.5 )
  * Sample - Android ICS 5.0+ (API 21) ( java >= 17 ; gradle >= 8.5 )
  * CPU - armeabi-v7a x86 x86_64 arm64-v8a

# Feedback and Contributions

If you have any ideas for uCrop'n'Edit, feel free to let me know. I will try my best to keep this up to date. If you find any bugs, please add a new issue.

# Changelog

**4.1.1-non-native**
- added wrapper controls color: https://github.com/jens-muenker/uCrop-n-Edit/issues/35
- fixed https://github.com/jens-muenker/uCrop-n-Edit/issues/37
- updated dependencies and gradle

**4.1.1**
- added wrapper controls color: https://github.com/jens-muenker/uCrop-n-Edit/issues/35
- updated dependencies and gradle

**4.1.0-non-native**
- fixed https://github.com/jens-muenker/uCrop-n-Edit/issues/32
- refactored code
- updated dependencies and gradle

**4.1.0**
- fixed https://github.com/jens-muenker/uCrop-n-Edit/issues/32
- refactored code
- updated dependencies and gradle

**4.0.4-non-native**
- improved support for edge-to-edge displays (thanks to [MankinChung](https://github.com/MankinChung) https://github.com/jens-muenker/uCrop-n-Edit/pull/28)
- updated dependencies and gradle

**4.0.4**
- improved support for edge-to-edge displays (thanks to [MankinChung](https://github.com/MankinChung) https://github.com/jens-muenker/uCrop-n-Edit/pull/28)
- updated dependencies and gradle

**4.0.3-non-native**
- add support for edge-to-edge displays
- fixed #24

**4.0.3**
- add support for edge-to-edge displays
- fixed #24

**4.0.2-non-native**
- fixed #20 and #21
- fixed an Android API error

**4.0.2**
- fixed #20 and #21
- fixed an Android API error

**4.0.1-non-native**
- fixed a file size bug (https://github.com/Yalantis/uCrop/pull/926)

**4.0.0-non-native**
- replaced native libraries with Kotlin implementations
- added support for webp (and possibly other [Android-supported image formats](https://developer.android.com/media/platform/supported-formats#image-formats), but I have not tested it yet)
- migrated AsyncTask to Kotlin Coroutines
- updated deprecated code to modern APIs

**3.0.9**
- updated `compileSdk` and `targetSdk` to Android API level 35 (Android 15)
- recompiled native libraries to support 16kB page sizes for improved compatibility and performance - [Learn more](https://developer.android.com/guide/practices/page-sizes#ndk-build_1)
- modified ProGuard rules to reduce APK sizes (https://github.com/jens-muenker/uCrop-n-Edit/issues/16)

**3.0.8**

- set sourceCompatibility and targetCompatibility to Java 17 as requested by the community (see issue https://github.com/jens-muenker/uCrop-n-Edit/issues/14)
- updated libpng and libjpeg (used by CImg) to their latest versions 

**3.0.7**

- added bundle proguard rules in AAR (https://github.com/Yalantis/uCrop/pull/881)
- updated CImg to 3.4.0
- set minSdkVersion to 21
- removed support for armeabi
- updated dependencies and gradle

**3.0.6**

- removed onActivityResult (https://github.com/jens-muenker/uCrop-n-Edit/issues/6)
- updated dependencies and gradle (https://github.com/jens-muenker/uCrop-n-Edit/issues/3)
- set sourceCompatibility and targetCompatibility to java version 21

**3.0.5**

- added support for destination uri as a content provider uri (https://github.com/Yalantis/uCrop/pull/886)
- updated dependencies and gradle

**3.0.4**

- updated dependencies
- added Vietnamese localization (https://github.com/Yalantis/uCrop/pull/845)
- updated French localization (https://github.com/Yalantis/uCrop/pull/838)
- updated Italian localization (https://github.com/Yalantis/uCrop/pull/878)
- updated German localization

**3.0.3**

- transleted sample to kotlin
- added option to set titlebar text gravity and size (https://github.com/Yalantis/uCrop/pull/856)

**3.0.2**

- downgraded gradle to 7.4 (Because jitpack.io does not support higher versions at the moment)

**3.0.1**

- updated to newest gradle version
- added option to pass an extra-bundle to the intent. This will be passed back after the cropping.

**3.0.0** - Based on <a href="https://github.com/krokyze/uCrop-n-Edit">uCrop'n'Edit</a> 2.2.8 and <a href="https://github.com/Yalantis/uCrop">uCrop</a> 2.2.8:

- updated to newest gradle version
- updated libarys to newest versions
- added an AcivityResultLauncher for UCrop
- fixed a bug mentioned in the <a href="https://github.com/Yalantis/uCrop">uCrop</a> pull requests (https://github.com/Yalantis/uCrop/pull/809)

# Technical Information

This library uses the [CImg](https://cimg.eu) library to modify images. CImg requires the libraries libpng and libjpeg. I will strive to keep them up to date to address security issues.

Current versions in use:
- **CImg**: 3.4.0
- **libpng**: 1.6.43
- **libjpeg**: 9f (see more [here](http://www.ijg.org/files/))

# License

This software is licensed under the Apache License, Version 2.0. See the <a href="https://www.apache.org/licenses/LICENSE-2.0">LICENSE</a> file for details.

    Copyright 2023, Jens Münker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

For <a href="https://github.com/Yalantis/uCrop">uCrop</a>:

    Copyright 2017, Yalantis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
