LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include D:/Sdk/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_MODULE := detection_based_tracker
LOCAL_SRC_FILES := DetectionBasedTracker_jni.cpp
LOCAL_LDLIBS += -llog -ldl
include $(BUILD_SHARED_LIBRARY)
