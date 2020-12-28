#include <jni.h>
#include "com_example_facedetector_FaceDetector.h"
#include "com_example_facedetector_Humandetector.h"
#include <opencv2/opencv.hpp>
#include <android/log.h>


#include <iostream>


using namespace cv;
using namespace std;


extern "C" {
/*
JNIEXPORT void JNICALL
Java_com_example_lasttest_MainActivity_ConvertRGBtoGray(
        JNIEnv * env ,
        jobject instance,
        jlong
        matAddrInput ,
        jlong matAddrResult ) {

Mat &matInput = *(Mat *) matAddrInput;
Mat &matResult = *(Mat *) matAddrResult;
cvtColor(matInput, matResult, COLOR_RGBA2GRAY ) ; //그레이 함수 C
}
}*/
float resize(Mat img_src, Mat &img_resize, int resize_width){
    float scale = resize_width/(float)img_src.cols;
    if(img_src.cols>resize_width){
        int new_height = cvRound(img_src.rows * scale);
        resize(img_src,img_resize, Size(resize_width, new_height));
    }
    else{
        img_resize = img_src;
    }
    return scale;
}


JNIEXPORT jlong JNICALL Java_com_example_facedetector_FaceDetector_loadCascade
        (JNIEnv *env, jobject thiz, jstring cascade_file_name) {

    const char *nativeFileNameString = env -> GetStringUTFChars(cascade_file_name,0);

    String baseDir("/storage/emulated/0/");
    baseDir.append(nativeFileNameString);
    const char *pathDir = baseDir.c_str();


    jlong ret = 0;
    ret = (jlong) new CascadeClassifier(pathDir);
    if(((CascadeClassifier *)ret)-> empty()){
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 실패 %s", nativeFileNameString);
    } else
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);

    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);

    return ret;

}


JNIEXPORT jint JNICALL Java_com_example_facedetector_FaceDetector_detect(JNIEnv *env, jobject thiz, jlong cascade_classifier_face, jlong cascade_classifier_eye,
                                                                         jlong mat_addr_input, jlong mat_addr_result, jlong mat_addr_face) {

    Mat &img_input = *(Mat *) mat_addr_input;
    Mat &img_result = *(Mat *) mat_addr_result;
    Mat &img_face = *(Mat *) mat_addr_face;

    int ret = 0;
    int ret2 = 0;
    img_result = img_input.clone(); //image 복사
    img_face = img_input.clone();




    std::vector<Rect> faces; //region of interest
    Mat img_gray;

    cvtColor(img_input,img_gray, COLOR_BGR2GRAY); //grayscale
    equalizeHist(img_gray,img_gray);

    Mat img_resize;
    float resizeRatio = resize(img_gray, img_resize, 640);

//Detect face
    ((CascadeClassifier *) cascade_classifier_face)-> detectMultiScale(img_resize, faces ,1.1,2, 0 | CASCADE_SCALE_IMAGE, Size(30,30));
    ret = faces.size();

  //__android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",(char *)"face %d found", ret);

    for(int i = 0; i <faces.size();i++){
        double real_facesize_x = faces[i].x / resizeRatio;
        double real_faceszie_y = faces[i].y / resizeRatio;
        double real_facesize_width = faces[i].width / resizeRatio;
        double real_faceszie_height = faces[i].height / resizeRatio;

        Rect face_area(real_facesize_x, real_faceszie_y,real_facesize_width,real_faceszie_height);
        rectangle(img_result,face_area,Scalar(255, 255,0),6,8,0);

        img_face = img_result(face_area);// 얼굴 크롭한 다음-> Matrix로 저장.

        Mat faceROI = img_gray(face_area); //face roi안의 이미지 따기.

        std::vector<Rect> eyes;
        //--In each face, detect eyes
        ((CascadeClassifier*) cascade_classifier_eye)-> detectMultiScale(faceROI, eyes , 1.1, 2, 0| CASCADE_SCALE_IMAGE, Size(30,30) );
        ret2 = eyes.size();

    }
    return ret2;
    }

JNIEXPORT jlong JNICALL Java_com_example_facedetector_Humandetector_loadCascade
        (JNIEnv *env, jobject thiz, jstring cascade_file_name) {

    const char *nativeFileNameString = env -> GetStringUTFChars(cascade_file_name,0);

    String baseDir("/storage/emulated/0/");
    baseDir.append(nativeFileNameString);
    const char *pathDir = baseDir.c_str();


    jlong ret = 0;
    ret = (jlong) new CascadeClassifier(pathDir);
    if(((CascadeClassifier *)ret)-> empty()){
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 실패 %s", nativeFileNameString);

    } else
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);

    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);

    return ret;

}

JNIEXPORT jint JNICALL Java_com_example_facedetector_Humandetector_detect
        (JNIEnv *env, jobject thiz, jlong cascade_classifier_body, jlong cascade_classifier_upperbody,jlong mat_addr_input,jlong mat_addr_result) {

    Mat &img_input = *(Mat *) mat_addr_input;
    Mat &img_result = *(Mat *) mat_addr_result;

    int ret = 0;
    int ret2 = 0;
    img_result = img_input.clone(); //image 복사



    std::vector<Rect> body; //region of interest
    Mat img_gray;

    cvtColor(img_input,img_gray, COLOR_BGR2GRAY); //grayscale
    equalizeHist(img_gray,img_gray);

    Mat img_resize;
    float resizeRatio = resize(img_gray, img_resize, 640);

//Detect face
    ((CascadeClassifier *) cascade_classifier_body)-> detectMultiScale(img_resize, body ,1.2,2, 0 | CASCADE_SCALE_IMAGE, Size(30,30));
    ret = body.size();

    //__android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",(char *)"face %d found", ret);

    for(int i = 0; i <body.size();i++){
        double real_bodysize_x = body[i].x / resizeRatio;
        double real_bodysize_y = body[i].y / resizeRatio;
        double real_bodysize_width = body[i].width / resizeRatio;
        double real_bodysize_height = body[i].height / resizeRatio;

        Rect body_area(real_bodysize_x,real_bodysize_y,real_bodysize_width,real_bodysize_height);
        rectangle(img_result,body_area,Scalar(255, 255,0),6,8,0);

        Mat bodyROI = img_gray(body_area);

        std::vector<Rect> upperbody;

        ((CascadeClassifier*) cascade_classifier_upperbody)-> detectMultiScale(bodyROI, upperbody , 1.1, 2, 0| CASCADE_SCALE_IMAGE, Size(30,30) );
        ret2 = upperbody.size();

    }

    return ret2;

}

}
