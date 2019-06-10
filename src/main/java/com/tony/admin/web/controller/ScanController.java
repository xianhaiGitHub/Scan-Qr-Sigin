package com.tony.admin.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.tony.admin.web.common.constant.Constants;
import com.tony.admin.web.common.enmu.QrCodeEnmu;
import com.tony.admin.web.common.redis.RedisRepository;
import com.tony.admin.web.common.response.ResponseBean;
import com.tony.admin.web.common.response.ResponseConstant;
import com.tony.admin.web.common.security.JwtTokenUtil;
import com.tony.admin.web.common.utils.StringHelper;

/**
 * 扫码控制，APP Server端处理
 * @author Guoqing.Lee
 * @date 2019年6月5日 下午3:18:24
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/scan")
public class ScanController {
	
	@Autowired
    private RedisRepository redisRepository;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	/**
	 * App扫一扫统一入口
	 * 说明：一般APP的扫一扫功能都是规划统一入口，以及针对不同的内容最终提供几种类型的响应，比如：
	 * 	 HTTP：通过webview打开一个http的连接
	 * 	 TEXT：弹出一个提示框内容
	 * 	 INTERNAL：打开一个APP的内部跳转
	 *     
	 * @param jsonObject
	 * @return
	 */
	@PostMapping("/scanService")
	public ResponseBean scanService(@RequestBody JSONObject jsonObject) {
		//APP扫描到的内容信息
		String message = jsonObject.containsKey("message")?jsonObject.getString("message"):"";
		String token = jsonObject.containsKey("token")?jsonObject.getString("token"):"";
		
		if(message.startsWith(Constants.QRCODE_HEADER)){//扫码登录
			//获取uuid，校验redis中是否存在该标识
            String uuid = message.replace(Constants.QRCODE_HEADER, "");
            String key = Constants.QRCODE_LOGIN + uuid;
            if( !redisRepository.exists(key) ) {
            	//如果不存在该KEY，表示二维码已经失效
            	JSONObject result = new JSONObject();
            	result.put("type", "TEXT");
            	result.put("content", "获取信息失败，请重新获取");
            	return new ResponseBean(ResponseConstant.GET_MESSAGE_FAILED, result);
            } else {
            	//更新二维码，并将二维码唯一标识与token绑定，有效时间120S
            	redisRepository.setExpire(key, QrCodeEnmu.scan.toString(), 120);
            	redisRepository.setExpire("qrcode_" + token, uuid, 120);
            	JSONObject result = new JSONObject();
            	result.put("type", "HTTP");
            	result.put("content", "http://localhost:8081/views/confirm.html");	//APP端打开一个页面，确认或者取消登录
            	return new ResponseBean(ResponseConstant.SUCCESS, result);
            }
		} else {
			//其他类型的请求操作
		}
		return new ResponseBean();
	}
	
	/**
	 * 扫码登录：确认/取消登录
	 * @param jsonObject
	 * @return 
	 */
	@PostMapping("/scanLogin")
	public ResponseBean scanLogin(@RequestBody JSONObject jsonObject){
		String token = jsonObject.containsKey("token")?jsonObject.getString("token"):"";
		String type = jsonObject.containsKey("type")?jsonObject.getString("type"):"";

		if( "".equals(token) || "".equals(type) ){
			return new ResponseBean(ResponseConstant.IVALID_ERROR);
		}
		
		//根据token获取用户信息
		final String authToken = StringHelper.substring(token, 7);
        String username = jwtTokenUtil.getUsernameFromToken(authToken);
        //根据token获取绑定的uuid，并校验是否已失效
        if(!redisRepository.exists("qrcode_"+token)){
            return new ResponseBean(ResponseConstant.GET_MESSAGE_FAILED);
        }else{
        	String uuid = redisRepository.get("qrcode_"+token);
        	if(QrCodeEnmu.login.toString().equals(type)){
                //更新二维码状态，并附上用户信息
                redisRepository.setExpire(Constants.QRCODE_LOGIN + uuid, "login_"+username, 120);
                //删除绑定了的token与uuid
                redisRepository.del("qrcode_"+token);
                return ResponseBean.success();
            }else if(QrCodeEnmu.cancel.toString().equals(type)){
            	redisRepository.setExpire(Constants.QRCODE_LOGIN + uuid, "cancel", 120);
                //删除绑定了的token与uuid
                redisRepository.del("qrcode_"+token);
                return new ResponseBean(ResponseConstant.CANCEL_SUCCESS);
            }
        }
        return new ResponseBean();
	}
	
}
