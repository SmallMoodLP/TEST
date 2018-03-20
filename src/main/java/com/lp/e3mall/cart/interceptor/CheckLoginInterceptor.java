package com.lp.e3mall.cart.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.lp.e3mall.common.pojo.EasyuiResultJson;
import com.lp.e3mall.common.utils.CookieUtils;
import com.lp.e3mall.pojo.TbUser;
import com.lp.e3mall.sso.service.TokenService;
/**
 * 本拦截器不用来拦截，而是用来判断用户是否登录，根据用户是否登录去做不同的处理
 * @author lp
 *
 */
public class CheckLoginInterceptor implements HandlerInterceptor{
	
	@Autowired
	private TokenService tokenService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 前处理
		//从cookie中获取token
		String json = CookieUtils.getCookieValue(request, "token");
		//如果为空，说明未登录，就放行
		if (StringUtils.isBlank(json)) {
			return true;
		}
		//如果不为空，那么就调用单点登录系统验证
		EasyuiResultJson resultJson = tokenService.getSessionUser(json);
		//如果用户登录失效了，那么就放行
		if (resultJson.getStatus() != 200) {
			return true;
		}
		//如果用户为登录状态，那么就获取用户信息
		TbUser tbUser  = (TbUser) resultJson.getData();
		//将用户信息存放到request中，放行。。之后在controlller中只需要判断request中是否有用户信息就知道用户是否登录
		request.setAttribute("user", tbUser);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// 返回之前的处理
		
	}

	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		// 返回之后处理异常
		
	}

}
