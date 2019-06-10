package com.tony.admin.web.controller;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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
import com.tony.admin.web.common.security.model.AuthUserFactory;
import com.tony.admin.web.common.utils.VerifyCodeUtils;
import com.tony.admin.web.model.SysUser;
import com.tony.admin.web.service.ISystemService;

import cn.hutool.core.codec.Base64;

/**
 * 用户登录相关控制器，PC Server端
 * @author Guoqing.Lee
 * @date 2019年6月5日 上午11:07:58
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {
	
	private Logger logger = LoggerFactory.getLogger(AuthController.class);
	@Autowired
	private RedisRepository redisRepository;
	@Autowired
	private ISystemService systemService;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	@Autowired
	private AuthenticationManager authenticationManager;
	
	/**
	 * create token
	 * @param jsonObject
	 * @param session
	 * @return
	 */
	@PostMapping("/token")
	public ResponseBean createToken(@RequestBody JSONObject jsonObject){
		String userName = jsonObject.getString("username").trim();
		String password = jsonObject.getString("password").trim();
		String uniqueCode = jsonObject.getString("uniqueCode").trim();
		String verifyCode = jsonObject.getString("verifyCode").trim();
		
		if( !redisRepository.exists(VerifyCodeUtils.VERIFY_CODE + uniqueCode) ){
			return new ResponseBean(ResponseConstant.IVALID_VERIFY_CODE);
		}
		
		//验证验证码是否正确
		String code = redisRepository.get(VerifyCodeUtils.VERIFY_CODE + uniqueCode);
		if( !code.equals(verifyCode) ){
			return new ResponseBean(ResponseConstant.VERIFY_CODE_ERROR);
		}else{
			//验证成功即删除验证码
			redisRepository.del(VerifyCodeUtils.VERIFY_CODE + uniqueCode);
		}
		
		try {
			
			final Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(userName, password));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
			final String token = jwtTokenUtil.generateToken(userDetails, jwtTokenUtil.getRandomKey());
			
			return new ResponseBean(ResponseConstant.SUCCESS, JwtTokenUtil.TOKEN_TYPE_BEARER + " " + token);
		} catch (UsernameNotFoundException e) {
			logger.info("用户认证失败：" + "userName wasn't in the system");
			return new ResponseBean(ResponseConstant.USERNAME_ERROR);
		} catch (LockedException lae) {
			logger.info("用户认证失败：" + "account for that username is locked, can't login");
			return new ResponseBean(ResponseConstant.ACCOUNT_LOCKED);
		} catch (AuthenticationException ace) {
			logger.info("用户认证失败：" + ace);
			ace.printStackTrace();
			return new ResponseBean(ResponseConstant.USER_INFO_ERROR);
		}
	}
	
	/**
	 * <p>Description:登陆首页获取扫一扫登陆的二维码内容</p>
	 * 1、二维码内容的有效时间是5分钟，生成的code是唯一码，存在redis缓存中，value里面带有该code的登陆状态
	 * UserBaseController.java
	 * return:Map<String,Object>
	 */
	@PostMapping(value="/getQrcodeContent")
	public ResponseBean getQrcodeContent(@RequestBody JSONObject jsonObject){
		String oldContext = jsonObject.containsKey("context")?jsonObject.getString("context"):"";
		//如果页面有旧的二维码，同时请求新的二维码内容，则直接删除旧内容
		if( !"".equals(oldContext) ){
			if( redisRepository.exists(Constants.QRCODE_LOGIN + oldContext.replace(Constants.QRCODE_HEADER, ""))){
				redisRepository.del(Constants.QRCODE_LOGIN + oldContext.replace(Constants.QRCODE_HEADER, ""));
			}
		}
		
		String code = Base64.encode(UUID.randomUUID().toString());
		String context = Constants.QRCODE_HEADER + code;
		//将生成的code存入redis，失效时间为120S
		redisRepository.setExpire(Constants.QRCODE_LOGIN + code, QrCodeEnmu.logout.toString(), 120);
		return new ResponseBean(ResponseConstant.SUCCESS, context);
	}
	
	/**
	 * <p>Description:web端与服务器建立连接检查当前用户是否有做登陆动作</p>
	 * UserBaseController.java
	 * return:Map<String,Object>
	 * @throws Exception 
	 */
	@PostMapping(value="/qrcodeCheckLogin")
	public ResponseBean qrcodeCheckLogin(@RequestBody JSONObject jsonObject, HttpServletRequest httpRequest) throws Exception{
		String context = jsonObject.containsKey("context")?jsonObject.getString("context"):"";
		String type = jsonObject.containsKey("type")?jsonObject.getString("type"):"";

		if( "".equals(context) ){
			return new ResponseBean(ResponseConstant.IVALID_QRCODE);
		}
		
		//统一一个开始时间，每次请求超过10s时自动跳出循环结束
		long starttime = System.currentTimeMillis();
		String code = context.replace(Constants.QRCODE_HEADER, "");
		ResponseBean responseBean = new ResponseBean();
		while(true){
			Thread.sleep(500);
			//logger.info("retry check login...");
			//检查redis是否中还存在二维码内容
			if( !redisRepository.exists(Constants.QRCODE_LOGIN + code) ){
				return new ResponseBean(ResponseConstant.IVALID_QRCODE);
			}else{
				String status = redisRepository.get(Constants.QRCODE_LOGIN + code);
				//如果status 的值是 scan，则表示该code已经被手机扫描，返回页面提示在手机上确认登陆
				//如果status 的值是  login，则表示该code处于登录状态，则返回前端状态信息
				//如果status 的值是  cancel，则表示该code为取消登录
				//如果status 的值是 logout，则表示该code尚未被扫描
				if( QrCodeEnmu.scan.toString().equals(status) ){
					//如果传入的type的状态值不为空，则表明已经扫描成功，在等待确认下一步操作
					if( QrCodeEnmu.scan.toString().equals(type) ){
						long endTime = System.currentTimeMillis();
						long exeTime = endTime - starttime;
						//请求大于10s，则跳出循环结束
						if( exeTime >= 10000 ){
							responseBean.setIsSuccess(true);
							responseBean.setResponseCode(1104);
							responseBean.setResponseMsg("请求超时，请重新请求");
							break;
						}
					}else{
						return new ResponseBean(ResponseConstant.SCAN_SUCCESS);
					}
				}else if( QrCodeEnmu.cancel.toString().equals(status) ){
					redisRepository.del(Constants.QRCODE_LOGIN + code); //删除redis中该二维码的缓存信息
					return new ResponseBean(ResponseConstant.CANCEL_SUCCESS);
				}else if( status.startsWith("login_") ){
					redisRepository.del(Constants.QRCODE_LOGIN + code);
					String userCode = status.replace("login_", "");
					
					SysUser sysUser = systemService.getUserByLoginName(userCode);
					
					//完成登录操作
					UserDetails userDetails = AuthUserFactory.create(sysUser);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
		            SecurityContextHolder.getContext().setAuthentication(authentication);
					
					final String jwtToken = jwtTokenUtil.generateToken(userDetails, jwtTokenUtil.getRandomKey());
					return new ResponseBean(ResponseConstant.SUCCESS, JwtTokenUtil.TOKEN_TYPE_BEARER + " " + jwtToken);
				}else{
					long endTime = System.currentTimeMillis();
					long exeTime = endTime - starttime;
					//请求大于10s，则跳出循环结束
					if( exeTime >= 10000 ){
						responseBean.setIsSuccess(true);
						responseBean.setResponseCode(1104);
						responseBean.setResponseMsg("请求超时，请重新请求");
						break;
					}
				}
			}
		}
		return responseBean;
	}

	/**
	 * 生成验证码图片
	 * @param jsonObject
	 * @return
	 */
	@PostMapping(value="/getVerifyCode")
	public ResponseBean getVerifyCode(@RequestBody JSONObject jsonObject) {
		//前端生成的唯一的key，用户登录时验证验证码的正确性
		String uniqueKey = jsonObject.getString("uniqueKey");
		String refresh = jsonObject.getString("refresh");		//是否强制刷新验证码
		String img = null;
		try {
			String code = null;
			String key = VerifyCodeUtils.VERIFY_CODE + uniqueKey;
			if(redisRepository.exists(key)) {
				if( "true".equals(refresh) ) {
					code = VerifyCodeUtils.getRandKey(4);
				}else {
					code = redisRepository.get(key);					
				}
			}else {
				code = VerifyCodeUtils.getRandKey(4);
			}
			img = VerifyCodeUtils.verifyCode(90, 25, code);
			//将验证码存入redis，5分钟有效
			redisRepository.setExpire(key, code, 5*60);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ResponseBean(ResponseConstant.SUCCESS, img);
	}
}
