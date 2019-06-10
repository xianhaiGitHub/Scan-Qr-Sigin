package com.tony.admin.web.common.exception;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONException;
import com.fasterxml.jackson.core.JsonParseException;
import com.tony.admin.web.common.response.ResponseBean;
import com.tony.admin.web.common.response.ResponseConstant;


/**
 * 全局异常处理
 * @author Guoqing
 * @version 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(value = JSONException.class)
	@ResponseBody
	public ResponseBean jsonExceptionHandler(HttpServletRequest req, JSONException e) {		
		ResponseBean response = new ResponseBean(ResponseConstant.IVALID_ERROR, null);
		logger.error("1102",e);
		logger.error("ExceptionHandler"+response.toString());
		return response;
	}
	
	@ExceptionHandler(value = JsonParseException.class)
	@ResponseBody
	public ResponseBean jsonParseExceptionHandler(HttpServletRequest req, JsonParseException e) {
		ResponseBean response = new ResponseBean(ResponseConstant.IVALID_ERROR, null);
		logger.error("1102",e);
		logger.error("ExceptionHandler"+response.toString());
		return response;
	}
    
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseBean exceptionHandler(HttpServletRequest req, Exception e) {   	
		ResponseBean response = new ResponseBean(ResponseConstant.SERVER_ERROR, null);
		logger.error("1000",e);
    	logger.error("ExceptionHandler"+response.toString());
    	return response;
    }
    
}
