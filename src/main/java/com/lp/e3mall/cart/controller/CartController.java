package com.lp.e3mall.cart.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lp.e3mall.cart.service.CartService;
import com.lp.e3mall.common.pojo.EasyuiResultJson;
import com.lp.e3mall.common.utils.CookieUtils;
import com.lp.e3mall.common.utils.JsonUtils;
import com.lp.e3mall.pojo.TbItem;
import com.lp.e3mall.pojo.TbUser;
import com.lp.e3mall.service.ItemService;

/**
 * 购物车功能的controller
 * @author Administrator
 *
 */
@Controller
public class CartController {
	
	@Value("${CART_COOKIE_TIME}")
	private Integer CART_COOKIE_TIME;
	
	@Autowired
	private ItemService itemService;
	
	@Autowired
	private CartService cartService;
	
	//删除指定购物车项
	@RequestMapping("/cart/delete/{itemId}")
	public String deleteCartItemById(@PathVariable Long itemId,
			HttpServletRequest request,HttpServletResponse response){
		//用户登录时
		//从request中获取user
		TbUser tbUser = (TbUser) request.getAttribute("user");
		//判断用户是否登录
		if (tbUser != null) {
			//如果登录
			//调用cart服务删除redis中的数据
			cartService.deleteCartItemById(tbUser.getId(), itemId);
			//返回
			return "redirect:/cart/cart.html";
		}
		
		//用户未登录时
		//先从cookie中获取购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		//遍历找到对应商品，删除
		for (TbItem tbItem : list) {
			if (tbItem.getId().longValue() == itemId) {
				//删除也就是从购物车列表中移除这一项
				list.remove(tbItem);
				//移除之后必须跳出循环，不然再遍历的话会拋异常
				break;
			}
		}
		//将cookie写回
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), CART_COOKIE_TIME, true);
		//删除之后需要刷新页面，重定向到/cart/cart.html
		return "redirect:/cart/cart.html";
	}
	
	
	
	//更新购物车中的商品数量
	@RequestMapping("/cart/update/num/{itemId}/{num}")
	@ResponseBody
	public EasyuiResultJson updataCartListItemNum(@PathVariable Long itemId,@PathVariable Integer num,
			HttpServletRequest request,HttpServletResponse response){
		//用户登录时
		//从request中取到user
		TbUser tbUser = (TbUser) request.getAttribute("user");
		//判断是否登录
		if (tbUser != null) {
			//如果登录
			//调用cart服务层更新redis中的商品数量
			cartService.updataRedisCartList(tbUser.getId(), itemId, num);
			//返回
			return new EasyuiResultJson(null);
		}
		
		//用户未登录时
		//先从cookie中的获取购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		//找到对应id的商品，修改商品数量
		for (TbItem tbItem : list) {
			if (tbItem.getId().longValue() == itemId) {
				tbItem.setNum(num);
				break;
			}
		}
		//写回cookie中
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), CART_COOKIE_TIME, true);
		return new EasyuiResultJson(null);
	}
	
	
	
	
	//展示购物车列表
	@RequestMapping("/cart/cart")
	public String showCartList(HttpServletRequest request,HttpServletResponse response){
		//用户登录时
		//从request中取出user
		TbUser tbUser = (TbUser) request.getAttribute("user");
		//判断是否登录
		if (tbUser != null) {
			//如果登录
			//从cookie中取购物车列表
			List<TbItem> list = getCartListFromCookie(request);
			
			//调用cart服务层合并购物车列表
			cartService.mergeCartList(tbUser.getId(), list);
			
			//删除cookie中的购物车
			CookieUtils.deleteCookie(request, response, "cart");

			//获取redis中的购物车列表
			List<TbItem> list2 = cartService.getCartListFromRedis(tbUser.getId());
			//只取一张图片展示
			for (TbItem tbItem : list2) {
				if (StringUtils.isNotBlank(tbItem.getImage())) {
					tbItem.setImage(tbItem.getImage().split(",")[0]);
				}
			}
			//将购物车列表存放到request中
			request.setAttribute("cartList", list2);
			//返回
			return "cart";
		}
		
		
		//用户没有登陆时
		//先从cookie中获取购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		for (TbItem tbItem : list) {
			//每个商品只需要展示一张图片
			if (StringUtils.isNotBlank(tbItem.getImage())) {
				tbItem.setImage(tbItem.getImage().split(",")[0]);
			}
		}
		//将购物车列表保存到request域中
		request.setAttribute("cartList", list);
		//返回
		return "cart";
	}
	
	
	
	//添加商品到购物车
	@RequestMapping("/cart/add/{itemId}")
	public String addCart(@PathVariable Long itemId,Integer num,
			HttpServletRequest request,HttpServletResponse response){
		//用户登录时使用购物车，就将购物车列表存放到redis中
		//先从request中取出user
		TbUser tbUser = (TbUser) request.getAttribute("user");
		//判断是否登录
		if (tbUser != null) {
			//如果登录
			//调用cart服务层将购物车列表加入redis
			cartService.addCartListToRedis(tbUser.getId(), itemId, num);
			//返回逻辑视图
			return "cartSuccess";
		}
		
		
		//如果没有登录
		//用户没有登录时使用购物车，购物车信息存放到cookie中
		//先从cookie中取到商品列表（经分析此步骤可能需要重复写，所以抽取到通用方法中）
		List<TbItem> cartList = getCartListFromCookie(request);
		boolean flag = false;
		for (TbItem tbItem : cartList) {
			//判断商品列表中是否存在该商品
			if (tbItem.getId().longValue() == itemId) {//tbItem.getId()返回的是Long 是对象  对象一起==比较  比较的是内存地址
				flag = true;
				//如果存在就把数量相加
				tbItem.setNum(tbItem.getNum() + num);
				//找到了就跳出循环就行，因为商品列表中一种商品只会有一项
				break;
			}
		}
		//如果不存在
		if (!flag) {
			//根据商品id查询商品的信息，经分析可以使用TbItem这个pojo，可以使用TbItem中的库存列num来存放购物车中的商品数量
			TbItem tbItem = itemService.getItemById(itemId);
			//设置商品的数量
			tbItem.setNum(num);
			//添加进购物车列表
			cartList.add(tbItem);
		}
		//写回cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(cartList), CART_COOKIE_TIME, true);
		return "cartSuccess";
	}
	
	
	//从cookie中取出购物车列表
	private List<TbItem> getCartListFromCookie(HttpServletRequest request){
		//从cookie中获取购物车列表
		String jsonList = CookieUtils.getCookieValue(request, "cart", true);
		if (StringUtils.isBlank(jsonList)) {
			//如果没有，就返回一个空的list
			return new ArrayList<>();
		}
		//如果有就返回就行
		return JsonUtils.jsonToList(jsonList, TbItem.class);
	}
}
