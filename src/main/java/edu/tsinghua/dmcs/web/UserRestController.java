package edu.tsinghua.dmcs.web;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import edu.tsinghua.dmcs.util.TockenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import edu.tsinghua.dmcs.Response;
import edu.tsinghua.dmcs.entity.Role;
import edu.tsinghua.dmcs.entity.User;
import edu.tsinghua.dmcs.interceptor.DmcsController;
import edu.tsinghua.dmcs.service.RoleService;
import edu.tsinghua.dmcs.service.UserService;
import io.swagger.annotations.ApiOperation;
import sun.misc.BASE64Encoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value="/dmcs/api/v1/user")
public class UserRestController {

	private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private RoleService roleService;

	@Autowired
	private TockenCache tockenCache;

	@Value("${security.sault.login}")
	private String securitySault;
	
	@DmcsController(loginRequired=false)
    @ApiOperation(value="查询用户名是否存在", notes="data中true or false代表用户是否已存在")
	@RequestMapping(value = "checkUserExistence", method = RequestMethod.GET)
	public Response checkExistence(@RequestParam String username) {
    	logger.trace("checkExistence");
		User user = userService.checkExistence(username);
		return Response.SUCCESS().setData(user);
	}
	
	@DmcsController(loginRequired=false)
    @ApiOperation(value="注册新用户", notes="")
	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public Response register(@RequestBody String body) throws ParseException {
		System.out.println(body);
		JSONObject o = JSONObject.parseObject(body);
		User u = new User();
		u.setUid("1"); // TODO
		u.setEmail(o.getString("mail"));

		String password = o.getString("password");
		u.setUsername(o.getString("mail"));
		// u.setRealname(realname);
		// u.setTitle(title);
		// u.setIdcard(idcard);
		String securedPasswd = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("md5");
			byte [] bs = digest.digest((this.securitySault + password).getBytes());
			securedPasswd = new String(bs);

		} catch (Exception e) {
			logger.error("fail to get md5 algorithm");
		}
		// if securedPasswd is null throw exception
		u.setPassword(securedPasswd);
		// u.setAlias(alias);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// Date d = sdf.parse(birthday);
		// u.setBirthday(d); // TODO
		// u.setImage(image);
		// u.setIcon(icon);
		u.setEmail(mail);//
		u.setMobile(o.getString("mobile"));
		u.setRegtime(new Date());
		int num = userService.addUser(u);
		u.setPassword(null);
		return Response.SUCCESSOK();
		
	}
	
	@DmcsController(loginRequired=false)
    @ApiOperation(value="用户登陆", notes="true登陆成功")
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public Response login(@RequestBody String body,
			HttpServletResponse response) {
		JSONObject o = JSONObject.parseObject(body);
		String username = o.getString("userName");
		String password = o.getString("password");
		User u = userService.checkExistence(username);
		if(u != null) {
			String securedPasswd = null;
			try {
				MessageDigest digest = MessageDigest.getInstance("md5");
				byte [] bs = digest.digest((this.securitySault + password).getBytes());
				securedPasswd = new String(bs);
				String token = this.getToken(u.getUsername()) ;
				Cookie cookie = new Cookie("dmcstoken", token);
				cookie.setMaxAge(3600);
				cookie.setPath("/");
				response.addCookie(cookie);
				tockenCache.setTokenForUser(token, u.getUsername());


			} catch (Exception e) {
				logger.error("fail to get md5 algorithm");
			}
			if(u.getPassword().equals(securedPasswd)) {
				u.setPassword(null);
				return Response.SUCCESSOK().setData(u);
			}
		}
		
		return Response.SUCCESS().setData(null);
		
	}

	@DmcsController(loginRequired=true)
	@ApiOperation(value="用户登出", notes="true登出成功")
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public Response logout(@RequestParam String username, HttpServletRequest request,
						   HttpServletResponse response) {

		User u = userService.checkExistence(username);
		if(u != null) {
			String securedPasswd = null;
			try {
				Cookie [] cookies = request.getCookies();
				for(Cookie cookie : cookies) {
					String cookieName = cookie.getName();
					if("dmcstoken".equals(cookieName)) {
						String cookieValue = cookie.getValue();
						if(tockenCache.isTokenExist(cookieValue)) {
							tockenCache.removeToken(cookieValue);
							cookie.setValue(null);
							cookie.setMaxAge(0);
							cookie.setPath("/");
							response.addCookie(cookie);
							break;

						}
					}
				}

			} catch (Exception e) {
				logger.error("fail to get md5 algorithm");
			}
		}

		return Response.SUCCESS().setData(null);

	}
	
	@DmcsController(description="更新用户信息")
    @ApiOperation(value="更新用户信息", notes="返回更新成功个数")
	@RequestMapping(value = "/update", method = RequestMethod.GET)
	public Response update(@RequestParam String username,
			@RequestParam String realname,
			@RequestParam String title,
			@RequestParam String idcard,
			@RequestParam String password,
			@RequestParam String alias,
			@RequestParam String birthday,
			@RequestParam String image,
			@RequestParam String icon,
			@RequestParam String email,
			@RequestParam String mobile) {
    	int num = 0;
		User u = userService.checkExistence(username);
		if(u != null) {
			u.setRealname(realname);
			u.setTitle(title);
			u.setIdcard(idcard);
			String securedPasswd = null;
			try {
				MessageDigest digest = MessageDigest.getInstance("md5");
				byte [] bs = digest.digest((this.securitySault + password).getBytes());
				securedPasswd = new String(bs);

			} catch (Exception e) {
				logger.error("fail to get md5 algorithm");
			}
			// if securedPasswd is null throw exception
			u.setPassword(securedPasswd);
			u.setAlias(alias);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date d = null;
			try {
				d = sdf.parse(birthday);
			} catch (ParseException e) {
				logger.error("fail to parse date", e);
			}
			u.setBirthday(d);
			u.setImage(image);
			u.setIcon(icon);
			u.setEmail(email);
			u.setMobile(mobile);
		} else {
			 num = userService.update(u);
		}
		return Response.SUCCESS().returnData(num);
		
	}
	
	@DmcsController(loginRequired=true)
    @ApiOperation(value="获得指定用户角色", notes="获得当前用户角色")
	@RequestMapping(value = "/listRolesByUserId", method = RequestMethod.GET)
	public Response listRolesByUserId(String userName) {
		List<Role> rlist = roleService.getRoleListByUserName(userName);
		return Response.SUCCESS().setData(rlist);
	}

	private String getToken(String userName) {
		String token = userName + this.securitySault + System.currentTimeMillis();
		String securedtoken = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("md5");
			byte [] bs = digest.digest(token.getBytes());
			securedtoken = new String(bs);
			securedtoken = securedtoken + "|" + (System.currentTimeMillis() + 1000 * 3600);
			securedtoken = new BASE64Encoder().encode(securedtoken.getBytes());
		} catch (Exception e) {
			logger.error("fail to get md5 algorithm");
		}
		return securedtoken;
	}
	
}
