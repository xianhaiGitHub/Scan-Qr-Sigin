package com.tony.admin.web.common.enmu;

/**
 * 二维码内容状态
 * @author Guoqing.Lee
 * @date 2019年6月10日 上午11:33:02
 *
 */
public enum QrCodeEnmu {
	
	scan,		//已被APP扫码状态
	login,		//处于登录状态
	cancel,		//已取消登录状态
	logout;		//尚未被扫码状态

}
