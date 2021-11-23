package com.ke.adas;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("all")
public class DeviceErrorCode {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, value = {
            SUCCESS,
            OBJECTISNULL,
            HASEXCEPTION,
            BLUETOOTH_NOT_ENABLED,
            BLE_NOT_SUPPORTED,
            BLE_NO_BT_ADAPTER,
            BLE_NULL,
            LOGIN_DEVICE_NULL,
            REQUEST_CONNECT_FAIL,
            LOGIN_FAIL,
            CONNECT_BLE_FAIL,
            DEVICE_NOT_FIND_SERVICE,
            BLE_LOGIN_PASSWORD_INCORRECT,
            BLE_DEVICE_UPDATEING,
            BLE_DEVICE_UNWORK,
            BLE_SUBSCRIBE_FAIL,
            TCP_SOKET_CONNECT_TIMEOUT,
            TCP_SOKET_DOWNLOAD_HASNEGATIVE,
            TCP_SOKET_DOWNLOAD_RECEIVE_FAIL,
            TCP_SOKET_DOWNLOAD_FILE_EXISTED,
            TCP_SOKET_DOWNLOAD_GET_STREAM_FAIL,
            TCP_SOKET_DOWNLOAD_CONNECT_TIME,
            VALUE_CHUANGE_FAIL,
            TCP_SOKET_UPDATE_CONNECT_TIMEOUT,
            TCP_SOKET_UPDATE_CONNECT_FAIL,
            TCP_SOKET_UPDATE_RECEIVE_FAIL,
            UPDATE_NOT_FIND_FILE,
            UPDATE_NOT_SEND_FAIL,
            UPDATE_TIME_OUT,
            ADJUSTMENTSDATA_LENGTH_ERROR,
            ADJUSTMENTSDATA_VALUE_RANGE_ERROR,
            CLOSE_REAL_TIME_DATA_STREAM_FAIL,
            SET_OBD_DATA_FAIL,
            DEVICE_TYPE_ERROR,
            TCP_SOKET_DOWNLOAD_NOT_DOWNLOAD_CLIENT,
            TCP_SOKET_SERVICE_IS_RUNNING,
            TCP_SOKET_SERVICE_IS_NOT_WORKING,
            NEED_PERMISSIOM,
            SDK_INIT_FAIL_WRONGAPPID,
            TCP_SOKET_SERVICE_DEVICE_CAN_NOT_CONNECT_WIFI
    })
    public @interface ErrorCode {
    }

    public static final int SUCCESS = 0;
    public static final int OBJECTISNULL = 1;
    public static final int HASEXCEPTION = 2;
    public static final int BLUETOOTH_NOT_ENABLED = 1010;
    public static final int BLE_NOT_SUPPORTED = 1011;
    public static final int BLE_NO_BT_ADAPTER = 1012;
    public static final int BLE_NULL = 1013;
    public static final int LOGIN_DEVICE_NULL = 1020;
    public static final int REQUEST_CONNECT_FAIL = 1021;
    public static final int LOGIN_FAIL = 1022;
    public static final int CONNECT_BLE_FAIL = 1023;
    public static final int DEVICE_NOT_FIND_SERVICE = 1024;
    public static final int BLE_LOGIN_PASSWORD_INCORRECT = 1025;
    public static final int BLE_DEVICE_UPDATEING = 1026;
    public static final int BLE_DEVICE_UNWORK = 1027;
    public static final int BLE_SUBSCRIBE_FAIL = 1028;
    public static final int TCP_SOKET_CONNECT_TIMEOUT = 1030;
    public static final int TCP_SOKET_DOWNLOAD_HASNEGATIVE = 1050;
    public static final int TCP_SOKET_DOWNLOAD_RECEIVE_FAIL = 1051;
    public static final int TCP_SOKET_DOWNLOAD_FILE_EXISTED = 1052;
    public static final int TCP_SOKET_DOWNLOAD_GET_STREAM_FAIL = 1053;
    public static final int TCP_SOKET_DOWNLOAD_CONNECT_TIME = 1054;
    public static final int VALUE_CHUANGE_FAIL = 1055;
    public static final int TCP_SOKET_UPDATE_CONNECT_TIMEOUT = 1060;
    public static final int TCP_SOKET_UPDATE_CONNECT_FAIL = 1061;
    public static final int TCP_SOKET_UPDATE_RECEIVE_FAIL = 1062;
    public static final int UPDATE_NOT_FIND_FILE = 1070;
    public static final int UPDATE_NOT_SEND_FAIL = 1071;
    public static final int UPDATE_TIME_OUT = 1072;
    public static final int ADJUSTMENTSDATA_LENGTH_ERROR = 1080;
    public static final int ADJUSTMENTSDATA_VALUE_RANGE_ERROR = 1081;
    public static final int CLOSE_REAL_TIME_DATA_STREAM_FAIL = 1090;
    public static final int SET_OBD_DATA_FAIL = 1091;
    public static final int DEVICE_TYPE_ERROR = 1101;
    public static final int TCP_SOKET_DOWNLOAD_NOT_DOWNLOAD_CLIENT = 1056;
    public static final int TCP_SOKET_SERVICE_IS_RUNNING = 1031;
    public static final int TCP_SOKET_SERVICE_IS_NOT_WORKING = 1040;
    public static final int NEED_PERMISSIOM = 1041;
    public static final int SDK_INIT_FAIL_WRONGAPPID = 1042;
    public static final int TCP_SOKET_SERVICE_DEVICE_CAN_NOT_CONNECT_WIFI = 1043;

    private DeviceErrorCode() {

    }

    public static String getErrorMessage(@ErrorCode int errorCode) {
        switch (errorCode) {
            case SUCCESS:
                return "操作成功";
            case OBJECTISNULL:
                return "对象没有初始化";
            case HASEXCEPTION:
                return "有异常";
            case BLUETOOTH_NOT_ENABLED:
                return "蓝牙未打开";
            case BLE_NOT_SUPPORTED:
                return "不支持BLE";
            case BLE_NO_BT_ADAPTER:
                return "无法获取的蓝牙的适配器";
            case BLE_NULL:
                return "BLE为空";
            case LOGIN_DEVICE_NULL:
                return "要登录的设备为空";
            case REQUEST_CONNECT_FAIL:
                return "请求建立BLE连接失败";
            case LOGIN_FAIL:
                return "登录失败";
            case CONNECT_BLE_FAIL:
                return "BLE连接失败";
            case DEVICE_NOT_FIND_SERVICE:
                return "没有找到设备服务器";
            case BLE_LOGIN_PASSWORD_INCORRECT:
                return "密码错误";
            case BLE_DEVICE_UPDATEING:
                return "设备正在升级";
            case BLE_DEVICE_UNWORK:
                return "设备内部智能程序损坏";
            case BLE_SUBSCRIBE_FAIL:
                return "订阅通知失败";
            case TCP_SOKET_CONNECT_TIMEOUT:
                return "socket连接超时";
            case TCP_SOKET_DOWNLOAD_HASNEGATIVE:
                return "下载进度错误";
            case TCP_SOKET_DOWNLOAD_RECEIVE_FAIL:
                return "接收文件失败";
            case TCP_SOKET_DOWNLOAD_FILE_EXISTED:
                return "要下载的文件已存在";
            case TCP_SOKET_DOWNLOAD_GET_STREAM_FAIL:
                return "获取文件流失败";
            case TCP_SOKET_DOWNLOAD_CONNECT_TIME:
                return "下载连接超时";
            case VALUE_CHUANGE_FAIL:
                return "改变数值失败";
            case TCP_SOKET_UPDATE_CONNECT_TIMEOUT:
                return "更新连接超时";
            case TCP_SOKET_UPDATE_CONNECT_FAIL:
                return "更新连接失败";
            case TCP_SOKET_UPDATE_RECEIVE_FAIL:
                return "更新接收失败";
            case UPDATE_NOT_FIND_FILE:
                return "找不到更新文件";
            case UPDATE_NOT_SEND_FAIL:
                return "发送失败";
            case UPDATE_TIME_OUT:
                return "更新超时";
            case ADJUSTMENTSDATA_LENGTH_ERROR:
                return "标定数据长度错误";
            case ADJUSTMENTSDATA_VALUE_RANGE_ERROR:
                return "标定数据范围错误";
            case CLOSE_REAL_TIME_DATA_STREAM_FAIL:
                return "关闭实况数据流失败";
            case SET_OBD_DATA_FAIL:
                return "设置OBD数据失败";
            case DEVICE_TYPE_ERROR:
                return "设备类型错误";
            case TCP_SOKET_DOWNLOAD_NOT_DOWNLOAD_CLIENT:
                return "下载器为空";
            case TCP_SOKET_SERVICE_IS_RUNNING:
                return "socket已经开始运行";
            case TCP_SOKET_SERVICE_IS_NOT_WORKING:
                return "socket已不再工作状态";
            case NEED_PERMISSIOM:
                return "需要权限";
            case SDK_INIT_FAIL_WRONGAPPID:
                return "错误的appid";
            case TCP_SOKET_SERVICE_DEVICE_CAN_NOT_CONNECT_WIFI:
                return "设备连接热点失败";
            default:
                return "";
        }
    }
}
